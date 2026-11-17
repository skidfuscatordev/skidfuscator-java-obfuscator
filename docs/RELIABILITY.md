# Protection workflow and reliability contracts

The supported pipeline is: build a thin application JAR, protect it, run its
integration tests on every supported deployment JVM, package it, and sign the
final distribution. The obfuscator itself requires Java 17+. Source names,
reflection metadata, JNI entry points and serialization contracts are part of
application behavior.

## CLI controls

`--libs=.` indexes every immediate `.jar` in the current directory, including
unrelated libraries. It neither selects dependencies automatically nor searches
subdirectories. Library JARs are analysis references and are not bundled into the
protected output. Class names are indexed without eagerly creating every analysis
node. Application definitions take precedence; libraries use descending priority
and stable insertion order for ties. Duplicate origins are listed in the report.
Use `--fail-on-duplicate-classes` to reject ambiguities before transformation. Prefer a dedicated `--libs=./libs` directory containing the
application's runtime dependencies, separate from input and output artifacts.

```bash
java -Xmx3g -jar client-standalone-all.jar obfuscate app.jar \
  --libs libs --runtime /path/to/target-jdk/jmods \
  --profile BALANCED --seed 123456 --offline --non-interactive \
  --report private-build/run.json --output app-protected.jar
```

For a Java 8 target, use its `jre/lib/rt.jar` instead of `jmods`. JDK distributions
without `jmods` (including current Temurin 25) can use their `lib/modules` image;
keep the accompanying `lib/jrt-fs.jar` in the JDK. The importer reads metadata
through the selected image's filesystem and does not execute application code. `--runtime`
selects the application's library model independently of the JVM running the
obfuscator. Public runtime images used by the tests are vendor checksum-verified.

`--dry-run` resolves the classpath and writes a plan with class, method and field
decisions. It does not transform or write an output JAR. It still needs valid
runtime/dependency metadata. Use `--offline` to prohibit automatic downloads.
`--non-interactive` makes automation intent explicit; the obfuscate command does
not open a GUI or prompt. Analytics require explicit `--analytics`; offline mode
also suppresses analytics.

A seed controls obfuscation randomness. Reproduction also requires the same tool,
input, configuration, dependencies and target runtime. The JSON report records
version, artifact and target-runtime hashes, seed, profile, decisions, transformer counts, phase
context/timings and observed heap usage. Configuration contents are not copied.
Schema 2 adds per-stage wall time, thread CPU time, allocated bytes, GC counts and
GC time, plus listener timings and the last transformer. Unsupported CPU/allocation
measurements use -1. GC metrics are process-wide; they are not per-stage allocation
estimates. Decisions are spooled to a temporary JSONL file and streamed into the
final JSON, then the spool is removed. `--report-summary` keeps counts and diagnostics
with an empty `elements` array. Atomic progress snapshots have `status: running`;
only `planned` or `succeeded` with exit code 0 confirms completion. A forcibly killed
process can leave a private decision spool beside its last progress report.
Treat reports as confidential build data: they contain original symbol names and
the seed. Publish the distribution SBOM independently.

## Profiles and bounds

| Profile | Default input-method instruction budget | Flow defaults |
|---|---:|---|
| CONSERVATIVE | 2,000 | Exception/range/switch and optional interprocedural/pure passes disabled |
| BALANCED | 8,000 | Standard passes; exception strength GOOD |
| STRONG | 10,000 | Standard passes; default exception strength AGGRESSIVE |

Explicit transformer configuration overrides profile defaults. Junk injection and
file-crasher metadata are off by default. Structural renaming is not implemented
by these profiles; field names remain preserved. A stronger profile does not
establish a quantified reverse-engineering guarantee.

Default budgets are 300,000 ms, 512 MiB input expansion and 512 MiB output.
`--max-duration-ms`, `--max-input-bytes`, `--max-output-bytes`,
`--max-growth-ratio` and `--max-method-instructions` override bounds. Zero for the
last two selects profile defaults. Growth allows the profile ratio (2/4/8) plus
256 KiB for runtime helpers in small applications. Methods over the instruction
budget are preserved and identified in the plan. Supported instruction budgets
are at most 10,000. A transformation failure aborts output instead of claiming
full protection.

Time checks are cooperative at phase/method and output boundaries. The Windows validation script supplies a process
supervisor with `-HardTimeoutSeconds` (1,200 seconds per invocation by default).
Other callers must supply their own process supervisor for a hard wall-clock limit.
Use `-Xmx` for a JVM heap bound. Reports
contain current heap usage, JVM heap limit and the sum of memory-pool peaks;
these measurements are not an operating-system RSS peak. Output is validated and
budget-checked before atomic replacement. Unsupported atomic replacement causes
a failure while retaining the previous output.

## Metadata and exclusions

String comparison transformations preserve null arguments, null-receiver
exceptions, Object arguments and the target JVM's native `equalsIgnoreCase`
semantics. They mask the literal with a hash-derived reversible UTF-16 key and
delegate the comparison to String itself, without locale-dependent lowercasing
or treating hash equality as proof of String equality. Literal masking is
obfuscation; the key travels with the code. `stringEqualsIgnoreCaseHash` is the
preferred config section; `stringEqigcaseHash` remains a supported legacy spelling.
A fixed 256-slot cache reuses decoded comparison literals up to 1,024 UTF-16 units.
Keys include the encoded value and mask key; collisions replace an entry and never
establish equality. Larger strings bypass the cache. This retains a bounded amount
of decoded text to reduce allocations; it does not make embedded text secret.

Type transformations pass the actual target Class to `Class.isInstance`. Name hashes
cannot distinguish identically named classes with different defining loaders. No
Class/ClassLoader objects are held in SDK caches. `instanceOfHash` and the legacy
`typeCheck` section both configure this pass. Generated SDK packages include the
tool version, so modules protected by different versions do not resolve incompatible
helpers from each other's JARs. The native tests retain null and array behavior.

Records and serializable/externalizable classes are preserved to retain canonical
constructors, serialized form and compatibility with already persisted data.
Their preservation reason appears in the plan. Application callers can still be
transformed. Runtime-visible annotations, resources and service-provider files
are retained. The framework suite verifies reflection, records, enums, lambdas,
monitors, exception/finally paths, current and pre-protection serialization, SPI,
JNI, Spring Boot, Hibernate/H2 and Jackson.

A class rule with no member clauses preserves the whole matching class. A rule
with member clauses scopes preservation to those members. An inclusion inside a
rule overrides matching exclusions in that rule. Independent preservation rules
are combined conservatively: another class-wide rule can still preserve the
class. Names use anchored glob matching, with literal punctuation escaped.
Explicit `method()` matches zero arguments; omitting parentheses leaves arguments
unconstrained. Type, owner and modifier restrictions are checked.

```hocon
exclude: ["""
  @class com.example.Model {
    @method save*
    !@method saveTemporary()
    @field serialVersionUID
  }
"""]
```

Purity analysis distinguishes PURE, IMPURE and UNSUPPORTED. Object/array-input
proofs remain unsupported and ineligible for hash selection; model tests assert
that boundary explicitly. SSVM hashes use a restricted set of Java SE integer
functions whose specified behavior is stable across target JDK releases. Methods
absent from the pinned internal VM are excluded from selection.

## Input packaging

Versioned classes in multi-release inputs are rejected before transformation;
the importer must not flatten release variants into one class. Signed inputs
must be rebuilt unsigned, protected and signed again. Executable Spring Boot
inputs must be protected as thin JARs before repackaging. The framework suite
repackages the protected thin JAR and executes the resulting Boot artifact.
These packaging refusals preserve the prior destination and produce an explicit
failed report; they are not successful protection results.

## Downloads, dependencies and updates

The internal Corretto image and automatic common-library mappings have pinned
HTTPS locations and SHA-256 hashes. Downloads and extraction are bounded;
traversal, links, duplicate overwrite and invalid digests are rejected. A file
lock serializes cache installation, and only completed verified cache directories
are imported. Legacy/unverified mapping directories are not automatically read.
The cache layout includes a digest prefix; an invalid cache requires explicit
removal rather than merging partial contents. The internal image is for SSVM's
class-library model, independently of the build/deployment JDK.

Local library/JDK metadata caches include the input SHA-256 and schema version
in their keys, retain interfaces/generic signatures and preserve all UTF-16
string code units through JSON escapes. Replacing a same-named library cannot
reuse stale metadata. Cache writes are atomic and corrupt metadata fails
explicitly. Cold imports read declarations one class at a time and skip code, frames, debug
information and resources. SHA-256 remains content-based; changed inputs still
invalidate the cache. Analysis nodes are materialized on first lookup and reuse
identity within the session. Source archive handles are closed and do not use
URLConnection's process-wide JAR cache.
Multi-release dependencies select the highest declared variant supported by the
deployment JDK. The selected release is recorded in the report and is part of
the metadata cache key; transforming multi-release application inputs still
requires a separate per-release pipeline and is refused.

GUI Maven downloads use the reviewed SHA-256 catalog embedded from the versioned
Gradle verification manifest. An unknown version is not downloaded automatically;
verify it externally and add its local JAR explicitly. A digest proves the pinned
bytes, not the absence of vulnerabilities or malicious source. Build inputs and
libraries must be trusted; the tool is not a sandbox for hostile JARs.

```bash
./gradlew --dependency-verification strict generateSbom
python3 scripts/audit-dependencies.py build/reports/sbom.cdx.json build/reports/dependency-audit.json
python3 scripts/test-audit.py
```

The CycloneDX inventory describes the distribution's resolved runtime components,
dependency edges and SHA-256 values. The OSV check fails on findings, an empty or
incomplete inventory, or service/network failure. Its result is an observation at
the report time and depends on advisory coverage. Dependabot proposes weekly
updates. Review upstream changes, lockfiles and verification checksums together;
then rerun the complete validation pipeline.

## Functional and protection evaluation

```bash
./gradlew :integration:jar :integration:writeClasspath :integration:copyDecompiler
EVALUATE_PROTECTION=1 bash scripts/framework-tests.sh
python3 scripts/test-reliability.py
python3 scripts/compatibility-tests.py /path/to/target-jdk 21
```

The compatibility matrix compiles with Java 8/11/17/21/25, protects with the tool
JDK, executes with `-Xverify:all` on the target JVM and checks original class-file
versions. The framework suite executes each profile and 3,000 deterministic
arithmetic/array/switch/exception cases per profile. Different fuzz input seeds
are independent of the protection seed. These are reference applications; run
customer-specific integration tests as a release condition as well.

CFR 0.152 is pinned in the integration tool configuration. Evaluation reports
record input/output hashes, archive sizes, five startup samples and decompiler
outcomes/source counts. Decompiler completion, failure and source size are
observations; none is a security score. The `performance` job compares the candidate and the published tool on the same
runner, interleaving three measured trials after warm-up. It records protection
wall time, process peak RSS, complete JVM run time and a warmed comparison loop.
See [PERFORMANCE.md](PERFORMANCE.md) for regression margins and reproduction.
Customer throughput/latency limits still require representative application workloads.

## Signing and threat model

```bash
# Set these securely in the build environment; do not put passwords in arguments.
# SIGNING_KEYSTORE, SIGNING_STOREPASS, SIGNING_ALIAS
# Optional SIGNING_TSA_URL must be HTTPS.
bash scripts/sign-artifact.sh app-protected.jar app-protected-signed.jar
jarsigner -verify -strict -keystore "$SIGNING_KEYSTORE" \
  -storepass:env SIGNING_STOREPASS app-protected-signed.jar
python3 scripts/test-signing.py
```

The signing script requires an existing identity, verifies the signature strictly
and emits a SHA-256 sidecar. It does not create a production identity or publish.
The automated test uses an ephemeral TEST certificate, validates its signature
and checks that changing a signed class is rejected. Production signing requires
your certificate/chain, protected private key and release timestamp policy.

Obfuscation raises analysis cost while preserving functionality. A recipient can
inspect or instrument the running JVM and recover embedded keys/strings. Keep
private signing keys, service credentials and critical authorization decisions in
controlled services. Signature verification needs an independent trust anchor;
a checksum distributed beside an unsigned artifact alone does not authenticate
its publisher. Avoid anti-debug behaviors that introduce random runtime failure.
