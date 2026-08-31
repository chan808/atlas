$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot

Push-Location $repoRoot
try {
    & .\scripts\verify-dev-launcher.ps1

    pnpm --dir apps/web verify
    if ($LASTEXITCODE -ne 0) {
        throw "Web verification failed with exit code $LASTEXITCODE"
    }

    Push-Location (Join-Path $repoRoot 'apps/api')
    try {
        & .\mvnw.cmd --batch-mode verify
        if ($LASTEXITCODE -ne 0) {
            throw "API verification failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}
finally {
    Pop-Location
}
