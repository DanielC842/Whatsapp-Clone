@echo off
REM Compila y ejecuta Nexo en Windows. Uso: run.bat [demo | server | client IP PUERTO]
chcp 65001 >nul
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
  echo.
  echo [Nexo] No se encontro el JDK de Java ^(javac^).
  echo        Instala Java 17 o superior desde https://adoptium.net y vuelve a intentarlo.
  echo.
  pause
  exit /b 1
)
echo [Nexo] Compilando...
if exist out rmdir /s /q out
javac -encoding UTF-8 --release 17 -d out --source-path src\main\java src\main\java\com\nexo\Main.java
if errorlevel 1 (
  echo [Nexo] Error de compilacion.
  pause
  exit /b 1
)
echo [Nexo] Iniciando...
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -cp out com.nexo.Main %*
