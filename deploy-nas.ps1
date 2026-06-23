param(
    [string]$NasUser = "heyanhub",
    [string]$NasHost = "192.168.31.24",
    [string]$RemoteBase = "/home/heyanhub/apps/priestessbot",
    [string]$ManualBat = "src/test/run-pipeline-manual.local.bat",
    [string]$ServerEnabled = "true",
    [int]$ServerPort = 8080,
    [ValidateSet("auto", "host", "docker")]
    [string]$RuntimeMode = "auto"
)

$ErrorActionPreference = "Stop"

function Read-BatEnv {
    param([string]$Path)

    $vars = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*set\s+([^=]+)=(.*)\s*$') {
            $vars[$matches[1].Trim()] = $matches[2]
        }
    }
    return $vars
}

function Get-EnvValue {
    param(
        [hashtable]$Vars,
        [string]$Name,
        [string]$Default = "",
        [switch]$Required
    )

    $value = if ($Vars.ContainsKey($Name)) { $Vars[$Name] } else { $Default }
    if ($Required -and [string]::IsNullOrWhiteSpace($value)) {
        throw "Missing required value '$Name' in $ManualBat"
    }
    return $value
}

function Convert-ToBool {
    param(
        [string]$Value,
        [string]$Name
    )

    switch -Regex ($Value.Trim()) {
        '^(?i:true|1|yes|y)$' { return $true }
        '^(?i:false|0|no|n)$' { return $false }
        default { throw "Invalid boolean value '$Value' for $Name. Use true/false or 1/0." }
    }
}

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $repoRoot

if (-not (Test-Path -LiteralPath $ManualBat)) {
    throw "Cannot find manual config launcher: $ManualBat"
}

$manual = Read-BatEnv -Path $ManualBat

$napcatToken = Get-EnvValue $manual "NAPCAT_ACCESS_TOKEN" -Required
$openAiApiKey = Get-EnvValue $manual "PRIESTESS_OPENAI_PROVIDER_API_KEY" -Required
$providerName = Get-EnvValue $manual "PRIESTESS_OPENAI_PROVIDER_NAME" "deepseek-v4-flash"
$providerModel = Get-EnvValue $manual "PRIESTESS_OPENAI_PROVIDER_MODEL" "deepseek-v4-flash"
$prefix = Get-EnvValue $manual "PRIESTESS_PIPELINE_MANUAL_PREFIX" "/"
$serverEnabledBool = Convert-ToBool -Value $ServerEnabled -Name "ServerEnabled"

$deployDir = Join-Path $repoRoot "build/deploy/nas"
$payloadDir = Join-Path $deployDir "payload"
$appPayloadDir = Join-Path $payloadDir "current"
$configPayloadDir = Join-Path $payloadDir "config"
$packagePath = Join-Path $deployDir "priestessbot-nas.tar.gz"

Write-Host "Building application distribution..."
& .\gradlew.bat --no-daemon clean installDist -PbuildDashboard=true
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed with exit code $LASTEXITCODE"
}

Remove-Item -LiteralPath $payloadDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $appPayloadDir, $configPayloadDir | Out-Null

$distDir = Join-Path $repoRoot "build/install/astrbot.kt"
if (-not (Test-Path -LiteralPath $distDir)) {
    throw "Gradle distribution not found: $distDir"
}

Get-ChildItem -LiteralPath $distDir -Force | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination $appPayloadDir -Recurse -Force
}

$config = [ordered]@{
    platforms = @(
        [ordered]@{
            name = Get-EnvValue $manual "NAPCAT_PLATFORM_NAME" "napcat4_18_6"
            type = "napcat4_18_6"
            enabled = $true
            host = Get-EnvValue $manual "NAPCAT_WS_HOST" "192.168.31.24"
            port = [int](Get-EnvValue $manual "NAPCAT_HTTP_PORT" "10000")
            wsPort = [int](Get-EnvValue $manual "NAPCAT_WS_PORT" "10001")
            token = $napcatToken
            baseUrl = ""
            useWs = $true
            config = @{}
        }
    )
    providers = @(
        [ordered]@{
            name = $providerName
            type = "openai"
            model = $providerModel
            baseUrl = Get-EnvValue $manual "PRIESTESS_OPENAI_PROVIDER_URL" "http://192.168.31.24:8090/v1/chat/completions"
            apiKey = $openAiApiKey
            enabled = $true
            config = @{}
        }
    )
    agent = [ordered]@{
        name = "pipeline-manual-agent"
        instructions = Get-EnvValue $manual "PRIESTESS_PIPELINE_MANUAL_PROMPT" "You are PriestessBot in a manual integration test. Reply briefly and clearly."
        model = $providerModel
        providerName = $providerName
        maxSteps = [int](Get-EnvValue $manual "PRIESTESS_PIPELINE_MANUAL_MAX_STEPS" "6")
        temperature = 0.7
        compressStrategy = "token_window"
        maxRounds = 20
        maxTokens = 4096
        toolTimeoutSeconds = 30
        enabledTools = @()
    }
    database = [ordered]@{
        path = "$RemoteBase/data/pipeline-manual.sqlite"
    }
    pipeline = [ordered]@{
        wakingPrefix = $prefix
        whitelistEnabled = $true
        whitelistUsers = @("1371735400")
        whitelistGroups = @("757063076", "729848189")
        rateLimitEnabled = $false
        rateLimitPerMinute = 20
        sessionEnabledByDefault = $true
        contentSafetyEnabled = $false
        maxHistoryMessages = 10
    }
    server = [ordered]@{
        enabled = $serverEnabledBool
        host = "0.0.0.0"
        port = $ServerPort
        corsEnabled = $true
        configWatchEnabled = $true
        configWatchIntervalMillis = 2000
    }
    plugins = [ordered]@{
        enabled = $true
        directory = "$RemoteBase/plugins"
        autoDiscover = $true
    }
    subAgents = [ordered]@{
        enabled = $false
        defaultAgentName = ""
        agents = @()
        routes = @()
    }
}

$configPath = Join-Path $configPayloadDir "config.json"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($configPath, ($config | ConvertTo-Json -Depth 20), $utf8NoBom)

if (Test-Path -LiteralPath $packagePath) {
    Remove-Item -LiteralPath $packagePath -Force
}

Write-Host "Creating deployment package..."
tar -czf $packagePath -C $payloadDir .
if ($LASTEXITCODE -ne 0) {
    throw "Failed to create deployment package"
}

$remote = "${NasUser}@${NasHost}"
$remotePackage = "/tmp/priestessbot-nas.tar.gz"
$serverEnabledText = if ($serverEnabledBool) { "true" } else { "false" }

Write-Host "Uploading package to $remote..."
scp $packagePath "${remote}:$remotePackage"
if ($LASTEXITCODE -ne 0) {
    throw "Upload failed"
}

$remoteScript = @"
set -eu
REMOTE_BASE='$RemoteBase'
PACKAGE='$remotePackage'
APP_PID="`$REMOTE_BASE/app.pid"
RUNTIME_MODE='$RuntimeMode'
SERVER_ENABLED='$serverEnabledText'
SERVER_PORT='$ServerPort'
CONTAINER_NAME='priestessbot'
RUN_AS_USER="`$(id -u):`$(id -g)"

mkdir -p "`$REMOTE_BASE" "`$REMOTE_BASE/logs" "`$REMOTE_BASE/data" "`$REMOTE_BASE/config"
mkdir -p "`$REMOTE_BASE/plugins"

if [ -f "`$APP_PID" ]; then
  OLD_PID=`$(cat "`$APP_PID" || true)
  if [ -n "`$OLD_PID" ] && kill -0 "`$OLD_PID" 2>/dev/null; then
    kill "`$OLD_PID" || true
    sleep 2
  fi
  rm -f "`$APP_PID"
fi

if command -v docker >/dev/null 2>&1; then
  docker rm -f "`$CONTAINER_NAME" >/dev/null 2>&1 || true
fi

rm -rf "`$REMOTE_BASE/current.new"
mkdir -p "`$REMOTE_BASE/current.new"
tar -xzf "`$PACKAGE" -C "`$REMOTE_BASE/current.new"

rm -rf "`$REMOTE_BASE/current"
mv "`$REMOTE_BASE/current.new/current" "`$REMOTE_BASE/current"
cp "`$REMOTE_BASE/current.new/config/config.json" "`$REMOTE_BASE/config/config.json"
rm -rf "`$REMOTE_BASE/current.new"
rm -f "`$PACKAGE"

chmod +x "`$REMOTE_BASE/current/bin/astrbot.kt" || true

SELECTED_RUNTIME="`$RUNTIME_MODE"
if [ "`$SELECTED_RUNTIME" = "auto" ]; then
  if command -v java >/dev/null 2>&1; then
    SELECTED_RUNTIME="host"
  elif command -v docker >/dev/null 2>&1; then
    SELECTED_RUNTIME="docker"
  else
    echo "Neither Java nor Docker is available on the NAS." >&2
    exit 1
  fi
fi

if [ "`$SELECTED_RUNTIME" = "host" ]; then
  if ! command -v java >/dev/null 2>&1; then
    echo "Java is required for host runtime. Install Java 21 runtime or use -RuntimeMode docker." >&2
    exit 1
  fi

  cd "`$REMOTE_BASE/current"
  PRIESTESS_CONFIG_PATH="`$REMOTE_BASE/config/config.json" nohup ./bin/astrbot.kt > "`$REMOTE_BASE/logs/app.log" 2>&1 &
  echo `$! > "`$APP_PID"
  STARTED_PID=`$(cat "`$APP_PID")
  echo "PriestessBot started on host with PID `$STARTED_PID"
elif [ "`$SELECTED_RUNTIME" = "docker" ]; then
  if ! command -v docker >/dev/null 2>&1; then
    echo "Docker is required for docker runtime." >&2
    exit 1
  fi

  if [ "`$SERVER_ENABLED" = "true" ]; then
    docker run -d \
      --name "`$CONTAINER_NAME" \
      --restart unless-stopped \
      --user "`$RUN_AS_USER" \
      -p "`$SERVER_PORT:`$SERVER_PORT" \
      -v "`$REMOTE_BASE:`$REMOTE_BASE" \
      -w "`$REMOTE_BASE/current" \
      -e "PRIESTESS_CONFIG_PATH=`$REMOTE_BASE/config/config.json" \
      eclipse-temurin:21-jre \
      ./bin/astrbot.kt >/dev/null
  else
    docker run -d \
      --name "`$CONTAINER_NAME" \
      --restart unless-stopped \
      --user "`$RUN_AS_USER" \
      -v "`$REMOTE_BASE:`$REMOTE_BASE" \
      -w "`$REMOTE_BASE/current" \
      -e "PRIESTESS_CONFIG_PATH=`$REMOTE_BASE/config/config.json" \
      eclipse-temurin:21-jre \
      ./bin/astrbot.kt >/dev/null
  fi
  echo "PriestessBot started in Docker container `$CONTAINER_NAME"
else
  echo "Unknown runtime mode: `$SELECTED_RUNTIME" >&2
  exit 1
fi
"@

Write-Host "Deploying and restarting on NAS..."
$remoteScript = $remoteScript -replace "`r`n", "`n" -replace "`r", "`n"
$remoteScript | ssh $remote "tr -d '\r' | sh -s"
if ($LASTEXITCODE -ne 0) {
    throw "Remote deploy failed"
}

Write-Host ""
Write-Host "Deployment complete."
if ($serverEnabledBool) {
    Write-Host "Dashboard/API: http://$NasHost`:$ServerPort"
}
Write-Host "Logs: ssh $remote `"tail -f $RemoteBase/logs/app.log`""
Write-Host "Docker logs: ssh $remote `"docker logs -f priestessbot`""
