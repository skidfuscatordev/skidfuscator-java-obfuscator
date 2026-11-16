# Changelog

## 2.2.0 — 2026-11-16

### Protection and reliability

- Add CONSERVATIVE, BALANCED and STRONG profiles with configurable resource bounds.
- Add seeded runs, dry-run plans, offline mode and JSON provenance reports containing
  tool/input/configuration/dependency/runtime hashes and transformation outcomes.
- Validate bytecode, entry counts, checksums and output budgets before atomic
  replacement; propagate failures instead of dropping classes or silently restoring
  unprotected input. Retain the previous output when protection fails before commit.
- Reject output/report aliases of input artifacts, including hard links.
- Fix event ordering, exception propagation, listener isolation, CFG dead blocks,
  natural-loop analysis, exclusion parsing and scoped member-rule precedence.
- Preserve records and serialization contracts, including inherited serializability,
  existing persisted data, interface metadata and generic signatures.
- Use stable Java SE integer hash functions across target JDK versions.
- Import JDK `lib/modules` images when `jmods` are absent, including Temurin 25.
- Select multi-release dependency metadata for the deployment JDK; version library
  caches by input digest, metadata schema and selected Java release.

### Supply chain and distribution

- Pin SSVM to an immutable revision, lock dependencies and enforce SHA-256 artifact
  and Gradle wrapper verification; normalize archive ordering and timestamps.
- Replace Log4j 1.2 with the maintained compatibility bridge and update vulnerable
  runtime dependencies. Generate a CycloneDX SBOM with dependency edges and hashes.
- Add OSV checks that fail on findings or incomplete/unavailable scans, scheduled
  audits and Dependabot updates. Replace stale clean reports after scan failures.
- Verify automatic HTTPS downloads against reviewed hashes, bound extraction and
  reject traversal, archive links and partial cache installations.
- Restrict GUI automatic Maven downloads to the embedded verified catalog.
- Add signing/strict verification with an existing keystore and tamper tests using
  an ephemeral test identity. Signing and publishing remain separate operations.
- Align active Gradle modules, CLI, manifests and SBOM with version 2.2.0.
- Gate distribution artifacts on the complete CI build and compatibility matrix.

### Validation and upgrade notes

- Add framework integration with Spring Boot, Hibernate/H2, Jackson, SPI, JNI,
  reflection, records, enums, lambdas, monitors and serialization.
- Execute each protection profile on Java 8, 11, 17, 21 and 25 with `-Xverify:all`
  and retain input class-file versions. The obfuscator itself requires Java 17+.
- Add 3,000 deterministic differential cases per profile, seed reproduction checks,
  negative resource-budget/configuration tests and CFR/startup/size observations.
- Analytics now require explicit `--analytics`; offline mode suppresses analytics.
- Transformer/configuration failures now abort the run. The unsafe verification
  bypass is refused. Review existing permissive workflows before upgrading.
- Protect thin Spring Boot JARs before repackaging; protect unsigned builds before
  signing. Multi-release application inputs are explicitly refused pending a
  per-release transformation pipeline. Multi-release dependencies are supported.
- Records, serializable classes and methods over instruction budgets can remain
  preserved; the report records these decisions. Structural renaming is not added.
- Keep provenance reports private: they contain original symbols and the seed.
  Decompiler observations do not establish a quantified protection guarantee.

See [RELIABILITY.md](docs/RELIABILITY.md) for options, limits and compatibility
boundaries, and [DEVELOPMENT.md](docs/DEVELOPMENT.md) for validation commands.
