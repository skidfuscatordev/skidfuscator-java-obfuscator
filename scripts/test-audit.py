#!/usr/bin/env python3
import contextlib,importlib.util,io,json,pathlib,sys,tempfile
from unittest.mock import patch
spec=importlib.util.spec_from_file_location('audit',pathlib.Path(__file__).with_name('audit-dependencies.py'))
audit=importlib.util.module_from_spec(spec);spec.loader.exec_module(audit)
bom={'components':[{'group':'example','name':'sample','version':'1.0','purl':'pkg:maven/example/sample@1.0'}]}
assert not audit.audit(bom,lambda request:{'results':[{}]})['findings']
assert audit.audit(bom,lambda request:{'results':[{'vulns':[{'id':'TEST-ADVISORY'}]}]})['findings'][0]['id']=='TEST-ADVISORY'
for invalid,query in [({'components':[]},lambda request: {}),(bom,lambda request:{'results':[]}),
        (bom,lambda request:{'results':[{'error':'provider unavailable'}]})]:
    try: audit.audit(invalid,query)
    except ValueError: pass
    else: raise AssertionError('Incomplete audit was accepted')
with tempfile.TemporaryDirectory() as directory:
    work=pathlib.Path(directory); inventory=work/'sbom.json'; report=work/'audit.json'
    inventory.write_text(json.dumps(bom))
    report.write_text(json.dumps({'status':'completed','findings':[]}))
    with patch.object(sys,'argv',['audit',str(inventory),str(report)]), \
            patch.object(audit,'query_osv',side_effect=RuntimeError('test network failure')), \
            contextlib.redirect_stderr(io.StringIO()):
        assert audit.main()==2
    failed=json.loads(report.read_text())
    assert failed['status']=='failed' and 'findings' not in failed
print('Audit checks: findings detected; empty/incomplete/provider errors rejected; stale reports replaced')
