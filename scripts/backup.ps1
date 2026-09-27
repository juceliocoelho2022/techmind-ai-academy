param(
    [string]$Destination = ".\\backups"
)

$ErrorActionPreference = "Stop"

function Assert-Command {
    param([string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Comando '$Name' não encontrado."
    }
}

Assert-Command "docker"

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path (Resolve-Path ".").Path $Destination
$backupDir = Join-Path $backupRoot "techmind-$timestamp"

New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

Write-Host "==> Validando containers..."
docker compose up -d postgres backend | Out-Host

$postgresContainer = (docker compose ps -q postgres).Trim()
$backendContainer = (docker compose ps -q backend).Trim()

if (-not $postgresContainer) {
    throw "Container PostgreSQL não encontrado."
}

if (-not $backendContainer) {
    throw "Container backend não encontrado."
}

Write-Host "==> Gerando dump PostgreSQL..."
docker compose exec -T postgres sh -c 'pg_dump -Fc -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f /tmp/techmind.dump'
docker cp "${postgresContainer}:/tmp/techmind.dump" (Join-Path $backupDir "database.dump") | Out-Host
docker compose exec -T postgres rm -f /tmp/techmind.dump

Write-Host "==> Compactando materiais das aulas..."
docker compose exec -T backend sh -c 'tar -czf /tmp/learning-resources.tar.gz -C /app/storage learning-resources'
docker cp "${backendContainer}:/tmp/learning-resources.tar.gz" (Join-Path $backupDir "learning-resources.tar.gz") | Out-Host
docker compose exec -T backend rm -f /tmp/learning-resources.tar.gz

$dbFile = Join-Path $backupDir "database.dump"
$resourcesFile = Join-Path $backupDir "learning-resources.tar.gz"

$dbHash = (Get-FileHash -Algorithm SHA256 $dbFile).Hash
$resourcesHash = (Get-FileHash -Algorithm SHA256 $resourcesFile).Hash

$manifest = [ordered]@{
    application = "TechMind AI Academy"
    createdAt = (Get-Date).ToString("o")
    formatVersion = 1
    database = [ordered]@{
        file = "database.dump"
        sha256 = $dbHash
        bytes = (Get-Item $dbFile).Length
    }
    learningResources = [ordered]@{
        file = "learning-resources.tar.gz"
        sha256 = $resourcesHash
        bytes = (Get-Item $resourcesFile).Length
    }
}

$manifest | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 (Join-Path $backupDir "manifest.json")

Write-Host ""
Write-Host "BACKUP CONCLUIDO" -ForegroundColor Green
Write-Host "Pasta: $backupDir"
Write-Host "Banco: $((Get-Item $dbFile).Length) bytes"
Write-Host "Materiais: $((Get-Item $resourcesFile).Length) bytes"
