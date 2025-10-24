@echo off

REM Get the directory where this batch file is located
set SCRIPT_DIR=%~dp0

REM Create temp batch file path and set as environment variable for Java to use
set PROJ_POSTRUN_BATCH=%TEMP%\proj_postrun_%RANDOM%_%RANDOM%.bat

REM Check which JAR location exists and run it
if exist "%SCRIPT_DIR%target\xyz-jphil-ai-proj_build_dev_misc_tools-1.0.jar" (
    java -jar "%SCRIPT_DIR%target\xyz-jphil-ai-proj_build_dev_misc_tools-1.0.jar" %*
) else if exist "%SCRIPT_DIR%xyz-jphil-ai-proj_build_dev_misc_tools-1.0.jar" (
    java -jar "%SCRIPT_DIR%xyz-jphil-ai-proj_build_dev_misc_tools-1.0.jar" %*
) else (
    echo ERROR: JAR file not found in either location:
    echo   - %SCRIPT_DIR%target\xyz-jphil-ai-proj_build_dev_misc_tools-1.0.jar
    echo   - %SCRIPT_DIR%xyz-jphil-ai-proj_build_dev_misc_tools-1.0.jar
    exit /b 1
)

REM If Java created the temp batch file, execute it (it will delete itself)
if exist "%PROJ_POSTRUN_BATCH%" (
    REM Suppress "batch file cannot be found" error when batch deletes itself
    call "%PROJ_POSTRUN_BATCH%" 2>nul
    REM Check if self-deletion was successful
    if not exist "%PROJ_POSTRUN_BATCH%" (
        echo [Cleanup successful]
    ) else (
        REM Fallback: delete temp batch if it still exists after self-delete attempt
        del "%PROJ_POSTRUN_BATCH%" 2>nul && echo [proj.bat: fallback cleanup successful] || echo [proj.bat: fallback cleanup failed - manual deletion needed: %PROJ_POSTRUN_BATCH%]
    )
)
