$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$launcherPath = Join-Path $repoRoot 'dev.ps1'
$wrapperPath = Join-Path $repoRoot 'dev.cmd'
$statePath = Join-Path $repoRoot '.dev\processes.json'

$parseTokens = $null
$parseErrors = $null
[System.Management.Automation.Language.Parser]::ParseFile(
    $launcherPath,
    [ref]$parseTokens,
    [ref]$parseErrors
) | Out-Null
if ($parseErrors.Count -gt 0) {
    $messages = ($parseErrors | ForEach-Object Message) -join [Environment]::NewLine
    throw "dev.ps1 contains PowerShell parse errors:$([Environment]::NewLine)$messages"
}

$launcher = Get-Content -LiteralPath $launcherPath -Raw
$wrapper = Get-Content -LiteralPath $wrapperPath -Raw

$requiredPatterns = @(
    "\[string\]\`$Command = 'start'",
    "'start', 'infra', 'status', 'stop'",
    'Start-Postgres',
    'config --environment',
    'Get-AtlasEnvironmentSnapshot',
    'Restore-AtlasEnvironment',
    '\$apiInfoUrl',
    '"name":"project-atlas"',
    'System.Text.Encoding\]::UTF8.GetString',
    'Get-OwnedProcess',
    'StartedAtUtcTicks',
    'System.Threading.Mutex',
    'WaitOne\(\[TimeSpan\]::FromSeconds\(120\)\)',
    'taskkill\.exe /PID',
    'compose --project-directory \$repoRoot stop postgres'
)
foreach ($pattern in $requiredPatterns) {
    if ($launcher -notmatch $pattern) {
        throw "dev.ps1 is missing required launcher evidence: $pattern"
    }
}

$forbiddenPatterns = @(
    '(?im)docker[^\r\n]*\bdown\b',
    '(?im)compose[^\r\n]*\bdown\b',
    '(?im)volume\s+rm',
    '(?im)Remove-Item[^\r\n]*-Recurse',
    '(?im)taskkill\.exe[^\r\n]*/IM',
    '(?im)Stop-Process[^\r\n]*-Name',
    '(?im)Start-Process[^\r\n]*Docker\s+Desktop'
)
foreach ($pattern in $forbiddenPatterns) {
    if ($launcher -match $pattern) {
        throw "dev.ps1 contains forbidden destructive or broad process behavior: $pattern"
    }
}

if ($launcher -match '(?im)Get-Content[^\r\n]*\.env') {
    throw 'dev.ps1 must not parse .env independently of Docker Compose.'
}

if ($wrapper -notmatch 'dev\.ps1" start') {
    throw 'dev.cmd does not invoke dev.ps1 start.'
}

$stateBefore = if (Test-Path -LiteralPath $statePath -PathType Leaf) {
    Get-Content -LiteralPath $statePath -Raw
}
else {
    $null
}

$statusOutput = & $launcherPath status *>&1 | Out-String
if ($statusOutput -notmatch '\[atlas\] API:' -or
        $statusOutput -notmatch '\[atlas\] Web:') {
    throw 'dev.ps1 status did not report both application states.'
}

$stateAfter = if (Test-Path -LiteralPath $statePath -PathType Leaf) {
    Get-Content -LiteralPath $statePath -Raw
}
else {
    $null
}
if ($stateBefore -ne $stateAfter) {
    throw 'dev.ps1 status changed launcher process state.'
}

Write-Host $statusOutput.TrimEnd()
Write-Host 'Local development launcher verification passed.'
