@echo off
cd /d "%~dp0"
java -jar Cementerio.jar
if errorlevel 1 (
 echo.
 echo No se pudo abrir el sistema. Verifica Java con: java -version
 pause
)
