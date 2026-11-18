@echo off
setlocal

echo === WS Proxy OLED: release build ===
call gradlew.bat :app:assembleRelease
if errorlevel 1 (
    echo BUILD FAILED
    exit /b 1
)

if not exist app\release mkdir app\release
copy /Y app\build\outputs\apk\release\app-release.apk app\release\ws-proxy-oled.apk >nul

echo.
echo APK: app\release\ws-proxy-oled.apk
