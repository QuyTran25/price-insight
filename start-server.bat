@echo off
chcp 65001 >nul
echo ================================================
echo    🚀 PRICE TRACKER - SERVER STARTER
echo    SSL/TLS + HikariCP Connection Pool
echo ================================================
echo.

cd /d "%~dp0server"

echo [1/4] 📦 Compiling Java files...
echo.

javac -encoding UTF-8 -d bin -cp "lib/*;../shared/src" ^
    src/com/pricetracker/server/http/SimpleHttpServer.java ^
    src/com/pricetracker/server/db/*.java ^
    src/com/pricetracker/server/utils/*.java ^
    src/com/pricetracker/server/crypto/*.java ^
    src/com/pricetracker/server/websocket/*.java ^
    src/com/pricetracker/server/core/*.java ^
    src/com/pricetracker/server/handler/*.java ^
    ../shared/src/com/pricetracker/models/*.java

if %errorlevel% neq 0 (
    echo.
    echo ❌ Compilation FAILED!
    echo Please check the error messages above.
    pause
    exit /b 1
)

echo.
echo ✅ Compilation successful!
echo.

echo [2/4] 🔐 Checking SSL certificates...
if not exist "certs\server.keystore" (
    echo ⚠️  SSL certificate not found!
    echo Running certificate generation...
    cd certs
    call generate-cert.bat
    call export-cert-for-client.bat
    cd ..
    echo ✅ SSL certificates generated!
) else (
    echo ✅ SSL certificates ready!
)
echo.

echo [3/4] 🔍 Checking MySQL connection...
echo Make sure XAMPP MySQL is running on port 3306
echo.

echo [4/4] 🚀 Starting SSL Server with HikariCP...
echo.
echo ================================================
echo    ✨ SERVER IS STARTING...
echo ================================================
echo.
echo � SSL/TLS: ENABLED (TLS 1.3 + 1.2)
echo ⚡ HikariCP: Connection Pool Active
echo 📡 Server Port: 8888 (Secure Socket)
echo 🌐 HTTP Endpoint: http://localhost:8080
echo.
echo Press Ctrl+C to stop the server
echo ================================================
echo.

REM Start PriceTrackerServer với SSL enabled
java -Dssl.enabled=true -cp "bin;lib/*;../shared/src" com.pricetracker.server.core.PriceTrackerServer

pause
