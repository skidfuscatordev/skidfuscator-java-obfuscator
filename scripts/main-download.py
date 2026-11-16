#!/usr/bin/env python3
"""Package validated CI output and maintain the main-build development download."""
import argparse
import hashlib
import json
import os
import pathlib
import re
import shutil
import subprocess
import tempfile
import urllib.parse
import zipfile

TAG = 'main-build'
DATA_FILES = ('skidfuscator.jar', 'sbom.cdx.json', 'build-info.json')
ASSETS = (*DATA_FILES, 'SHA256SUMS')


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def validate_identity(repository, commit):
    if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', repository):
        raise ValueError('Expected a GitHub owner/repository')
    if not re.fullmatch(r'[0-9a-f]{40}', commit):
        raise ValueError('Expected the complete source commit SHA')


def version_of(jar, sbom):
    with zipfile.ZipFile(jar) as archive:
        if archive.testzip() is not None:
            raise ValueError('Invalid JAR checksum')
        manifest = archive.read('META-INF/MANIFEST.MF').decode('utf-8')
    versions = [line.split(':', 1)[1].strip() for line in manifest.splitlines()
                if line.startswith('Implementation-Version:')]
    inventory = json.loads(sbom.read_text())
    if len(versions) != 1 or not re.fullmatch(r'\d+\.\d+\.\d+(?:-[A-Za-z0-9.-]+)?', versions[0]):
        raise ValueError('JAR implementation version is missing or invalid')
    if inventory.get('bomFormat') != 'CycloneDX' or inventory['metadata']['component']['version'] != versions[0]:
        raise ValueError('JAR and SBOM versions do not agree')
    return versions[0]


def bundle(jar, sbom, output, repository, commit, run_id, run_attempt=1):
    validate_identity(repository, commit)
    if int(run_id) <= 0 or int(run_attempt) <= 0:
        raise ValueError('Expected a GitHub workflow run and attempt')
    version = version_of(jar, sbom)
    if output.exists():
        raise ValueError('Bundle destination already exists; refusing stale assets')
    output.parent.mkdir(parents=True, exist_ok=True)
    staging = pathlib.Path(tempfile.mkdtemp(prefix='.download-', dir=output.parent))
    try:
        shutil.copyfile(jar, staging / 'skidfuscator.jar')
        shutil.copyfile(sbom, staging / 'sbom.cdx.json')
        info = {'schemaVersion': 1, 'version': version, 'repository': repository, 'commit': commit,
                'runId': int(run_id), 'runAttempt': int(run_attempt),
                'workflowUrl': f'https://github.com/{repository}/actions/runs/{run_id}',
                'files': {name: digest(staging / name) for name in DATA_FILES[:2]}}
        (staging / 'build-info.json').write_text(json.dumps(info, indent=2) + '\n')
        (staging / 'SHA256SUMS').write_text(''.join(f'{digest(staging / name)}  {name}\n' for name in DATA_FILES))
        verify(staging)
        os.replace(staging, output)
    finally:
        if staging.exists():
            shutil.rmtree(staging)


def verify(directory):
    if {p.name for p in directory.iterdir()} != set(ASSETS):
        raise ValueError('Download bundle has missing or unexpected files')
    if any(not (directory / name).is_file() or (directory / name).is_symlink() for name in ASSETS):
        raise ValueError('Bundle assets must be regular files')
    checksums = {}
    for line in (directory / 'SHA256SUMS').read_text().splitlines():
        match = re.fullmatch(r'([0-9a-f]{64})  ([A-Za-z0-9.-]+)', line)
        if not match or match[2] not in DATA_FILES or match[2] in checksums:
            raise ValueError('Invalid or duplicate checksum entry')
        checksums[match[2]] = match[1]
    if set(checksums) != set(DATA_FILES) or any(digest(directory / name) != checksums[name] for name in DATA_FILES):
        raise ValueError('Download bundle checksum mismatch')
    info = json.loads((directory / 'build-info.json').read_text())
    validate_identity(info['repository'], info['commit'])
    if info.get('schemaVersion') != 1 or info.get('files') != {name: checksums[name] for name in DATA_FILES[:2]}:
        raise ValueError('Build provenance does not match the assets')
    if info['version'] != version_of(directory / 'skidfuscator.jar', directory / 'sbom.cdx.json'):
        raise ValueError('Build provenance version mismatch')
    expected_url = f"https://github.com/{info['repository']}/actions/runs/{int(info['runId'])}"
    if info['workflowUrl'] != expected_url or int(info['runId']) <= 0 or int(info['runAttempt']) <= 0:
        raise ValueError('Invalid workflow provenance')
    return info


class GitHub:
    def run(self, *arguments):
        subprocess.run(['gh', *arguments], check=True)

    def api(self, method, path, data=None, missing_ok=False):
        arguments = ['gh', 'api', '--method', method, path]
        if data is not None:
            arguments += ['--input', '-']
        result = subprocess.run(arguments, input=json.dumps(data) if data is not None else None,
                                capture_output=True, text=True)
        if result.returncode:
            if missing_ok and '(HTTP 404)' in result.stderr:
                return None
            raise RuntimeError('GitHub API request failed: ' + result.stderr.strip())
        return json.loads(result.stdout) if result.stdout.strip() else None


def find_release(github, repository):
    page = 1
    while True:
        releases = github.api('GET', f'repos/{repository}/releases?per_page=100&page={page}')
        for release in releases:
            if release['tag_name'] == TAG:
                return release
        if len(releases) < 100:
            return None
        page += 1


def publish(directory, repository, commit, branch, github=None, dry_run=False):
    info = verify(directory)
    if info['repository'] != repository or info['commit'] != commit:
        raise ValueError('Bundle belongs to a different repository or commit')
    if dry_run:
        print(f"Validated preview: {TAG}, version {info['version']}, commit {commit}")
        return
    github = github or GitHub()
    repo = github.api('GET', f'repos/{repository}')
    if branch != repo['default_branch']:
        raise ValueError('Only the default branch may update the main download')
    head = github.api('GET', f'repos/{repository}/git/ref/heads/{urllib.parse.quote(branch, safe="")}')
    if head['object']['sha'] != commit:
        print('Skipping an outdated build; the default branch has advanced')
        return
    release = find_release(github, repository)
    if release is not None and not release['prerelease']:
        raise ValueError('Refusing to overwrite an official release using the reserved main-build tag')
    base = f'https://github.com/{repository}'
    download = f'{base}/releases/download/{TAG}'
    notes = (f"Último build aprovado da branch `{branch}`. Pré-release atualizável.\n\n"
             f"Versão: **{info['version']}**\n\n"
             f"Commit: [`{commit}`]({base}/commit/{commit})\n\n"
             f"Validação: [GitHub Actions]({info['workflowUrl']})\n\n"
             f"[Baixar JAR]({download}/skidfuscator.jar) · [SHA-256]({download}/SHA256SUMS) · "
             f"[SBOM]({download}/sbom.cdx.json) · [Build]({download}/build-info.json)\n\n"
             "Java 17+ é necessário para executar o obfuscador. Esta pré-release não substitui releases oficiais por versão.\n")
    title = f"Último build aprovado — {branch}"
    with tempfile.TemporaryDirectory() as temporary:
        notes_file = pathlib.Path(temporary) / 'notes.md'
        notes_file.write_text(notes)
        if release is None:
            github.run('release', 'create', TAG, '--repo', repository, '--target', commit,
                       '--draft', '--prerelease', '--latest=false', '--title', title, '--notes-file', str(notes_file))
        # Keep an initial release private until all four assets have uploaded.
        # Existing rolling releases are mutable; a failed upload fails the job and can be retried.
        github.run('release', 'upload', TAG, *(str(directory / name) for name in ASSETS),
                   '--repo', repository, '--clobber')
        ref = github.api('GET', f'repos/{repository}/git/ref/tags/{TAG}', missing_ok=True)
        if ref is None:
            github.api('POST', f'repos/{repository}/git/refs', {'ref': f'refs/tags/{TAG}', 'sha': commit})
        else:
            github.api('PATCH', f'repos/{repository}/git/refs/tags/{TAG}', {'sha': commit, 'force': True})
        github.run('release', 'edit', TAG, '--repo', repository, '--target', commit,
                   '--draft=false', '--prerelease', '--latest=false', '--title', title, '--notes-file', str(notes_file))
    print(f'Published verified development download: {base}/releases/tag/{TAG}')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    prepare = commands.add_parser('bundle')
    prepare.add_argument('--jar', type=pathlib.Path, required=True)
    prepare.add_argument('--sbom', type=pathlib.Path, required=True)
    prepare.add_argument('--output', type=pathlib.Path, required=True)
    prepare.add_argument('--repository', required=True)
    prepare.add_argument('--commit', required=True)
    prepare.add_argument('--run-id', type=int, required=True)
    prepare.add_argument('--run-attempt', type=int, default=1)
    for name in ['verify', 'publish']:
        command = commands.add_parser(name)
        command.add_argument('directory', type=pathlib.Path)
        if name == 'publish':
            command.add_argument('--repository', required=True)
            command.add_argument('--commit', required=True)
            command.add_argument('--branch', required=True)
            command.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()
    if args.command == 'bundle':
        bundle(args.jar, args.sbom, args.output, args.repository, args.commit, args.run_id, args.run_attempt)
    elif args.command == 'verify':
        info = verify(args.directory)
        print(f"Download bundle verified: {info['version']} at {info['commit']}")
    else:
        publish(args.directory, args.repository, args.commit, args.branch, dry_run=args.dry_run)


if __name__ == '__main__':
    main()
