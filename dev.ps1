[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet('start', 'infra', 'status', 'stop', '__api', '__web')]
    [string]$Command = 'start',

    [switch]$NoBrowser
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = $PSScriptRoot
$stateDirectory = Join-Path $repoRoot '.dev'
$statePath = Join-Path $stateDirectory 'processes.json'
$apiHealthUrl = 'http://127.0.0.1:8080/actuator/health'
$apiInfoUrl = 'http://127.0.0.1:8080/actuator/info'
$webUrl = 'http://127.0.0.1:5173'
$apiHealthMarker = '"status":"UP"'
$apiIdentityMarker = '"name":"project-atlas"'
$webMarker = '<title>Project Atlas</title>'
$repoIdentity = [System.IO.Path]::GetFullPath($repoRoot).ToUpperInvariant()
$hashAlgorithm = [System.Security.Cryptography.SHA256]::Create()
try {
    $repoHashBytes = $hashAlgorithm.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($repoIdentity))
}
finally {
    $hashAlgorithm.Dispose()
}
$repoHash = [BitConverter]::ToString($repoHashBytes).Replace('-', '').Substring(0, 24)
$launcherMutexName = "Local\ProjectAtlas.DevLauncher.$repoHash"
$atlasEnvironmentNames = @(
    'ATLAS_DB_NAME',
    'ATLAS_DB_USER',
    'ATLAS_DB_PASSWORD',
    'ATLAS_DB_PORT',
    'ATLAS_DB_URL',
    'ATLAS_AI_PROVIDER'
)

function Write-Step {
    param([string]$Message)

    Write-Host "[atlas] $Message" -ForegroundColor Cyan
}

function Invoke-WithLauncherLock {
    param([scriptblock]$Action)

    $mutex = New-Object System.Threading.Mutex($false, $launcherMutexName)
    $lockAcquired = $false
    try {
        try {
            $lockAcquired = $mutex.WaitOne(0)
        }
        catch [System.Threading.AbandonedMutexException] {
            $lockAcquired = $true
        }

        if (-not $lockAcquired) {
            Write-Step 'Another launcher command is running; waiting for it to finish...'
            try {
                $lockAcquired = $mutex.WaitOne([TimeSpan]::FromSeconds(120))
            }
            catch [System.Threading.AbandonedMutexException] {
                $lockAcquired = $true
            }
        }

        if (-not $lockAcquired) {
            throw 'Timed out waiting for another launcher command to finish.'
        }

        & $Action
    }
    finally {
        if ($lockAcquired) {
            $mutex.ReleaseMutex()
        }
        $mutex.Dispose()
    }
}

function Get-ExecutablePath {
    param([string]$Name)

    $commandInfo = Get-Command $Name -ErrorAction SilentlyContinue
    if ($null -eq $commandInfo) {
        throw "Required command '$Name' was not found on PATH."
    }

    return $commandInfo.Source
}

function Import-AtlasEnvironment {
    param([string]$DockerPath)

    $resolvedEnvironment = @(
        & $DockerPath compose --project-directory $repoRoot config --environment
    )
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose environment resolution failed (exit code $LASTEXITCODE)."
    }

    foreach ($line in $resolvedEnvironment) {
        $separator = $line.IndexOf('=')
        if ($separator -lt 1) {
            continue
        }

        $name = $line.Substring(0, $separator)
        if ($atlasEnvironmentNames -contains $name) {
            $value = $line.Substring($separator + 1)
            Set-Item -Path "Env:$name" -Value $value
        }
    }

    if ([string]::IsNullOrWhiteSpace($env:ATLAS_DB_URL)) {
        $databaseName = if ($env:ATLAS_DB_NAME) { $env:ATLAS_DB_NAME } else { 'atlas' }
        $databasePort = if ($env:ATLAS_DB_PORT) { $env:ATLAS_DB_PORT } else { '55432' }
        $env:ATLAS_DB_URL = "jdbc:postgresql://localhost:$databasePort/$databaseName"
    }
}

function Get-AtlasEnvironmentSnapshot {
    $snapshot = @{}
    foreach ($name in $atlasEnvironmentNames) {
        $existing = Get-Item -Path "Env:$name" -ErrorAction SilentlyContinue
        $snapshot[$name] = if ($null -eq $existing) {
            [pscustomobject]@{ Exists = $false; Value = $null }
        }
        else {
            [pscustomobject]@{ Exists = $true; Value = $existing.Value }
        }
    }
    return $snapshot
}

function Restore-AtlasEnvironment {
    param([hashtable]$Snapshot)

    foreach ($name in $atlasEnvironmentNames) {
        $entry = $Snapshot[$name]
        if ($entry.Exists) {
            Set-Item -Path "Env:$name" -Value $entry.Value
        }
        else {
            Remove-Item -Path "Env:$name" -ErrorAction SilentlyContinue
        }
    }
}

function Test-DockerEngine {
    param([string]$DockerPath)

    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $DockerPath
    $startInfo.Arguments = 'info --format "{{.ServerVersion}}"'
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    try {
        if (-not $process.Start()) {
            return $false
        }
        if (-not $process.WaitForExit(8000)) {
            $process.Kill()
            return $false
        }

        return $process.ExitCode -eq 0
    }
    finally {
        $process.Dispose()
    }
}

function Require-DockerEngine {
    $dockerPath = Get-ExecutablePath 'docker'
    if (-not (Test-DockerEngine $dockerPath)) {
        throw 'Docker Engine is unavailable. Start Docker Desktop, wait until it is ready, and try again.'
    }

    return $dockerPath
}

function Start-Postgres {
    param([string]$DockerPath)

    Write-Step 'Starting PostgreSQL and waiting for its health check...'
    & $DockerPath compose --project-directory $repoRoot up -d --wait postgres
    if ($LASTEXITCODE -ne 0) {
        throw "PostgreSQL failed to start (docker compose exit code $LASTEXITCODE)."
    }
}

function Test-Endpoint {
    param(
        [string]$Uri,
        [string]$ExpectedText
    )

    try {
        $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 2
        $content = if ($response.Content -is [byte[]]) {
            [System.Text.Encoding]::UTF8.GetString($response.Content)
        }
        else {
            [string]$response.Content
        }
        return $response.StatusCode -ge 200 -and
            $response.StatusCode -lt 400 -and
            $content.Contains($ExpectedText)
    }
    catch {
        return $false
    }
}

function Test-TcpPort {
    param([int]$Port)

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $connection = $client.ConnectAsync('127.0.0.1', $Port)
        if (-not $connection.Wait(750)) {
            return $false
        }
        return $client.Connected
    }
    catch {
        return $false
    }
    finally {
        $client.Dispose()
    }
}

function Test-ApiReady {
    return (Test-Endpoint $apiHealthUrl $apiHealthMarker) -and
        (Test-Endpoint $apiInfoUrl $apiIdentityMarker)
}

function Test-WebReady {
    return Test-Endpoint $webUrl $webMarker
}

function Wait-Service {
    param(
        [string]$Name,
        [scriptblock]$Probe,
        [string]$DisplayUri,
        [int]$TimeoutSeconds = 90
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    while ([DateTime]::UtcNow -lt $deadline) {
        if (& $Probe) {
            Write-Step "$Name is ready at $DisplayUri"
            return
        }
        Start-Sleep -Milliseconds 750
    }

    throw "$Name did not become ready within $TimeoutSeconds seconds. Inspect its terminal for the startup error."
}

function New-EmptyState {
    return [pscustomobject]@{
        Api = $null
        Web = $null
    }
}

function Read-LauncherState {
    if (-not (Test-Path -LiteralPath $statePath -PathType Leaf)) {
        return New-EmptyState
    }

    try {
        $state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
        if ($state.PSObject.Properties.Name -notcontains 'Api') {
            $state | Add-Member -NotePropertyName Api -NotePropertyValue $null
        }
        if ($state.PSObject.Properties.Name -notcontains 'Web') {
            $state | Add-Member -NotePropertyName Web -NotePropertyValue $null
        }
        return $state
    }
    catch {
        Write-Warning 'Ignoring unreadable .dev/processes.json state.'
        return New-EmptyState
    }
}

function Save-LauncherState {
    param([psobject]$State)

    if (-not (Test-Path -LiteralPath $stateDirectory -PathType Container)) {
        New-Item -ItemType Directory -Path $stateDirectory | Out-Null
    }
    $State | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $statePath -Encoding UTF8
}

function Get-OwnedProcess {
    param(
        [AllowNull()][psobject]$Entry,
        [string]$Role
    )

    if ($null -eq $Entry) {
        return $null
    }

    try {
        $process = Get-Process -Id ([int]$Entry.Pid) -ErrorAction Stop
        $startTicks = $process.StartTime.ToUniversalTime().Ticks
        if ($startTicks -ne [long]$Entry.StartedAtUtcTicks) {
            return $null
        }

        $processDetails = Get-CimInstance Win32_Process -Filter "ProcessId = $($process.Id)" -ErrorAction Stop
        $expectedRole = "__$Role"
        if ([string]::IsNullOrWhiteSpace($processDetails.CommandLine) -or
                $processDetails.CommandLine.IndexOf(
                    $PSCommandPath,
                    [StringComparison]::OrdinalIgnoreCase
                ) -lt 0 -or
                $processDetails.CommandLine.IndexOf(
                    $expectedRole,
                    [StringComparison]::OrdinalIgnoreCase
                ) -lt 0) {
            return $null
        }

        return $process
    }
    catch {
        return $null
    }
}

function New-ProcessEntry {
    param([System.Diagnostics.Process]$Process)

    return [pscustomobject]@{
        Pid = $Process.Id
        StartedAtUtcTicks = $Process.StartTime.ToUniversalTime().Ticks
    }
}

function Get-PowerShellPath {
    $modernPowerShell = Get-Command 'pwsh' -ErrorAction SilentlyContinue
    if ($null -ne $modernPowerShell) {
        return $modernPowerShell.Source
    }

    return (Get-ExecutablePath 'powershell.exe')
}

function Start-AtlasTerminal {
    param([ValidateSet('api', 'web')][string]$Role)

    $powerShellPath = Get-PowerShellPath
    $quotedScriptPath = '"' + $PSCommandPath.Replace('"', '""') + '"'
    $arguments = "-NoExit -NoProfile -ExecutionPolicy Bypass -File $quotedScriptPath __$Role"
    $process = Start-Process `
        -FilePath $powerShellPath `
        -ArgumentList $arguments `
        -WorkingDirectory $repoRoot `
        -WindowStyle Normal `
        -PassThru

    Start-Sleep -Milliseconds 250
    if ($process.HasExited) {
        throw "The $Role terminal exited before startup could begin."
    }

    return New-ProcessEntry $process
}

function Assert-StartPrerequisites {
    Get-ExecutablePath 'java' | Out-Null
    Get-ExecutablePath 'pnpm' | Out-Null

    $mavenWrapper = Join-Path $repoRoot 'apps\api\mvnw.cmd'
    if (-not (Test-Path -LiteralPath $mavenWrapper -PathType Leaf)) {
        throw 'The API Maven wrapper is missing.'
    }

    $webModules = Join-Path $repoRoot 'apps\web\node_modules'
    if (-not (Test-Path -LiteralPath $webModules -PathType Container)) {
        throw 'Web dependencies are missing. Run pnpm install --frozen-lockfile once, then retry.'
    }
}

function Start-Applications {
    $state = Read-LauncherState
    $ownedApi = Get-OwnedProcess $state.Api 'api'
    $ownedWeb = Get-OwnedProcess $state.Web 'web'

    if ($null -eq $ownedApi) {
        $state.Api = $null
    }
    if ($null -eq $ownedWeb) {
        $state.Web = $null
    }

    if (Test-ApiReady) {
        Write-Step 'API is already healthy; leaving the existing process untouched.'
    }
    elseif ($null -ne $ownedApi) {
        Write-Step 'Waiting for the launcher-owned API process that is already running...'
    }
    elseif (Test-TcpPort 8080) {
        throw 'Port 8080 is occupied by an unknown process that is not a healthy Atlas API.'
    }
    else {
        Write-Step 'Opening the Spring Boot API terminal...'
        $state.Api = Start-AtlasTerminal 'api'
        Save-LauncherState $state
    }

    if (Test-WebReady) {
        Write-Step 'Web app is already healthy; leaving the existing process untouched.'
    }
    elseif ($null -ne $ownedWeb) {
        Write-Step 'Waiting for the launcher-owned web process that is already running...'
    }
    elseif (Test-TcpPort 5173) {
        throw 'Port 5173 is occupied by an unknown process that is not the Atlas web app.'
    }
    else {
        Write-Step 'Opening the Vite web terminal...'
        $state.Web = Start-AtlasTerminal 'web'
        Save-LauncherState $state
    }

    Save-LauncherState $state
    Wait-Service 'API' { Test-ApiReady } $apiHealthUrl
    Wait-Service 'Web app' { Test-WebReady } $webUrl
}

function Show-Status {
    $dockerCommand = Get-Command 'docker' -ErrorAction SilentlyContinue
    if ($null -eq $dockerCommand) {
        Write-Host '[atlas] Docker CLI: missing' -ForegroundColor Red
    }
    elseif (Test-DockerEngine $dockerCommand.Source) {
        Write-Host '[atlas] Docker Engine: ready' -ForegroundColor Green
        & $dockerCommand.Source compose --project-directory $repoRoot ps
    }
    else {
        Write-Host '[atlas] Docker Engine: unavailable (start Docker Desktop manually)' -ForegroundColor Yellow
    }

    $state = Read-LauncherState
    $ownedApi = Get-OwnedProcess $state.Api 'api'
    $ownedWeb = Get-OwnedProcess $state.Web 'web'
    $apiState = if (Test-ApiReady) { 'healthy' } elseif ($null -ne $ownedApi) { 'process running, endpoint unavailable' } else { 'stopped' }
    $webState = if (Test-WebReady) { 'healthy' } elseif ($null -ne $ownedWeb) { 'process running, endpoint unavailable' } else { 'stopped' }

    Write-Host "[atlas] API: $apiState"
    Write-Host "[atlas] Web: $webState"
}

function Stop-AtlasProcess {
    param(
        [AllowNull()][psobject]$Entry,
        [string]$Role
    )

    $process = Get-OwnedProcess $Entry $Role
    if ($null -eq $process) {
        Write-Step "No launcher-owned $Role process is running."
        return
    }

    Write-Step "Stopping launcher-owned $Role process tree (PID $($process.Id))..."
    & taskkill.exe /PID $process.Id /T /F | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to stop the launcher-owned $Role process tree."
    }
}

function Stop-LocalStack {
    $state = Read-LauncherState
    Stop-AtlasProcess $state.Api 'api'
    Stop-AtlasProcess $state.Web 'web'

    if (Test-Path -LiteralPath $statePath -PathType Leaf) {
        Remove-Item -LiteralPath $statePath -Force
    }

    $dockerCommand = Get-Command 'docker' -ErrorAction SilentlyContinue
    if ($null -ne $dockerCommand -and (Test-DockerEngine $dockerCommand.Source)) {
        Write-Step 'Stopping PostgreSQL without deleting its volume...'
        & $dockerCommand.Source compose --project-directory $repoRoot stop postgres
        if ($LASTEXITCODE -ne 0) {
            throw "PostgreSQL stop failed (docker compose exit code $LASTEXITCODE)."
        }
    }
    else {
        Write-Step 'Docker Engine is unavailable; PostgreSQL was not changed.'
    }
}

function Run-ApiHost {
    $Host.UI.RawUI.WindowTitle = 'Project Atlas - API'
    Set-Location (Join-Path $repoRoot 'apps\api')
    Write-Step 'Starting Spring Boot API on http://127.0.0.1:8080'
    & .\mvnw.cmd spring-boot:run
    Write-Host "`n[atlas] API process exited with code $LASTEXITCODE." -ForegroundColor Yellow
}

function Run-WebHost {
    $Host.UI.RawUI.WindowTitle = 'Project Atlas - Web'
    Set-Location $repoRoot
    Write-Step 'Starting Vite web app on http://127.0.0.1:5173'
    & pnpm --dir apps/web dev --host 127.0.0.1 --port 5173
    Write-Host "`n[atlas] Web process exited with code $LASTEXITCODE." -ForegroundColor Yellow
}

switch ($Command) {
    '__api' {
        Run-ApiHost
        break
    }
    '__web' {
        Run-WebHost
        break
    }
    'infra' {
        Invoke-WithLauncherLock {
            $dockerPath = Require-DockerEngine
            Start-Postgres $dockerPath
            Write-Step 'PostgreSQL is ready. API and web were not started.'
        }
        break
    }
    'status' {
        Show-Status
        break
    }
    'stop' {
        Invoke-WithLauncherLock {
            Stop-LocalStack
        }
        break
    }
    'start' {
        Invoke-WithLauncherLock {
            Assert-StartPrerequisites
            $dockerPath = Require-DockerEngine
            $environmentSnapshot = Get-AtlasEnvironmentSnapshot
            try {
                Import-AtlasEnvironment $dockerPath
                Start-Postgres $dockerPath
                Start-Applications
                if (-not $NoBrowser) {
                    Write-Step "Opening $webUrl"
                    Start-Process -FilePath $webUrl | Out-Null
                }
                Write-Step 'Atlas is ready. Close this launcher window; API and web keep running in their own terminals.'
            }
            finally {
                Restore-AtlasEnvironment $environmentSnapshot
            }
        }
        break
    }
}
