@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0dev.ps1" start
if errorlevel 1 (
  echo.
  echo Project Atlas failed to start. Review the message above.
  pause
)
endlocal
