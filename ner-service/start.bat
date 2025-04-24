@echo off
echo =======================================================================
echo Starting PII detection service with local model
echo =======================================================================
echo.

REM Check if Python is available
where python >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Python not found! Please install Python and try again.
    pause
    exit /b 1
)

REM Set environment variables for certificate processing
set HF_HUB_DISABLE_SSL_VERIFICATION=1
set PYTHONHTTPSVERIFY=0

REM Create the models folder if it doesn't exist
if not exist "models" mkdir models

REM Check if the model already exists
if not exist "models\E3-JSI_gliner-multi-pii-domains-v1\gliner_config.json" (
    echo The model doesn't exist locally. Preloading...
    python preload_model.py
    if %ERRORLEVEL% neq 0 (
        echo Model preloading failed. Using direct download.
        set USE_DIRECT_MODEL=1
    )
) else (
    echo Model found in local folder.
    set USE_DIRECT_MODEL=0
)

echo.
echo The service will be available at: http://localhost:5000
echo.
echo Press Ctrl+C to stop the service.
echo.

REM Start the service with the local model path - switching to single worker mode
if "%USE_DIRECT_MODEL%"=="1" (
    echo Launching service with direct download from Hugging Face...
    python -m main --model-id "E3-JSI/gliner-multi-pii-domains-v1" --use-windows-certs --threads 8 --workers 1
) else (
    echo Launching service with local model...
    python -m main --model-path "models\E3-JSI_gliner-multi-pii-domains-v1" --use-windows-certs --threads 8 --workers 1
)

REM If we get here, the service has stopped
echo.
echo Service stopped.
pause