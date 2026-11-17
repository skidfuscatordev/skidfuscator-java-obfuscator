# Validate protection without starting the application

Run `scripts/test-protection.ps1` as one PowerShell script invocation. It supports
Windows PowerShell 5.1. Pasting separate console commands can continue after an
earlier `throw`; a final success message alone does not prove validation passed.

```powershell
$parameters = @{
    InputJar = 'D:\apps\example\application.jar'
    LibrariesDirectory = 'D:\apps\example\lib'
    ObfuscatorJar = 'D:\tools\skidfuscator.jar'
    Java17Executable = 'D:\jdks\jdk17\bin\java.exe'
    Java8RuntimeJar = 'D:\jdks\jdk8\jre\lib\rt.jar'
    OutputJar = 'D:\apps\example\application-protected.jar'
}
& .\scripts\test-protection.ps1 @parameters
```

The script runs the obfuscator with Java 17, the provided Java 8 library model,
CONSERVATIVE profile, seed 123456 and a 900,000 ms time budget by default. It does
not start the application's main class. `-ConfigPath` can select a HOCON config;
profile, seed, heap and output/time budgets have explicit parameters.
`-HardTimeoutSeconds` defaults to 1,200 seconds for each Java invocation. The script
terminates a stalled process tree and records failure in `validation.json`. It
protects into `candidate.jar` in the diagnostics directory and moves/replaces the
requested output only after the exit code, report and all checksums pass. A timeout
or validation failure retains any previous destination. Java stdout/stderr are
written to separate files while running and copied to the transcript afterward.

It first requires a successful dry-run report, then requires a zero protection
exit code, a `succeeded` report for the same input hash and a matching output
SHA-256. It verifies the input hash again even after failures. Only all-successful
runs print `VALIDATED:` and return exit code 0. Failures return exit code 1 and do
not try to hash an output that was never generated.

Each attempt records `execution.log`, `plan.json` and, if protection was reached,
`protection.json`, `validation.json` and separate Java logs in a new `<output>.validation-<timestamp>-<id>` directory. Keep
these diagnostics private because they contain original symbols and the seed.

The `windows-script` CI job runs this on Windows PowerShell 5.1 with Java 17 and
Java 8, including paths containing spaces. It checks successful behavior on a
small reference application, a failed dry run and a failed protection run caused
by an output budget, plus a stalled child with an existing destination. Publication requires this job to pass as well. Customer
application startup and functional testing remain separate.
