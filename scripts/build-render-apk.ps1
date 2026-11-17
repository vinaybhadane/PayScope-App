param(
    [Parameter(Mandatory = $true)]
    [uri]$ApiUrl
)

$ErrorActionPreference = 'Stop'
if (-not $ApiUrl.IsAbsoluteUri -or $ApiUrl.Scheme -ne 'https' -or
    $ApiUrl.AbsolutePath -ne '/' -or $ApiUrl.Query -or $ApiUrl.Fragment -or $ApiUrl.UserInfo) {
    throw 'Supply only the HTTPS service root, for example https://your-service.onrender.com/.'
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$baseUrl = $ApiUrl.AbsoluteUri.TrimEnd('/') + '/'
Write-Host 'Checking the hosted API. A sleeping free service can take about a minute to start.'
$health = Invoke-RestMethod -Uri ($baseUrl + 'api/v1/health') -TimeoutSec 150
$ready = Invoke-RestMethod -Uri ($baseUrl + 'api/v1/ready') -TimeoutSec 60
if ($health.status -ne 'ok' -or $ready.status -ne 'ready') {
    throw 'The hosted API or its database is not ready. Check Render logs before building.'
}

Push-Location -LiteralPath $projectRoot
try {
    & (Join-Path $projectRoot 'gradlew.bat') ':app:verifyFirebaseConfiguration' ':app:assembleDebug' "-PPAYSCOPE_API_BASE_URL=$baseUrl" '--console=plain'
    if ($LASTEXITCODE -ne 0) { throw 'The Android build failed.' }
    $destination = Join-Path $projectRoot 'output\android'
    New-Item -ItemType Directory -Path $destination -Force | Out-Null
    $apk = Join-Path $destination 'payscope-render-debug.apk'
    Copy-Item -LiteralPath (Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk') -Destination $apk -Force
    Write-Host "Hosted-backend APK: $apk"
}
finally {
    Pop-Location
}
