#!/usr/bin/env python3
"""Verify bundle integrity and publication guards without publishing a release."""
import contextlib
import importlib.util
import io
import json
import pathlib
import tempfile
import unittest
import zipfile

spec = importlib.util.spec_from_file_location('main_download', pathlib.Path(__file__).with_name('main-download.py'))
download = importlib.util.module_from_spec(spec)
spec.loader.exec_module(download)
REPOSITORY = 'example/application'
COMMIT = 'a' * 40


class FakeGitHub:
    def __init__(self, head=COMMIT, branch='master', releases=None, upload_failure=False, tag=None):
        self.head, self.branch, self.releases = head, branch, releases or []
        self.upload_failure = upload_failure
        self.tag = tag
        self.mutations = []
        self.requests = []
        self.commands = []

    def api(self, method, path, data=None, missing_ok=False):
        self.requests.append((method, path))
        if method != 'GET':
            self.mutations.append((method, path, data))
            return {}
        if path == f'repos/{REPOSITORY}':
            return {'default_branch': self.branch}
        if '/git/ref/heads/' in path:
            return {'object': {'sha': self.head}}
        if '/releases?' in path:
            return self.releases
        if '/git/ref/tags/' in path:
            return self.tag
        raise AssertionError(path)

    def run(self, *arguments):
        self.commands.append(arguments)
        if arguments[:2] == ('release', 'upload') and self.upload_failure:
            raise RuntimeError('simulated upload failure')


class MainDownloadTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = pathlib.Path(self.temporary.name)
        self.jar, self.sbom, self.output = self.root / 'input.jar', self.root / 'input.json', self.root / 'bundle'
        with zipfile.ZipFile(self.jar, 'w') as jar:
            jar.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\r\nImplementation-Version: 2.2.0\r\n\r\n')
            jar.writestr('fixture', b'original bytes')
        self.sbom.write_text(json.dumps({'bomFormat': 'CycloneDX', 'metadata': {'component': {'version': '2.2.0'}}}))

    def prepare(self):
        download.bundle(self.jar, self.sbom, self.output, REPOSITORY, COMMIT, 123, 2)

    def publish(self, github, **kwargs):
        with contextlib.redirect_stdout(io.StringIO()):
            download.publish(self.output, REPOSITORY, COMMIT, kwargs.pop('branch', 'master'), github=github, **kwargs)

    def test_bundle_preserves_tested_bytes_and_records_provenance(self):
        self.prepare()
        info = download.verify(self.output)
        self.assertEqual(self.jar.read_bytes(), (self.output / 'skidfuscator.jar').read_bytes())
        self.assertEqual(COMMIT, info['commit'])
        self.assertEqual(2, info['runAttempt'])
        self.assertEqual('https://github.com/example/application/actions/runs/123', info['workflowUrl'])

    def test_mismatched_versions_do_not_create_a_bundle(self):
        self.sbom.write_text(json.dumps({'bomFormat': 'CycloneDX', 'metadata': {'component': {'version': '1.0.0'}}}))
        with self.assertRaises(ValueError):
            self.prepare()
        self.assertFalse(self.output.exists())

    def test_tampered_bundle_is_never_sent_to_github(self):
        self.prepare()
        (self.output / 'skidfuscator.jar').write_bytes(b'tampered')
        github = FakeGitHub()
        with self.assertRaises(ValueError):
            self.publish(github)
        self.assertEqual([], github.commands)
        self.assertEqual([], github.mutations)

    def test_missing_extra_and_symlink_assets_are_rejected(self):
        self.prepare()
        extra = self.output / 'unexpected'
        extra.write_text('stale')
        with self.assertRaises(ValueError):
            download.verify(self.output)
        extra.unlink()
        asset = self.output / 'skidfuscator.jar'
        asset.unlink()
        with self.assertRaises(ValueError):
            download.verify(self.output)
        asset.symlink_to(self.jar)
        with self.assertRaises(ValueError):
            download.verify(self.output)

    def test_non_default_or_outdated_builds_cannot_publish(self):
        self.prepare()
        github = FakeGitHub()
        with self.assertRaises(ValueError):
            self.publish(github, branch='feature')
        outdated = FakeGitHub(head='b' * 40)
        self.publish(outdated)
        self.assertEqual([], github.commands + outdated.commands)
        self.assertEqual([], github.mutations + outdated.mutations)

    def test_official_release_cannot_be_overwritten(self):
        self.prepare()
        github = FakeGitHub(releases=[{'tag_name': 'main-build', 'prerelease': False}])
        with self.assertRaises(ValueError):
            self.publish(github)
        self.assertEqual([], github.commands)

    def test_upload_failure_keeps_initial_release_unpublished(self):
        self.prepare()
        github = FakeGitHub(upload_failure=True)
        with self.assertRaises(RuntimeError):
            self.publish(github)
        self.assertIn('--draft', github.commands[0])
        self.assertFalse(any(command[:2] == ('release', 'edit') for command in github.commands))
        self.assertEqual([], github.mutations)

    def test_first_success_uses_a_draft_then_publishes_exact_commit(self):
        self.prepare()
        github = FakeGitHub()
        self.publish(github)
        self.assertEqual([('release', 'create'), ('release', 'upload'), ('release', 'edit')],
                         [command[:2] for command in github.commands])
        self.assertIn('--draft', github.commands[0])
        self.assertIn('--draft=false', github.commands[-1])
        self.assertIn('--prerelease', github.commands[-1])
        self.assertIn('--latest=false', github.commands[-1])
        self.assertEqual({'ref': 'refs/tags/main-build', 'sha': COMMIT}, github.mutations[0][2])

    def test_preview_never_calls_github(self):
        self.prepare()
        github = FakeGitHub()
        self.publish(github, dry_run=True)
        self.assertEqual([], github.commands + github.requests)

    def test_existing_preview_updates_only_the_reserved_tag(self):
        self.prepare()
        github = FakeGitHub(releases=[{'tag_name': 'main-build', 'prerelease': True}],
                           tag={'object': {'sha': 'b' * 40}})
        self.publish(github)
        self.assertFalse(any(command[:2] == ('release', 'create') for command in github.commands))
        self.assertEqual([('PATCH', f'repos/{REPOSITORY}/git/refs/tags/main-build',
                           {'sha': COMMIT, 'force': True})], github.mutations)
        self.assertIn('--latest=false', github.commands[-1])

    def test_other_commit_or_repository_assets_are_never_published(self):
        self.prepare()
        github = FakeGitHub()
        with self.assertRaises(ValueError):
            download.publish(self.output, 'different/application', COMMIT, 'master', github=github)
        with self.assertRaises(ValueError):
            download.publish(self.output, REPOSITORY, 'b' * 40, 'master', github=github)
        self.assertEqual([], github.commands + github.requests)

    def test_github_read_failure_does_not_attempt_release_creation(self):
        self.prepare()
        github = FakeGitHub()
        def unavailable(*args, **kwargs):
            raise RuntimeError('simulated API outage')
        github.api = unavailable
        with self.assertRaises(RuntimeError):
            self.publish(github)
        self.assertEqual([], github.commands)


if __name__ == '__main__':
    unittest.main()
