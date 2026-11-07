@echo off
echo ===================================================
echo Running EngBuddy Complete Test Suite
echo ===================================================

echo.
echo [1/2] Running Native Android Robolectric and Math Tests...
call .\gradlew.bat :app:testDebugUnitTest --no-daemon "-Dorg.gradle.parallel=false"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Android unit tests failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/2] Running FastAPI Backend Pytest Suite...
set PYTHONPATH=backend
.\.venv\Scripts\python.exe -m pytest backend/tests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Backend pytest suite failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ===================================================
echo [SUCCESS] All Android and Backend tests PASSED!
echo ===================================================
if "%CI%"=="" (
    rem In interactive shells, wait 2 seconds before closing; do not hang if redirected
    timeout /t 2 /nobreak >nul 2>&1
)
exit /b 0
