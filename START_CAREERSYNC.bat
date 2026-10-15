@echo off
setlocal
cd /d "%~dp0"
title CareerSync AI - Start Correct Backend

if not exist "pom.xml" (
  echo ERROR: pom.xml not found. Run this file from the extracted CareerSync_AI_FIXED project folder.
  pause
  exit /b 1
)
if not exist "mvnw.cmd" (
  echo ERROR: mvnw.cmd not found in this project folder.
  pause
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-careersync.ps1"
set "RUN_EXIT=%ERRORLEVEL%"
if not "%RUN_EXIT%"=="0" echo CareerSync AI stopped or failed. See the message above.
pause
exit /b %RUN_EXIT%
