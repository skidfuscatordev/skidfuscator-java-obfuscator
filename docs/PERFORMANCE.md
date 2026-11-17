# Stability and performance checks

Run with the build JDK 17 and the verified internal SSVM runtime:

```bash
python3 scripts/performance-tests.py --baseline /path/to/previous/skidfuscator.jar
```

The baseline must be a previously validated distribution. CI downloads the
published `main-build` bundle from this repository and verifies its checksums
before comparing it with the candidate. The benchmark uses generated reference
classes and never starts a customer application.

The benchmark generates 120 classes plus a comparison loop, protects with the
CONSERVATIVE profile and seed 42, and verifies behavior with `-Xverify:all`.
String encryption is excluded only for the loop fixture to ensure comparison
transformations execute. Each tool gets one protection warm-up followed by three
interleaved trials on the same host. The Java loop warms up independently inside
each fresh JVM. The report includes both artifact hashes, individual samples,
medians and the candidate's stage diagnostics. It also loads two modules protected
with different tools/seeds in one application loader and checks type behavior.

Publication gates use generous regression margins to tolerate shared-runner noise:

| Measurement | Maximum candidate result relative to baseline median |
|---|---|
| Protection wall time | 1.5 times baseline plus 500 ms |
| Warmed loop time | 1.5 times baseline plus 20 ms |
| Protection peak RSS | 1.25 times baseline plus 32 MiB |

These bounds detect substantial regressions on this fixture. They are not an
application latency SLA or a promised optimization percentage. Complete JVM run
time is reported separately; the loop's warm-up/measurement work means that value
must not be presented as pure startup time. Peak RSS uses the operating system's
child resource accounting on POSIX, independently of the JVM's pool peaks; it is
unavailable on platforms without `wait4` and the report explicitly uses null.
GitHub's Linux job requires and checks RSS measurements.

Use `--jfr` to retain Java Flight Recorder profiles next to the report for both
tools. For a representative local protection run, the same recording can be
requested directly with `-XX:StartFlightRecording=settings=profile,filename=run.jfr`
before `-jar`. Keep profiles and run reports private because they contain symbols.

The report schema's phase measurements distinguish declaration import, runtime
metadata, hierarchy/graphs, individual transformation phases and output validation.
IR required by global analyses remains available until those passes finish; no
mutable method graph is evicted or rebuilt halfway through transformation.
Library metadata is loaded lazily and all supplied dependencies remain available,
including dependencies reached through reflection or service discovery.
