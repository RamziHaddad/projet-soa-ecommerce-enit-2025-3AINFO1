@echo off
REM Quick development setup script for Windows

echo Starting Feedback Service Development Setup...
echo.

REM Check if uv is installed
where uv >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [OK] uv is installed
    set USE_UV=true
) else (
    echo [WARNING] uv not found, using pip instead
    echo    Install uv for faster dependency management: https://astral.sh/uv
    set USE_UV=false
)

REM Create virtual environment
if "%USE_UV%"=="true" (
    echo Creating virtual environment with uv...
    uv venv
    call .venv\Scripts\activate.bat
    echo Installing dependencies with uv...
    uv pip install -r requirements.txt
) else (
    echo Creating virtual environment with venv...
    python -m venv venv
    call venv\Scripts\activate.bat
    echo Installing dependencies with pip...
    pip install -r requirements.txt
)

REM Create .env if it doesn't exist
if not exist .env (
    echo Creating .env file from template...
    copy .env.example .env
    echo [WARNING] Please edit .env with your configuration
)

REM Start Docker services
echo Starting PostgreSQL and Redis...
docker-compose up -d db redis

REM Wait for services
echo Waiting for services to be ready...
timeout /t 5 /nobreak >nul

REM Run migrations
echo Running database migrations...
alembic upgrade head

echo.
echo [SUCCESS] Setup complete!
echo.
echo Next steps:
echo   1. Edit .env with your configuration (if needed)
echo   2. Start the app: uvicorn app.main:app --reload
echo   3. Open http://localhost:8000/docs
echo.
echo Generate test tokens: python scripts\generate_token.py
echo.
pause
