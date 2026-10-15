#!/usr/bin/env bash
# Проверки логики без Minecraft (генератор &k и выгрузка логов) для всех трёх версий мода.
# Запуск из корня репозитория: bash tests/logic/run.sh
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT
fail=0
for v in ${VERSIONS:-1.21.11 26.1.2 26.2}; do
  src=hexgen-$v/src/main/java/ru/fspirat/hexgen
  d=$OUT/hexgen-$v; mkdir -p "$d"
  javac -encoding UTF-8 -d "$d" $src/HexCore.java $src/HexState.java $src/SiteData.java \
        tests/logic/stub/ru/fspirat/hexgen/HexUi.java tests/logic/src/HexCoreTest.java
  echo "== hexgen-$v"; java -Dstdout.encoding=UTF-8 -cp "$d" ru.fspirat.hexgen.HexCoreTest || fail=1
done
# Выгрузка лога (LogUpload без Minecraft): встроенная копия в FSTWEAK и отдельный FSLOG.
for v in ${VERSIONS:-1.21.11 26.1.2 26.2}; do
  for mod in hexgen fslog; do
    if [ $mod = hexgen ]; then pkg=ru.fspirat.hexgen.fslog; src=hexgen-$v/src/main/java/ru/fspirat/hexgen/fslog
    else pkg=ru.fspirat.fslog; src=fslog-$v/src/main/java/ru/fspirat/fslog; fi
    d=$OUT/log-$mod-$v; mkdir -p "$d/t"
    sed "s/^package ru\.fspirat\.hexgen\.fslog;/package $pkg;/" tests/logic/src/LogUploadTest.java > "$d/t/LogUploadTest.java"
    javac -encoding UTF-8 -d "$d" $src/LogUpload.java "$d/t/LogUploadTest.java"
    echo "== $mod-$v LogUpload"; java -Dstdout.encoding=UTF-8 -cp "$d" $pkg.LogUploadTest || fail=1
  done
done
exit $fail
