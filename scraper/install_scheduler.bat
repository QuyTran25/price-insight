@echo off
REM ============================================================
REM PRICE TRACKER - CÀI ĐẶT TASK SCHEDULER TỰ ĐỘNG
REM Tự động import task vào Windows Task Scheduler
REM ============================================================

echo.
echo ============================================================
echo     CAI DAT TASK SCHEDULER TU DONG
echo ============================================================
echo.

REM Kiểm tra quyền Admin
net session >nul 2>&1
if %errorLevel% NEQ 0 (
    echo [ERROR] Yeu cau quyen Administrator!
    echo.
    echo Vui long:
    echo   1. Click chuot phai vao file nay
    echo   2. Chon "Run as administrator"
    echo.
    pause
    exit /b 1
)

echo [OK] Da co quyen Administrator
echo.

REM Lấy đường dẫn hiện tại
set "CURRENT_DIR=%~dp0"
set "CURRENT_DIR=%CURRENT_DIR:~0,-1%"

echo Duong dan scraper: %CURRENT_DIR%
echo.

REM Tạo file XML tạm với đường dẫn thật
set "TEMP_XML=%TEMP%\price_tracker_task.xml"

echo Dang tao file cau hinh Task Scheduler...

REM Đọc template và thay thế đường dẫn
powershell -Command "(Get-Content '%CURRENT_DIR%\task_scheduler.xml') -replace 'FULL_PATH_TO_SCRAPER', '%CURRENT_DIR%' | Set-Content '%TEMP_XML%'"

if not exist "%TEMP_XML%" (
    echo [ERROR] Khong the tao file cau hinh!
    pause
    exit /b 1
)

echo [OK] Da tao file cau hinh
echo.

REM Import task vào Task Scheduler
echo Dang import task vao Windows Task Scheduler...

schtasks /Create /TN "PriceTracker_AutoScraper" /XML "%TEMP_XML%" /F

if %errorLevel% EQU 0 (
    echo.
    echo ============================================================
    echo  CAI DAT THANH CONG!
    echo ============================================================
    echo.
    echo Task "PriceTracker_AutoScraper" da duoc tao
    echo.
    echo Lich chay:
    echo   - 08:00 sang
    echo   - 16:00 chieu  
    echo   - 00:00 dem
    echo.
    echo Moi ngay: 3 lan
    echo.
    echo Ban co the:
    echo   1. Mo Task Scheduler: Win + R ^> taskschd.msc
    echo   2. Tim task: PriceTracker_AutoScraper
    echo   3. Chinh sua hoac chay thu ngay
    echo.
    echo ============================================================
) else (
    echo.
    echo [ERROR] Cai dat that bai!
    echo Vui long kiem tra log va thu lai
    echo.
)

REM Xóa file tạm
del "%TEMP_XML%" 2>nul

echo.
pause
