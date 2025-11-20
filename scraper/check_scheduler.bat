@echo off
chcp 65001 >nul
echo.
echo ============================================================
echo     KIEM TRA TASK SCHEDULER
echo ============================================================
echo.

echo Dang kiem tra task "PriceTracker_AutoScraper"...
echo.

schtasks /Query /TN "PriceTracker_AutoScraper" /V /FO LIST

if %errorLevel% EQU 0 (
    echo.
    echo ============================================================
    echo  TASK DA TON TAI!
    echo ============================================================
    echo.
    echo Ban co the:
    echo   1. Mo Task Scheduler: Win + R ^> taskschd.msc
    echo   2. Tim task: PriceTracker_AutoScraper
    echo   3. Xem lich chay trong tab "Triggers"
    echo.
) else (
    echo.
    echo ============================================================
    echo  CHUA CO TASK!
    echo ============================================================
    echo  Vui long chay install_scheduler.bat voi quyen Administrator
    echo.
)

pause
