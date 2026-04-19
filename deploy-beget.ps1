# Deploy Java Planet (Beget/VPS). Run in PowerShell: .\deploy-beget.ps1
# Or run the batch via cmd: cmd /c deploy-beget.bat

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "===== DEPLOY Spring App (Beget / VPS) =====" -ForegroundColor Cyan

$SERVER_IP   = Read-Host "Enter server IP or host"
$SSH_USER    = Read-Host "Enter SSH user (e.g. root)"
$REMOTE_PATH = Read-Host "Enter full path on server (folder will be created)"
$DOMAIN      = Read-Host "Enter main domain (e.g. example.com)"
$EMAIL       = Read-Host "Enter email for Let's Encrypt (default: admin@$DOMAIN)"
if ([string]::IsNullOrWhiteSpace($EMAIL)) {
    $EMAIL = "admin@$DOMAIN"
}

if (-not (Test-Path "build\libs\*.jar")) {
    Write-Host "Build the project first: .\gradlew.bat build" -ForegroundColor Red
    Write-Host "Expected JAR in build\libs\" -ForegroundColor Red
    exit 1
}

Write-Host "`nCreating folders on server..."
ssh "${SSH_USER}@${SERVER_IP}" "mkdir -p ${REMOTE_PATH}/nginx/ssl ${REMOTE_PATH}/dumps"
if ($LASTEXITCODE -ne 0) { Write-Host "SSH or mkdir failed." -ForegroundColor Red; exit 1 }

Write-Host "`nCopying files..."
scp Dockerfile.deploy docker-compose.deploy.yml "${SSH_USER}@${SERVER_IP}:${REMOTE_PATH}/"
if ($LASTEXITCODE -ne 0) { exit 1 }
scp nginx/nginx.conf "${SSH_USER}@${SERVER_IP}:${REMOTE_PATH}/nginx/"
if ($LASTEXITCODE -ne 0) { exit 1 }
scp init-ssl.sh backup-db.sh "${SSH_USER}@${SERVER_IP}:${REMOTE_PATH}/"
if ($LASTEXITCODE -ne 0) { exit 1 }

$jar = Get-ChildItem "build\libs\*.jar" | Where-Object { $_.Name -notmatch "-plain\.jar$" } | Select-Object -First 1
if ($jar) {
    scp $jar.FullName "${SSH_USER}@${SERVER_IP}:${REMOTE_PATH}/"
    if ($LASTEXITCODE -ne 0) { exit 1 }
}
if (Test-Path "init-db.sql") {
    scp init-db.sql "${SSH_USER}@${SERVER_IP}:${REMOTE_PATH}/"
}

Write-Host "`nRunning SSL init and containers on server..."
ssh "${SSH_USER}@${SERVER_IP}" "cd ${REMOTE_PATH} && chmod +x init-ssl.sh backup-db.sh && DOMAIN=${DOMAIN} EMAIL=${EMAIL} ./init-ssl.sh"
if ($LASTEXITCODE -ne 0) { Write-Host "init-ssl.sh or docker compose failed." -ForegroundColor Red; exit 1 }

Write-Host "`nDone. Site: https://$DOMAIN" -ForegroundColor Green
Write-Host "DB dumps: server folder dumps. For daily backup add to cron: 0 3 * * * ${REMOTE_PATH}/backup-db.sh"
