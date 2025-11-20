@echo off
chcp 65001 >nul
echo ================================================
echo    📦 Export Database Schema for Railway
echo ================================================
echo.

cd /d "%~dp0scraper"

echo [1/2] Exporting database schema + data...
echo.

REM Export với data (cho demo)
mysql -u root -p price_insight > database_backup.sql

if %errorlevel% neq 0 (
    echo.
    echo ❌ Export FAILED!
    echo Make sure MySQL is running and password is correct.
    pause
    exit /b 1
)

echo.
echo ✅ Database backup created: scraper\database_backup.sql
echo.

echo [2/2] Exporting schema only (no data)...
mysql -u root -p --no-data price_insight > database_schema.sql

if %errorlevel% neq 0 (
    echo.
    echo ⚠️  Schema export FAILED, but backup is OK.
) else (
    echo ✅ Schema created: scraper\database_schema.sql
)

echo.
echo ================================================
echo    ✅ Export Complete!
echo ================================================
echo.
echo Files created:
echo   - database_backup.sql (with data) - Use for demo
echo   - database_schema.sql (structure only)
echo.
echo Next step: Import to Railway MySQL
echo   1. railway login
echo   2. railway connect mysql
echo   3. source database_backup.sql
echo.
pause
