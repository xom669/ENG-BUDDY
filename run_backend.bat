@echo off
echo ===================================================
echo Starting EngBuddy FastAPI Backend Server (Port 8000)
echo ===================================================
cd /d "%~dp0"
set PYTHONPATH=backend
.\.venv\Scripts\uvicorn.exe app.main:app --app-dir backend --host 127.0.0.1 --port 8000 --reload
pause
