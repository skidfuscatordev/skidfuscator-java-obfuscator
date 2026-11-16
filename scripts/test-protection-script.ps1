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
if ((Get-FileHash -LiteralPath $inputJar -Algorithm SHA256).Hash -ne $hash) { throw 'Original fixture was modified' }
Write-Host 'PowerShell validation: success, failed plan, failed protection and Java 8 behavior verified'
exit 0
