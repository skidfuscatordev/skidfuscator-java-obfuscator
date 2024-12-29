# Variables
$installDir = "$HOME\.skidfuscator"
$wrapperDir = "$HOME\AppData\Local\Microsoft\WindowsApps"
$wrapperPath = "$wrapperDir\skidfuscator.bat"
$repo = "skidfuscatordev/skidfuscator-java-obfuscator"
$jarName = "skidfuscator.jar"

# Check for Java
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "Java is required but not installed. Install Java and try again." -ForegroundColor Red
    Exit 1
}

# Fetch the latest release URL
Write-Host "Fetching the latest Skidfuscator release..."
$releaseData = Invoke-RestMethod -Uri "https://api.github.com/repos/$repo/releases/latest"
$releaseUrl = $releaseData.assets | Where-Object { $_.name -eq $jarName } | Select-Object -ExpandProperty browser_download_url

if (-not $releaseUrl) {
    Write-Host "Failed to find the latest release. Ensure the repository and asset names are correct." -ForegroundColor Red
    Exit 1
}

# Create install directory
Write-Host "Installing Skidfuscator to $installDir..."
if (-not (Test-Path -Path $installDir)) {
    New-Item -ItemType Directory -Path $installDir | Out-Null
}
Invoke-WebRequest -Uri $releaseUrl -OutFile "$installDir\$jarName"

# Create wrapper script
Write-Host "Creating wrapper script at $wrapperPath..."
if (-not (Test-Path -Path $wrapperDir)) {
    New-Item -ItemType Directory -Path $wrapperDir | Out-Null
}
$wrapperContent = @"
@echo off
java -jar $installDir\$jarName %*
"@
Set-Content -Path $wrapperPath -Value $wrapperContent -Encoding ASCII

# Add wrapper directory to PATH
Write-Host "Ensuring $wrapperDir is in the system PATH..."
$envPath = [System.Environment]::GetEnvironmentVariable("Path", [System.EnvironmentVariableTarget]::User)
if (-not $envPath -like "*$wrapperDir*") {
    Write-Host "Adding $wrapperDir to PATH..."
    [System.Environment]::SetEnvironmentVariable("Path", "$envPath;$wrapperDir", [System.EnvironmentVariableTarget]::User)
} else {
    Write-Host "$wrapperDir is already in PATH."
}

Write-Host "Installation complete. Restart your terminal or run 'refreshenv' to update your PATH."
Write-Host "Run 'skidfuscator --help' to get started." -ForegroundColor Green
