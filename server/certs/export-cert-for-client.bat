@echo off
REM ============================================
REM Export Server Certificate for Client Trust
REM ============================================

echo.
echo ============================================
echo   EXPORTING SERVER CERTIFICATE
echo ============================================
echo.

cd /d "%~dp0"
cd ..\..

set SERVER_KEYSTORE=server\certs\server.keystore
set SERVER_PASS=pricetracker123
set SERVER_ALIAS=pricetracker
set CERT_FILE=server\certs\server.crt

set CLIENT_TRUSTSTORE=client\certs\truststore.jks
set TRUSTSTORE_PASS=pricetracker123

echo [1/4] Checking keytool...
where keytool >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: keytool not found! Please install JDK and add to PATH
    pause
    exit /b 1
)

echo [2/4] Creating client certs directory...
if not exist "client\certs" (
    mkdir "client\certs"
    echo    Created client\certs\
)

echo [3/4] Exporting server certificate...
keytool -exportcert ^
    -alias %SERVER_ALIAS% ^
    -keystore %SERVER_KEYSTORE% ^
    -storepass %SERVER_PASS% ^
    -file %CERT_FILE% ^
    -rfc

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to export certificate!
    pause
    exit /b 1
)

echo    Certificate exported to %CERT_FILE%

echo [4/4] Importing certificate to client truststore...

REM Delete old truststore if exists
if exist "%CLIENT_TRUSTSTORE%" (
    del "%CLIENT_TRUSTSTORE%"
    echo    Old truststore deleted
)

keytool -importcert ^
    -alias %SERVER_ALIAS% ^
    -file %CERT_FILE% ^
    -keystore %CLIENT_TRUSTSTORE% ^
    -storepass %TRUSTSTORE_PASS% ^
    -noprompt

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================
    echo   SUCCESS!
    echo ============================================
    echo   Server certificate: %CERT_FILE%
    echo   Client truststore: %CLIENT_TRUSTSTORE%
    echo   Truststore password: %TRUSTSTORE_PASS%
    echo ============================================
    echo.
    
    REM List truststore contents
    echo Truststore contents:
    keytool -list -keystore %CLIENT_TRUSTSTORE% -storepass %TRUSTSTORE_PASS%
    
) else (
    echo.
    echo ERROR: Failed to import certificate to truststore!
    pause
    exit /b 1
)

echo.
echo NEXT STEPS:
echo 1. Copy truststore.jks to client application
echo 2. Client will automatically verify server certificate
echo 3. For dev, use -Dssl.trustAll=true to skip verification
echo.
pause
