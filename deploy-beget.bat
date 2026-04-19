@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion

echo ===== DEPLOY Spring App (Beget / VPS) =====
echo.

set /p SERVER_IP="Server IP or host: "
set /p SSH_USER="SSH user (e.g. root): "
set /p REMOTE_PATH="Full path on server: "
set /p DOMAIN="Main domain (e.g. example.com): "
set /p EMAIL="Email for Let's Encrypt (default: admin@%DOMAIN%): "
if "%EMAIL%"=="" (
    set "EMAIL=admin@%DOMAIN%"
)

echo.
if not exist "build\libs\*.jar" (
    echo Build the project first: gradlew.bat build
    echo Expected JAR in build\libs\
    exit /b 1
)

echo Creating folders on server...
ssh %SSH_USER%@%SERVER_IP% "mkdir -p %REMOTE_PATH%/nginx/ssl %REMOTE_PATH%/dumps"
if errorlevel 1 (
    echo SSH or mkdir failed.
    exit /b 1
)

echo.
echo Copying files...
scp Dockerfile.deploy docker-compose.deploy.yml %SSH_USER%@%SERVER_IP%:%REMOTE_PATH%/
if errorlevel 1 (
    echo Failed to copy Docker files.
    exit /b 1
)
scp nginx/nginx.conf %SSH_USER%@%SERVER_IP%:%REMOTE_PATH%/nginx/
if errorlevel 1 (
    echo Failed to copy nginx.conf.
    exit /b 1
)
scp init-ssl.sh backup-db.sh %SSH_USER%@%SERVER_IP%:%REMOTE_PATH%/
if errorlevel 1 (
    echo Failed to copy scripts.
    exit /b 1
)

for %%F in (build\libs\*.jar) do (
    echo %%~nF | findstr /i "plain" >nul
    if errorlevel 1 (
        scp "%%F" %SSH_USER%@%SERVER_IP%:%REMOTE_PATH%/
        if errorlevel 1 (
            echo Failed to copy JAR.
            exit /b 1
        )
        goto :jar_done
    )
)
echo No runnable JAR found. Build with gradlew.bat build. Skip *-plain.jar.
exit /b 1
:jar_done

if exist "init-db.sql" (
    scp init-db.sql %SSH_USER%@%SERVER_IP%:%REMOTE_PATH%/
)

echo.
echo Running init-ssl.sh and starting containers...
ssh %SSH_USER%@%SERVER_IP% "cd %REMOTE_PATH% && sed -i 's/\r$//' init-ssl.sh backup-db.sh && chmod +x init-ssl.sh backup-db.sh && DOMAIN=%DOMAIN% EMAIL=%EMAIL% ./init-ssl.sh"
if errorlevel 1 (
    echo init-ssl.sh or docker compose failed.
    exit /b 1
)

echo.
echo Done. Site: https://%DOMAIN%
echo DB dumps: server folder dumps. Cron: 0 3 * * * %REMOTE_PATH%/backup-db.sh
exit /b 0
