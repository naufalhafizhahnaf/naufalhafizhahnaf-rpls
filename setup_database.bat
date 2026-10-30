@echo off
setlocal
cd /d "%~dp0"

set "MYSQL=C:\xampp\mysql\bin\mysql.exe"
if not exist "%MYSQL%" (
    where mysql >nul 2>nul
    if errorlevel 1 (
        echo [GAGAL] mysql.exe tidak ditemukan.
        echo Pastikan XAMPP terpasang di C:\xampp atau MySQL ada di PATH.
        pause
        exit /b 1
    )
    set "MYSQL=mysql"
)

echo ============================================================
echo  Membuat database ihsse_db dan mengisi data awal...
echo ============================================================
"%MYSQL%" -u root < "database\ihsse_db.sql"
if errorlevel 1 (
    echo.
    echo [GAGAL] Import database gagal.
    echo Pastikan MySQL XAMPP sudah berjalan dan password root kosong.
    pause
    exit /b 1
)

echo.
echo [OK] Database ihsse_db siap dipakai.
echo      Login GUI: admin / admin123
echo ============================================================
pause
endlocal
