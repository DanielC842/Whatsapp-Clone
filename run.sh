#!/usr/bin/env sh
# ==========================================================================
#  Nexo - compila y ejecuta la app en Linux / macOS.
#  Uso: ./run.sh [demo | server | client IP PUERTO]
#  Si no hay Java 17+, descarga uno portable en .jdk (solo la primera vez).
# ==========================================================================
set -e
cd "$(dirname "$0")"

usar_jdk_portable() {
  if [ -x ".jdk/bin/javac" ]; then PATH="$PWD/.jdk/bin:$PATH"; return 0; fi
  if [ -x ".jdk/Contents/Home/bin/javac" ]; then PATH="$PWD/.jdk/Contents/Home/bin:$PATH"; return 0; fi
  return 1
}

version_javac() {
  javac -version 2>&1 | awk '{print $2}' | cut -d. -f1
}

if ! usar_jdk_portable; then
  if command -v javac >/dev/null 2>&1 && [ "$(version_javac)" -ge 17 ] 2>/dev/null; then
    :
  else
    echo "[Nexo] No se encontró Java 17+. Descargando Java 17 portable (solo la primera vez)..."
    case "$(uname -s)" in
      Darwin) os=mac ;;
      *) os=linux ;;
    esac
    case "$(uname -m)" in
      arm64|aarch64) arch=aarch64 ;;
      *) arch=x64 ;;
    esac
    url="https://api.adoptium.net/v3/binary/latest/17/ga/$os/$arch/jdk/hotspot/normal/eclipse"
    rm -rf .jdk .jdk-tmp && mkdir .jdk-tmp
    if command -v curl >/dev/null 2>&1; then curl -fL "$url" -o jdk.tar.gz; else wget -O jdk.tar.gz "$url"; fi
    tar -xzf jdk.tar.gz -C .jdk-tmp
    mv .jdk-tmp/* .jdk && rm -rf .jdk-tmp jdk.tar.gz
    usar_jdk_portable || { echo "[Nexo] No se pudo preparar Java. Instálalo desde https://adoptium.net"; exit 1; }
  fi
fi

echo "[Nexo] Compilando..."
rm -rf out
javac -encoding UTF-8 --release 17 -d out --source-path src/main/java src/main/java/com/nexo/Main.java
echo "[Nexo] Iniciando..."
exec java -Dfile.encoding=UTF-8 -cp out com.nexo.Main "$@"
