@echo off
REM ==========================================================================
REM  Nexo - compila y ejecuta la app en Windows.
REM  Uso: run.bat [demo | server | client IP PUERTO]
REM  Si el PC no tiene Java 17 o superior, descarga uno portable en la carpeta
REM  .jdk (solo la primera vez, necesita internet). No instala nada en el sistema.
REM ==========================================================================
setlocal EnableDelayedExpansion
chcp 65001 >nul
cd /d "%~dp0"

REM 1) Si ya hay un Java portable descargado antes, usarlo.
if exist ".jdk\bin\javac.exe" (
  set "PATH=%CD%\.jdk\bin;!PATH!"
  goto :compilar
)

REM 2) Si el sistema tiene javac 17 o superior, usarlo.
set "JAVAC_VER="
set "JAVAC_MAJOR=0"
where javac >nul 2>nul
if not errorlevel 1 (
  for /f "tokens=2" %%v in ('javac -version 2^>^&1 ^| findstr /b javac') do if not defined JAVAC_VER set "JAVAC_VER=%%v"
)
if defined JAVAC_VER (
  for /f "tokens=1 delims=." %%m in ("!JAVAC_VER!") do set /a "JAVAC_MAJOR=%%m" 2>nul
  if !JAVAC_MAJOR! GEQ 17 goto :compilar
  echo [Nexo] Se encontro Java !JAVAC_VER!, pero Nexo necesita Java 17 o superior.
)

REM 3) Descargar un JDK 17 portable (Eclipse Temurin).
echo [Nexo] Descargando Java 17 portable. Puede tardar unos minutos y solo pasa la primera vez...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue';" ^
  "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12;" ^
  "$arch = if ($env:PROCESSOR_ARCHITECTURE -eq 'ARM64') { 'aarch64' } else { 'x64' };" ^
  "$url = 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/' + $arch + '/jdk/hotspot/normal/eclipse';" ^
  "Invoke-WebRequest -Uri $url -OutFile 'jdk.zip' -UseBasicParsing;" ^
  "if (Test-Path '.jdk-tmp') { Remove-Item '.jdk-tmp' -Recurse -Force };" ^
  "Expand-Archive -Path 'jdk.zip' -DestinationPath '.jdk-tmp' -Force;" ^
  "$d = Get-ChildItem '.jdk-tmp' -Directory | Select-Object -First 1;" ^
  "Move-Item $d.FullName '.jdk'; Remove-Item '.jdk-tmp' -Recurse -Force; Remove-Item 'jdk.zip' -Force"
if not exist ".jdk\bin\javac.exe" (
  echo.
  echo [Nexo] No se pudo descargar Java automaticamente.
  echo        Revisa la conexion a internet, o instala Java 17+ desde https://adoptium.net
  echo.
  pause
  exit /b 1
)
set "PATH=%CD%\.jdk\bin;%PATH%"
echo [Nexo] Java descargado en la carpeta .jdk

:compilar
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
if errorlevel 1 pause
