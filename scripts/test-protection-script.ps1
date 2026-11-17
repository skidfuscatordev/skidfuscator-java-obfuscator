#Requires -Version 5.1
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$ObfuscatorJar,
    [Parameter(Mandatory = $true)][string]$Java17Home,
    [Parameter(Mandatory = $true)][string]$Java8Home
)
$ErrorActionPreference = 'Stop'
$script = Join-Path $PSScriptRoot 'test-protection.ps1'
$work = Join-Path ([IO.Path]::GetTempPath()) ('protection script test ' + [Guid]::NewGuid().ToString('N'))
$classes = Join-Path $work 'classes'
$libraries = Join-Path $work 'empty libraries'
$inputJar = Join-Path $work 'input application.jar'
$java17 = Join-Path $Java17Home 'bin/java.exe'
$java8 = Join-Path $Java8Home 'bin/java.exe'
$runtime = Join-Path $Java8Home 'jre/lib/rt.jar'
$config = Join-Path $work 'comparisons.hocon'
New-Item -ItemType Directory -Path $classes, $libraries -Force | Out-Null
$comparisonSource = Join-Path $PSScriptRoot '../dev.skidfuscator.integration/src/main/java/fixtures/StringComparisonCases.java'
& (Join-Path $Java8Home 'bin/javac.exe') '-encoding' 'UTF-8' '-d' $classes $comparisonSource
if ($LASTEXITCODE -ne 0) { throw 'Fixture compilation failed' }
& (Join-Path $Java17Home 'bin/jar.exe') '--create' '--file' $inputJar '--main-class' 'fixtures.StringComparisonCases' '-C' $classes '.'
if ($LASTEXITCODE -ne 0) { throw 'Fixture archive failed' }
'stringEncryption.exempt=["class{^fixtures/StringComparisonCases$}"]' | Set-Content -LiteralPath $config -Encoding ASCII
$expected = & $java8 '-Xverify:all' '-jar' $inputJar
if ($LASTEXITCODE -ne 0) { throw 'Original comparison fixture failed' }
$parameters = @{
    InputJar = $inputJar
    LibrariesDirectory = $libraries
    ObfuscatorJar = $ObfuscatorJar
    Java17Executable = $java17
    Java8RuntimeJar = $runtime
    ConfigPath = $config
}
$hash = (Get-FileHash -LiteralPath $inputJar -Algorithm SHA256).Hash
$output = Join-Path $work 'protected application.jar'
& $script @parameters -OutputJar $output
if ($LASTEXITCODE -ne 0) { throw 'Successful validation returned failure' }
$success = Get-ChildItem -LiteralPath $work -Directory | Where-Object { $_.Name -like 'protected application.jar.validation-*' }
if ((Get-Content -LiteralPath (Join-Path $success.FullName 'execution.log') -Raw) -notmatch '(?m)^VALIDATED:') {
    throw 'Successful validation did not log its success'
}
$actual = & $java8 '-Xverify:all' '-jar' $output
if ($LASTEXITCODE -ne 0 -or "$actual" -ne "$expected") { throw 'Protected fixture behavior changed' }

# An output-budget failure occurs after the dry run and must not emit success.
$failedOutput = Join-Path $work 'failed protection.jar'
& $script @parameters -OutputJar $failedOutput -MaxOutputBytes 1
if ($LASTEXITCODE -eq 0) { throw 'Failed protection was reported as success' }
if (Test-Path -LiteralPath $failedOutput) { throw 'Failed protection committed an output' }
$failure = Get-ChildItem -LiteralPath $work -Directory | Where-Object { $_.Name -like 'failed protection.jar.validation-*' }
$failureLog = Get-Content -LiteralPath (Join-Path $failure.FullName 'execution.log') -Raw
if ($failureLog -match '(?m)^VALIDATED:') { throw 'Failure printed a success message' }
if ($failureLog -notmatch '(?m)^FAILED\.') { throw 'Failure status was not logged' }
if (-not (Test-Path -LiteralPath (Join-Path $failure.FullName 'plan.json'))) { throw 'Dry-run diagnostics missing' }
if ((Get-Content -LiteralPath (Join-Path $failure.FullName 'protection.json') -Raw | ConvertFrom-Json).status -ne 'failed') {
    throw 'Protection failure report missing'
}

# A bad configuration must stop at planning without attempting protection.
$badConfig = Join-Path $work 'bad config.hocon'
'unknownConfigurationSection.enabled=true' | Set-Content -LiteralPath $badConfig -Encoding ASCII
$parameters.ConfigPath = $badConfig
$failedPlan = Join-Path $work 'failed plan.jar'
& $script @parameters -OutputJar $failedPlan
if ($LASTEXITCODE -eq 0) { throw 'Invalid dry run was reported as success' }
$planning = Get-ChildItem -LiteralPath $work -Directory | Where-Object { $_.Name -like 'failed plan.jar.validation-*' }
if (Test-Path -LiteralPath (Join-Path $planning.FullName 'protection.json')) { throw 'Protection ran after planning failed' }
if ((Get-Content -LiteralPath (Join-Path $planning.FullName 'execution.log') -Raw) -match '(?m)^VALIDATED:') {
    throw 'Failed planning printed a success message'
}
if ((Get-FileHash -LiteralPath $inputJar -Algorithm SHA256).Hash -ne $hash) { throw 'Original fixture was modified' }
# A deliberately stalled child proves the hard deadline kills it and preserves an existing destination.
$stalledSource = Join-Path $work 'Stalled.java'
'public class Stalled { public static void main(String[] args) throws Exception { Thread.sleep(30000); } }' |
    Set-Content -LiteralPath $stalledSource -Encoding ASCII
& (Join-Path $Java8Home 'bin/javac.exe') '-d' $classes $stalledSource
if ($LASTEXITCODE -ne 0) { throw 'Stalled fixture compilation failed' }
$stalledJar = Join-Path $work 'stalled.jar'
& (Join-Path $Java17Home 'bin/jar.exe') '--create' '--file' $stalledJar '--main-class' 'Stalled' '-C' $classes 'Stalled.class'
if ($LASTEXITCODE -ne 0) { throw 'Stalled fixture archive failed' }
$parameters.ObfuscatorJar = $stalledJar
$parameters.ConfigPath = $config
$previousOutputHash = (Get-FileHash -LiteralPath $output -Algorithm SHA256).Hash
$timer = [Diagnostics.Stopwatch]::StartNew()
& $script @parameters -OutputJar $output -HardTimeoutSeconds 1
if ($LASTEXITCODE -eq 0 -or $timer.Elapsed.TotalSeconds -gt 20) { throw 'Hard deadline was not enforced' }
if ((Get-FileHash -LiteralPath $output -Algorithm SHA256).Hash -ne $previousOutputHash) { throw 'Timeout replaced the previous output' }
if ((Get-FileHash -LiteralPath $inputJar -Algorithm SHA256).Hash -ne $hash) { throw 'Timeout modified input' }
$latest = Get-ChildItem -LiteralPath $work -Directory | Where-Object { $_.Name -like 'protected application.jar.validation-*' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$validation = Get-Content -LiteralPath (Join-Path $latest.FullName 'validation.json') -Raw | ConvertFrom-Json
if ($validation.status -ne 'failed' -or $validation.error -notmatch 'hard timeout') { throw 'Timeout diagnosis missing' }
Write-Host 'PowerShell validation: success, failed plan, failed protection and Java 8 behavior verified'
exit 0
