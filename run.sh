#!/usr/bin/env sh
# Compila y ejecuta Nexo en Linux/macOS. Uso: ./run.sh [demo | server | client IP PUERTO]
set -e
cd "$(dirname "$0")"
if ! command -v javac >/dev/null 2>&1; then
  echo "[Nexo] No se encontró el JDK de Java (javac). Instala Java 17 o superior: https://adoptium.net"
  exit 1
fi
echo "[Nexo] Compilando..."
rm -rf out
javac -encoding UTF-8 --release 17 -d out --source-path src/main/java src/main/java/com/nexo/Main.java
echo "[Nexo] Iniciando..."
exec java -Dfile.encoding=UTF-8 -cp out com.nexo.Main "$@"
