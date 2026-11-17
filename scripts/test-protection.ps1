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
    $originalHash = (Get-FileHash -LiteralPath $InputJar -Algorithm SHA256).Hash
    $diagnosticDirectory = $OutputJar + '.validation-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0, 8)
    New-Item -ItemType Directory -Path $diagnosticDirectory -Force | Out-Null
    $planPath = Join-Path $diagnosticDirectory 'plan.json'
    $reportPath = Join-Path $diagnosticDirectory 'protection.json'
    Start-Transcript -Path (Join-Path $diagnosticDirectory 'execution.log') | Out-Null
    $transcriptStarted = $true
    Push-Location ([IO.Path]::GetDirectoryName($InputJar))
    $locationPushed = $true
    $options = @('obfuscate', $InputJar, '--runtime', $Java8RuntimeJar, '--libs', $LibrariesDirectory,
        '--output', $OutputJar, '--profile', $Profile, '--seed', "$Seed",
        '--max-duration-ms', "$MaxDurationMs", '--max-output-bytes', "$MaxOutputBytes", '--non-interactive')
    if (-not [string]::IsNullOrWhiteSpace($ConfigPath)) { $options += @('--config', $ConfigPath) }

    & $Java17Executable "-Xmx$HeapSize" '-jar' $ObfuscatorJar @options '--dry-run' '--report' $planPath
    if ($LASTEXITCODE -ne 0) { throw "Dry run failed with exit code $LASTEXITCODE. Diagnostics: $diagnosticDirectory" }
    $plan = Get-Content -LiteralPath $planPath -Raw | ConvertFrom-Json
    if ($plan.status -ne 'planned' -or $plan.inputSha256 -ne $originalHash) {
        throw 'The dry-run report did not confirm the selected input'
    }

    & $Java17Executable "-Xmx$HeapSize" '-jar' $ObfuscatorJar @options '--report' $reportPath
    if ($LASTEXITCODE -ne 0) { throw "Protection failed with exit code $LASTEXITCODE. Diagnostics: $diagnosticDirectory" }
    $protectionReport = Get-Content -LiteralPath $reportPath -Raw | ConvertFrom-Json
    if ($protectionReport.status -ne 'succeeded' -or $protectionReport.inputSha256 -ne $originalHash) {
        throw 'The protection report did not confirm success for the selected input'
    }
    if (-not (Test-Path -LiteralPath $OutputJar -PathType Leaf)) { throw 'Protected JAR was not generated' }
    if ((Get-FileHash -LiteralPath $OutputJar -Algorithm SHA256).Hash -ne $protectionReport.outputSha256) {
        throw 'Protected JAR checksum does not match the report'
    }
    $exitCode = 0
}
catch {
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
