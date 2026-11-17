#Requires -Version 5.1
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$InputJar,
    [Parameter(Mandatory = $true)][string]$LibrariesDirectory,
    [Parameter(Mandatory = $true)][string]$ObfuscatorJar,
    [Parameter(Mandatory = $true)][string]$Java17Executable,
    [Parameter(Mandatory = $true)][string]$Java8RuntimeJar,
    [string]$OutputJar,
    [string]$ConfigPath,
    [ValidateSet('CONSERVATIVE', 'BALANCED', 'STRONG')][string]$Profile = 'CONSERVATIVE',
    [long]$Seed = 123456,
    [ValidateRange(1, 2147483647)][int]$MaxDurationMs = 900000,
    [ValidateRange(1, 2147483647)][long]$MaxOutputBytes = 536870912,
    [ValidateRange(1, 2147483)][int]$HardTimeoutSeconds = 1200,
    [ValidatePattern('^[1-9][0-9]*[mMgG]$')][string]$HeapSize = '3g'
)

# Keep the whole operation inside one script invocation. In an interactive
# console, throwing from one pasted command does not stop later pasted commands.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$exitCode = 1
$originalHash = $null
$diagnosticDirectory = $null
$transcriptStarted = $false
$locationPushed = $false
$protectionReport = $null
$candidate = $null
$failureMessage = $null

function Invoke-SupervisedJava([string]$Phase, [string[]]$Arguments) {
    # Windows command-line quoting, including trailing backslashes and embedded quotes.
    $quoted = ($Arguments | ForEach-Object {
        '"' + [regex]::Replace([regex]::Replace($_, '(\\*)"', '$1$1\"'), '(\\+)$', '$1$1') + '"'
    }) -join ' '
    $stdout = Join-Path $diagnosticDirectory ($Phase + '.stdout.log')
    $stderr = Join-Path $diagnosticDirectory ($Phase + '.stderr.log')
    $process = Start-Process -FilePath $Java17Executable -ArgumentList $quoted -NoNewWindow -PassThru `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    try {
        $processHandle = $process.Handle
        if (-not $process.WaitForExit($HardTimeoutSeconds * 1000)) {
            & taskkill.exe /PID $process.Id /T /F | Out-Null
            if (-not $process.WaitForExit(10000)) { $process.Kill(); $process.WaitForExit() }
            throw "$Phase exceeded the hard timeout of $HardTimeoutSeconds seconds; destination was not replaced"
        }
        $process.WaitForExit()
        return $process.ExitCode
    }
    finally {
        foreach ($log in @($stdout, $stderr)) {
            if (Test-Path -LiteralPath $log) { Get-Content -LiteralPath $log | ForEach-Object { Write-Host $_ } }
        }
        $process.Dispose()
    }
}


try {
    foreach ($path in @($InputJar, $ObfuscatorJar, $Java17Executable, $Java8RuntimeJar)) {
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            throw "Required file not found: $path"
        }
    }
    if (-not (Test-Path -LiteralPath $LibrariesDirectory -PathType Container)) {
        throw "Library directory not found: $LibrariesDirectory"
    }
    $InputJar = (Get-Item -LiteralPath $InputJar).FullName
    $ObfuscatorJar = (Get-Item -LiteralPath $ObfuscatorJar).FullName
    $Java17Executable = (Get-Item -LiteralPath $Java17Executable).FullName
    $Java8RuntimeJar = (Get-Item -LiteralPath $Java8RuntimeJar).FullName
    $LibrariesDirectory = (Get-Item -LiteralPath $LibrariesDirectory).FullName
    if (-not [string]::IsNullOrWhiteSpace($ConfigPath)) {
        if (-not (Test-Path -LiteralPath $ConfigPath -PathType Leaf)) { throw "Configuration file not found: $ConfigPath" }
        $ConfigPath = (Get-Item -LiteralPath $ConfigPath).FullName
    }
    if ([string]::IsNullOrWhiteSpace($OutputJar)) {
        $OutputJar = Join-Path ([IO.Path]::GetDirectoryName($InputJar)) ([IO.Path]::GetFileNameWithoutExtension($InputJar) + '-protected.jar')
    }
    $OutputJar = [IO.Path]::GetFullPath($OutputJar)
    if ([string]::Equals($InputJar, $OutputJar, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Input and output must be different files'
    }
    $sourcePaths = @($InputJar, $ObfuscatorJar, $Java17Executable, $Java8RuntimeJar)
    if ($ConfigPath) { $sourcePaths += $ConfigPath }
    $sourcePaths += @(Get-ChildItem -LiteralPath $LibrariesDirectory -Filter '*.jar' -File | ForEach-Object { $_.FullName })
    foreach ($sourcePath in $sourcePaths) {
        if ([string]::Equals($OutputJar, $sourcePath, [StringComparison]::OrdinalIgnoreCase)) {
            throw "Output must differ from every input artifact: $sourcePath"
        }
    }
    $originalHash = (Get-FileHash -LiteralPath $InputJar -Algorithm SHA256).Hash
    $diagnosticDirectory = $OutputJar + '.validation-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0, 8)
    New-Item -ItemType Directory -Path $diagnosticDirectory -Force | Out-Null
    $candidate = Join-Path $diagnosticDirectory 'candidate.jar'
    $planPath = Join-Path $diagnosticDirectory 'plan.json'
    $reportPath = Join-Path $diagnosticDirectory 'protection.json'
    Start-Transcript -Path (Join-Path $diagnosticDirectory 'execution.log') | Out-Null
    $transcriptStarted = $true
    Push-Location ([IO.Path]::GetDirectoryName($InputJar))
    $locationPushed = $true
    $options = @('obfuscate', $InputJar, '--runtime', $Java8RuntimeJar, '--libs', $LibrariesDirectory,
        '--output', $candidate, '--profile', $Profile, '--seed', "$Seed",
        '--max-duration-ms', "$MaxDurationMs", '--max-output-bytes', "$MaxOutputBytes", '--non-interactive')
    if (-not [string]::IsNullOrWhiteSpace($ConfigPath)) { $options += @('--config', $ConfigPath) }

    $nativeExit = Invoke-SupervisedJava 'plan' (@("-Xmx$HeapSize", '-jar', $ObfuscatorJar) + $options + @('--dry-run', '--report', $planPath))
    if ($nativeExit -ne 0) { throw "Dry run failed with exit code $nativeExit. Diagnostics: $diagnosticDirectory" }
    $plan = Get-Content -LiteralPath $planPath -Raw | ConvertFrom-Json
    if ($plan.status -ne 'planned' -or $plan.inputSha256 -ne $originalHash) {
        throw 'The dry-run report did not confirm the selected input'
    }

    $nativeExit = Invoke-SupervisedJava 'protection' (@("-Xmx$HeapSize", '-jar', $ObfuscatorJar) + $options + @('--report', $reportPath))
    if ($nativeExit -ne 0) { throw "Protection failed with exit code $nativeExit. Diagnostics: $diagnosticDirectory" }
    $protectionReport = Get-Content -LiteralPath $reportPath -Raw | ConvertFrom-Json
    if ($protectionReport.status -ne 'succeeded' -or $protectionReport.inputSha256 -ne $originalHash) {
        throw 'The protection report did not confirm success for the selected input'
    }
    if (-not (Test-Path -LiteralPath $candidate -PathType Leaf)) { throw 'Protected JAR was not generated' }
    if ((Get-FileHash -LiteralPath $candidate -Algorithm SHA256).Hash -ne $protectionReport.outputSha256) {
        throw 'Protected JAR checksum does not match the report'
    }
    if ((Get-FileHash -LiteralPath $InputJar -Algorithm SHA256).Hash -ne $originalHash) { throw 'Input JAR changed during validation' }
    if (Test-Path -LiteralPath $OutputJar) { [IO.File]::Replace($candidate, $OutputJar, $null) }
    else { [IO.File]::Move($candidate, $OutputJar) }
    $exitCode = 0
}
catch {
    $failureMessage = $_.Exception.Message
    Write-Error -ErrorRecord $_ -ErrorAction Continue
}
finally {
    if ($originalHash) {
        try {
            if ((Get-FileHash -LiteralPath $InputJar -Algorithm SHA256).Hash -ne $originalHash) {
                throw 'Input JAR changed during validation'
            }
        }
        catch {
            $exitCode = 1
            Write-Error -ErrorRecord $_ -ErrorAction Continue
        }
    }
    if ($candidate -and (Test-Path -LiteralPath $candidate)) { Remove-Item -LiteralPath $candidate -Force }
    if ($diagnosticDirectory -and (Test-Path -LiteralPath $diagnosticDirectory)) {
        @{ status = $(if ($exitCode -eq 0) { 'succeeded' } else { 'failed' }); error = $failureMessage; hardTimeoutSeconds = $HardTimeoutSeconds } |
            ConvertTo-Json | Set-Content -LiteralPath (Join-Path $diagnosticDirectory 'validation.json') -Encoding UTF8
    }
    if ($locationPushed) { Pop-Location }
    if ($exitCode -eq 0) {
        $protectionReport | Select-Object status, toolVersion, profile, seed, durationMillis, outputBytes | Format-List | Out-Host
        Write-Host "VALIDATED: $OutputJar"
    } elseif ($diagnosticDirectory) {
        Write-Host "FAILED. Review diagnostics in: $diagnosticDirectory"
    }
    if ($transcriptStarted) { Stop-Transcript | Out-Null }
}
exit $exitCode
