@echo off
REM ============================================================
REM PRICE TRACKER - AUTO SCRAPER
REM Tự động kiểm tra MySQL, chạy scraper, và gửi email
REM ============================================================

setlocal EnableDelayedExpansion

REM Màu sắc cho output
color 0A

echo.
echo ============================================================
echo     PRICE TRACKER - AUTO SCRAPER
echo     Thoi gian: %date% %time%
echo ============================================================
echo.

REM Chuyển đến thư mục scraper
cd /d "%~dp0"

REM ============================================================
REM BUOC 1: KIEM TRA MYSQL
REM ============================================================
echo [1/5] Kiem tra MySQL dang chay...

tasklist /FI "IMAGENAME eq mysqld.exe" 2>NUL | find /I /N "mysqld.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo [OK] MySQL dang chay
) else (
    echo [!] MySQL chua chay, dang khoi dong XAMPP...
    
    REM Kiểm tra XAMPP có tồn tại không
    if exist "C:\xampp\xampp-control.exe" (
        start "" "C:\xampp\xampp-control.exe"
        timeout /t 5 /nobreak >nul
        
        REM Thử bật MySQL service
        net start MySQL 2>NUL
        if !ERRORLEVEL! EQU 0 (
            echo [OK] Da khoi dong MySQL thanh cong
            timeout /t 10 /nobreak >nul
        ) else (
            echo [WARNING] Khong the tu dong bat MySQL
            echo Vui long bat MySQL thu cong trong XAMPP Control Panel
            timeout /t 15 /nobreak >nul
        )
    ) else (
        echo [ERROR] Khong tim thay XAMPP tai C:\xampp\
        echo Vui long kiem tra duong dan XAMPP trong config.ini
        goto :error
    )
)

REM ============================================================
REM BUOC 2: KIEM TRA KET NOI DATABASE
REM ============================================================
echo.
echo [2/5] Kiem tra ket noi database...

python check_db.py
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Khong ket noi duoc database!
    echo Vui long kiem tra:
    echo   - MySQL da chay chua?
    echo   - Database 'price_insight' da tao chua?
    echo   - Thong tin trong config.ini dung chua?
    goto :error
)

REM ============================================================
REM BUOC 3: CHAY SCRAPER
REM ============================================================
echo.
echo [3/5] Bat dau cao du lieu tu Tiki...
echo ------------------------------------------------------------

python scraper.py

set SCRAPER_EXIT_CODE=%ERRORLEVEL%

REM ============================================================
REM BUOC 4: GUI EMAIL THONG BAO
REM ============================================================
echo.
echo [4/5] Gui email thong bao...

if %SCRAPER_EXIT_CODE% EQU 0 (
    echo [OK] Scraper chay thanh cong!
    python send_email.py success
) else (
    echo [ERROR] Scraper gap loi! (Exit code: %SCRAPER_EXIT_CODE%)
    python send_email.py failed "Scraper exit code: %SCRAPER_EXIT_CODE%"
)

REM ============================================================
REM BUOC 5: TOM TAT KET QUA
REM ============================================================
echo.
echo [5/5] Hoan thanh!
echo ============================================================

if %SCRAPER_EXIT_CODE% EQU 0 (
    echo  TRANG THAI: THANH CONG
    echo  Du lieu da duoc cap nhat vao database
    echo  Email thong bao da duoc gui
) else (
    echo  TRANG THAI: THAT BAI
    echo  Vui long kiem tra log file de biet chi tiet
)

echo ============================================================
echo.

REM Tự động đóng sau 10 giây khi chạy từ Task Scheduler
timeout /t 10

exit /b 0

REM ============================================================
REM XU LY LOI
REM ============================================================
:error
echo.
echo ============================================================
echo  CO LOI XAY RA!
echo ============================================================
echo  Vui long kiem tra va thu lai
echo ============================================================
echo.

REM Gửi email báo lỗi
python send_email.py failed "Script bi dung do loi nghiem trong"

timeout /t 15
exit /b 1
