#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"

if [ -x ".jdk/bin/javac" ]; then
  PATH="$PWD/.jdk/bin:$PATH"
  export PATH
fi

if ! command -v javac >/dev/null 2>&1; then
  echo "[Nexo] No se encontró javac. Ejecuta ./run.sh para preparar Java 17 o instala un JDK 17+." >&2
  exit 1
fi

mkdir -p target/test-classes
find src/main/java src/test/java -name '*.java' -print > target/test-sources.txt
javac -encoding UTF-8 --release 17 -d target/test-classes @target/test-sources.txt
java -Dfile.encoding=UTF-8 -cp target/test-classes com.nexo.ServerSmokeTest
