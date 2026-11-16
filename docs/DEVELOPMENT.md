# Build and regression checks

Use a JDK (including `javac`), Java 17 or 21, and the checked-in Gradle 8.10
wrapper. The source target is Java 17. No service or credentials are required
for the public dependencies.

The release version is defined by `Skidfuscator.VERSION`. Gradle uses that same
constant for all active modules, JAR `Implementation-Version` attributes and
the SBOM. CLI `--version` reports it as well. The distribution keeps the stable
filename `client-standalone-all.jar`; use its manifest or CLI to identify the build.

```bash
bash scripts/prepare-test-jdk.sh
./gradlew --no-daemon --no-parallel --max-workers=2 --console=plain \
  --dependency-verification strict --continue \
  :modasm:test :stdlib:test :ir:test :pure-analysis:test :obfuscator:test :client-standalone:test \
  :integration:jar :integration:writeClasspath :integration:copyDecompiler generateSbom
python3 scripts/test-reliability.py
python3 scripts/test-signing.py
python3 scripts/test-audit.py
python3 scripts/test-main-download.py
python3 scripts/audit-dependencies.py build/reports/sbom.cdx.json build/reports/dependency-audit.json
EVALUATE_PROTECTION=1 bash scripts/framework-tests.sh
bash scripts/smoke-test.sh
```

`prepare-test-jdk.sh` prepares the Linux x64 Corretto 17 image expected by SSVM,
verifying its published SHA-256 before extraction. It accepts a writable home
directory as its first argument. For the smoke test, `SKIDFUSCATOR_USER_HOME`
selects the same home and `JAVA_BIN` can select another Java executable. The
`SKIDFUSCATOR_RUNTIME` variable can point to the target JDK's `jmods` when the
selected Java executable is only a JRE without runtime library files. The
smoke test compiles a Java 17 application, protects it with the default settings,
and executes both versions with `-Xverify:all`. It compares their output and
checks that an invalid CLI invocation exits with status 2.

The CI uses Ubuntu 24.04 and commit-pinned Actions running on Node 24. It builds
with Java 17 and tests target JVMs 8, 11, 17, 21 and 25. Reports are retained even when tests
fail; a failed check prevents uploading the distribution artifact. Individual
module HTML reports are in `build/reports/tests/test`.

Dependency versions are locked per module. SSVM is pinned to the full Git commit
behind the former `1.0.0-SNAPSHOT` tag. Artifact and metadata SHA-256 checksums are
stored in `gradle/verification-metadata.xml`, and the wrapper distribution has its
own checksum. Builds do not use `mavenLocal()`. Dependency changes require review
of the upstream source and artifacts before updating both the lockfiles and
verification metadata; do not accept a new checksum merely to bypass a failure.
Check both POM and Gradle `.module` metadata against publisher checksums when
reviewing a BOM update. Validate resolution with an isolated `GRADLE_USER_HOME`;
an existing cache may hide metadata needed by a fresh GitHub runner. Compatibility
jobs retain dependency-verification diagnostics as well as their test reports.
Archive timestamps and entry ordering are normalized. This makes build packaging
stable. Obfuscation can be reproduced with `--seed` and identical input, tool,
configuration, dependencies and target runtime, as described in RELIABILITY.md.

Output is written to a temporary file beside the target. Class bytecode, entry
counts and CRCs are checked before atomic replacement. If validation, frame
computation or atomic replacement fails, the previous target is retained and the
command fails. Classes are no longer silently omitted or restored from
unprotected input bytes. Applications relying on permissive recovery must fix
their classpath or transformation errors instead of accepting degraded output.

Bytecode validation and the small smoke test complement application-level tests.
Consumers should also run their own integration tests against the protected JAR,
especially when using reflection, serialization, JNI or framework discovery.

The extended controls, test matrix, threat model, signing process and compatibility
boundaries are documented in [RELIABILITY.md](RELIABILITY.md). The complete build
also runs `:ir:test :pure-analysis:test`, generates the SBOM and exercises the
reference `:integration` project. Runtime compatibility jobs compile payloads
with the selected Java 8/11/17/21/25 JDK while the build tool remains on JDK 17.

The compatibility corpus also checks null String operands, Object arguments,
single evaluation and Unicode/Turkish/Azeri case comparisons. Its comparison
class is excluded only from string-literal encryption so both comparison passes
must actually execute; the rest of the fixture retains the profile defaults.
Windows PowerShell 5.1 validation is described in [WINDOWS_VALIDATION.md](WINDOWS_VALIDATION.md).

The approved artifact is packaged into a flat download bundle, downloaded again
and verified before publication. Pull requests validate the same bundle and run
a publication preview without release writes. Default-branch pushes may update
the `main-build` pre-release after those checks. See [DOWNLOADS.md](DOWNLOADS.md).
