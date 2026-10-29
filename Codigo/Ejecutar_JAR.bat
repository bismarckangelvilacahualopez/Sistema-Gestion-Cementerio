@echo off
cd /d "%~dp0"
where java >nul 2>&1
if errorlevel 1 (
  echo ERROR: Instala Java 17 o superior.
  pause
  exit /b 1
)
java -jar Cementerio.jar
if errorlevel 1 pause
