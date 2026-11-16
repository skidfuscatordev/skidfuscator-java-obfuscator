#!/usr/bin/env python3
"""Audit a CycloneDX Maven inventory against OSV; errors and findings fail closed."""
import argparse
import datetime
import json
import os
import pathlib
import sys
import tempfile
import urllib.request


def audit(bom, query):
    components = [c for c in bom.get('components', []) if c.get('purl', '').startswith('pkg:maven/')]
    if not components:
        raise ValueError('No Maven components were found; refusing an empty audit')
    request = {'queries': [{'package': {'ecosystem': 'Maven', 'name': c['group'] + ':' + c['name']},
                            'version': c['version']} for c in components]}
    results = query(request)['results']
    if not isinstance(results, list) or len(results) != len(components):
        raise ValueError('Incomplete vulnerability response')
    findings = []
    for component, result in zip(components, results):
        if not isinstance(result, dict) or 'error' in result:
            raise ValueError('Vulnerability provider failed to check a component')
        for vulnerability in result.get('vulns', []):
            findings.append({'component': component['purl'], 'id': vulnerability['id']})
    return {'schemaVersion': 1, 'provider': 'OSV', 'status': 'completed', 'checkedComponents': len(components),
            'observedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'findings': findings}


def query_osv(payload):
    request = urllib.request.Request('https://api.osv.dev/v1/querybatch',
        data=json.dumps(payload).encode(), headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def write_report(path, result):
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary = tempfile.mkstemp(prefix='.audit-', dir=path.parent)
    try:
        with os.fdopen(descriptor, 'w') as stream:
            json.dump(result, stream, indent=2)
            stream.write('\n')
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('sbom', type=pathlib.Path)
    parser.add_argument('report', type=pathlib.Path)
    args = parser.parse_args()
    try:
        result = audit(json.loads(args.sbom.read_text()), query_osv)
        write_report(args.report, result)
        print(f"OSV: {result['checkedComponents']} components, {len(result['findings'])} findings")
        return 1 if result['findings'] else 0
    except Exception as failure:
        print(f'Vulnerability audit did not complete: {failure}', file=sys.stderr)
        # Replace any previous successful result; an unavailable scan must not
        # leave a stale clean report looking like the current result.
        try:
            write_report(args.report, {'schemaVersion': 1, 'provider': 'OSV', 'status': 'failed',
                'observedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(),
                'error': str(failure)})
        except OSError as report_failure:
            print(f'Could not write failed audit report: {report_failure}', file=sys.stderr)
        return 2

if __name__ == '__main__':
    sys.exit(main())
