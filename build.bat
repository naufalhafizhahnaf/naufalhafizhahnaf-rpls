@echo off
setlocal
cd /d "%~dp0"

set "JDBC=lib\mysql-connector-j-8.3.0.jar"
if not exist "%JDBC%" (
    echo [GAGAL] Driver JDBC tidak ditemukan: %JDBC%
    echo Salin mysql-connector-j-8.3.0.jar ke folder lib.
    pause
    exit /b 1
)

if not exist build\classes mkdir build\classes
if exist sources.txt del sources.txt
dir /s /b src\*.java > sources.txt

echo Mengompilasi aplikasi...
javac --release 11 -encoding UTF-8 -cp "%JDBC%" -d build\classes @sources.txt
if errorlevel 1 (
    echo.
    echo Kompilasi gagal.
    pause
    exit /b 1
)

echo Menjalankan GUI I-HSSE...
java -cp "build\classes;%JDBC%" ihsse.gui.GuiMain
endlocal
