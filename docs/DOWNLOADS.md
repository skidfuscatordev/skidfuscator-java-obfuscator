# Download the last approved default-branch build

The default branch of this repository is currently `master`. The workflow also
accepts `main` if the default branch is renamed. It does not rename branches.

The fixed pre-release page is:

https://github.com/walbergaia/skidfuscator-java-obfuscator/releases/tag/main-build

Its assets have stable names and links:

| Asset | Download |
|---|---|
| Executable JAR | [skidfuscator.jar](https://github.com/walbergaia/skidfuscator-java-obfuscator/releases/download/main-build/skidfuscator.jar) |
| SHA-256 checksums | [SHA256SUMS](https://github.com/walbergaia/skidfuscator-java-obfuscator/releases/download/main-build/SHA256SUMS) |
| Dependency inventory | [sbom.cdx.json](https://github.com/walbergaia/skidfuscator-java-obfuscator/releases/download/main-build/sbom.cdx.json) |
| Source commit and CI provenance | [build-info.json](https://github.com/walbergaia/skidfuscator-java-obfuscator/releases/download/main-build/build-info.json) |

The links become available after this automation is merged and the first approved
default-branch build publishes the pre-release. The JAR requires Java 17+.

## What happens after a merge

1. The default-branch push runs the unit, integration, differential, dependency
   audit and Java 8/11/17/21/25 compatibility checks.
2. After they pass, `distribution` packages the tested distribution with its SBOM,
   source commit, workflow URL and SHA-256 checksums. The `verified-distribution`
   artifact remains downloadable from the Actions run page as a ZIP.
3. `verify-download` downloads that artifact and checks its contents and hashes.
4. `publish-main` reuses the downloaded bundle and updates only the `main-build`
   pre-release/tag. It does not recompile the JAR or create a versioned release.

Pull requests perform the same packaging/download checks and a publication dry
run; the publishing job is skipped. Failed checks do not reach publication.
The write token is restricted to `publish-main`; other jobs keep read access.
The publisher checks the repository, source SHA, default branch and its current
head before making changes. An older run is skipped if the branch has advanced,
and publishing jobs are serialized to avoid concurrent asset replacement.

`main-build` is a mutable development pre-release, separate from immutable
versioned releases such as `v2.2.0`. Its body and `build-info.json` identify the
exact source commit and successful CI run. It is not marked as the latest official
release. The first publication stays a draft until the assets upload successfully.
Updates replace the existing rolling assets; an interrupted upload fails the job
and may require rerunning the workflow. Official releases are never overwritten.

## Download and verify

Download all four files from the same completed build into an empty directory:

```bash
base=https://github.com/walbergaia/skidfuscator-java-obfuscator/releases/download/main-build
for file in skidfuscator.jar sbom.cdx.json build-info.json SHA256SUMS; do
  curl --fail --location "$base/$file" --output "$file"
done
sha256sum --check SHA256SUMS
java -jar skidfuscator.jar --version
```

On Windows, compare `Get-FileHash skidfuscator.jar -Algorithm SHA256` with the
`skidfuscator.jar` entry in `SHA256SUMS`. Check the source commit in
`build-info.json` and the linked CI run when identifying a build. On macOS,
`shasum -a 256 --check SHA256SUMS` verifies the full bundle.

The legacy Homebrew workflow is limited to the upstream repository's official
releases. This fork's automatic pre-release does not write to an upstream tap or
update upstream installers.

## Retry publication

Use **Actions → Java CI with Gradle → Run workflow**, selecting the default
branch. This reruns validation and publishes only if it passes and the source is
still the default branch's head. The workflow requests `contents: write` only for
the publication job through `GITHUB_TOKEN`; no personal access token is required.
Repository or organization policies must allow that job permission.
