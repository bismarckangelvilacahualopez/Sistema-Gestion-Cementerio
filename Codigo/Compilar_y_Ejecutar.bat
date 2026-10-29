@echo off
cd /d "%~dp0"
where javac >nul 2>&1
if errorlevel 1 (
 echo Instala un JDK de Java 8 o superior para compilar.
 pause
 exit /b 1
)
if not exist build mkdir build
javac -encoding UTF-8 -d build src\cementerio\Main.java
if errorlevel 1 (
 echo Error de compilacion.
 pause
 exit /b 1
)
java -cp build cementerio.Main
if errorlevel 1 pause
