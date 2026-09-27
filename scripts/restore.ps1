param(
    [Parameter(Mandatory = $true)]
    [string]$BackupPath,

    [switch]$Force
)

$ErrorActionPreference = "Stop"

$resolvedBackup = (Resolve-Path $BackupPath).Path
$dbFile = Join-Path $resolvedBackup "database.dump"
$resourcesFile = Join-Path $resolvedBackup "learning-resources.tar.gz"
$manifestFile = Join-Path $resolvedBackup "manifest.json"

if (-not (Test-Path $dbFile)) {
    throw "database.dump não encontrado em '$resolvedBackup'."
}

if (-not (Test-Path $resourcesFile)) {
    throw "learning-resources.tar.gz não encontrado em '$resolvedBackup'."
}

if (Test-Path $manifestFile) {
    $manifest = Get-Content $manifestFile -Raw | ConvertFrom-Json

    $dbHash = (Get-FileHash -Algorithm SHA256 $dbFile).Hash
    $resourcesHash = (Get-FileHash -Algorithm SHA256 $resourcesFile).Hash

    if ($dbHash -ne $manifest.database.sha256) {
        throw "Checksum do banco inválido. Backup pode estar corrompido."
    }

    if ($resourcesHash -ne $manifest.learningResources.sha256) {
        throw "Checksum dos materiais inválido. Backup pode estar corrompido."
    }

    Write-Host "Checksums validados." -ForegroundColor Green
}

if (-not $Force) {
    Write-Host ""
    Write-Host "ATENÇÃO: o restore substituirá os dados atuais." -ForegroundColor Yellow
    $confirmation = Read-Host "Digite RESTORE para continuar"

    if ($confirmation -ne "RESTORE") {
        Write-Host "Restore cancelado."
        exit 0
    }
}

Write-Host "==> Garantindo PostgreSQL disponível..."
docker compose up -d postgres | Out-Host

Write-Host "==> Parando aplicação durante o restore..."
docker compose stop backend frontend 2>$null | Out-Host

$postgresContainer = (docker compose ps -q postgres).Trim()

if (-not $postgresContainer) {
    throw "Container PostgreSQL não encontrado."
}

Write-Host "==> Restaurando banco..."
docker cp $dbFile "${postgresContainer}:/tmp/techmind.dump" | Out-Host
docker compose exec -T postgres sh -c 'pg_restore --clean --if-exists --no-owner -U "$POSTGRES_USER" -d "$POSTGRES_DB" /tmp/techmind.dump'
docker compose exec -T postgres rm -f /tmp/techmind.dump

Write-Host "==> Preparando backend para restaurar materiais..."
docker compose create backend | Out-Host
$backendContainer = (docker compose ps -q backend).Trim()

if (-not $backendContainer) {
    throw "Container backend não encontrado."
}

docker cp $resourcesFile "${backendContainer}:/tmp/learning-resources.tar.gz" | Out-Host
docker compose start backend | Out-Host

Start-Sleep -Seconds 3

Write-Host "==> Restaurando materiais..."
docker compose exec -T backend sh -c 'rm -rf /app/storage/learning-resources/* && tar -xzf /tmp/learning-resources.tar.gz -C /app/storage && rm -f /tmp/learning-resources.tar.gz'

Write-Host "==> Reiniciando aplicação..."
docker compose restart backend | Out-Host
docker compose up -d frontend | Out-Host

Write-Host ""
Write-Host "RESTORE CONCLUIDO" -ForegroundColor Green
Write-Host "Valide:"
Write-Host "  docker compose ps"
Write-Host "  Invoke-RestMethod http://localhost:8080/actuator/health"
