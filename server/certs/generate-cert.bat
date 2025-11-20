@echo off
REM ============================================
REM Generate Self-Signed SSL Certificate
REM for Price Tracker Server Development
REM ============================================

echo.
echo ============================================
echo   GENERATING SSL CERTIFICATE
echo ============================================
echo.

set KEYSTORE_FILE=server.keystore
set KEYSTORE_PASS=pricetracker123
set KEY_ALIAS=pricetracker
set VALIDITY_DAYS=365
set DNAME="CN=localhost, OU=PriceTracker, O=PriceTracker, L=HCM, S=HCM, C=VN"

echo [1/3] Checking Java keytool...
where keytool >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: keytool not found! Please install JDK and add to PATH
    pause
    exit /b 1
)

echo [2/3] Deleting old keystore if exists...
if exist %KEYSTORE_FILE% (
    del %KEYSTORE_FILE%
    echo    Old keystore deleted
)

echo [3/3] Generating new keystore and certificate...
keytool -genkeypair ^
    -alias %KEY_ALIAS% ^
    -keyalg RSA ^
    -keysize 2048 ^
    -validity %VALIDITY_DAYS% ^
    -keystore %KEYSTORE_FILE% ^
    -storepass %KEYSTORE_PASS% ^
    -keypass %KEYSTORE_PASS% ^
    -dname %DNAME% ^
    -ext SAN=dns:localhost,ip:127.0.0.1

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================
    echo   SUCCESS!
    echo ============================================
    echo   Keystore file: %KEYSTORE_FILE%
    echo   Password: %KEYSTORE_PASS%
    echo   Alias: %KEY_ALIAS%
    echo   Valid for: %VALIDITY_DAYS% days
    echo ============================================
    echo.
    
    REM List certificate info
    echo Certificate details:
    keytool -list -v -keystore %KEYSTORE_FILE% -storepass %KEYSTORE_PASS% -alias %KEY_ALIAS% | findstr /C:"Owner:" /C:"Valid from"
    
) else (
    echo.
    echo ERROR: Failed to generate certificate!
    pause
    exit /b 1
)

echo.
echo NEXT STEPS:
echo 1. Keep this keystore file secure
echo 2. For production, use a certificate from a trusted CA
echo 3. Update config if you changed password/path
echo.
pause
