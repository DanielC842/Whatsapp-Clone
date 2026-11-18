@echo off
setlocal
cd /d "%~dp0"

if exist ".jdk\bin\javac.exe" set "PATH=%CD%\.jdk\bin;%PATH%"
where javac >nul 2>nul
if errorlevel 1 (
  echo [Nexo] No se encontro javac. Ejecuta run.bat para preparar Java 17 o instala un JDK 17+.
  exit /b 1
)

if not exist "target\test-classes" mkdir "target\test-classes"
 powershell -NoProfile -Command "Get-ChildItem src/main/java,src/test/java -Recurse -Filter *.java | ForEach-Object { $_.FullName.Replace('\','/') } | Set-Content -Encoding ASCII target/test-sources.txt"
if errorlevel 1 exit /b 1
javac -encoding UTF-8 --release 17 -d target\test-classes @target\test-sources.txt
if errorlevel 1 exit /b 1

java -Dfile.encoding=UTF-8 -cp target\test-classes com.nexo.ServerSmokeTest
if errorlevel 1 exit /b 1
exit /b 0
