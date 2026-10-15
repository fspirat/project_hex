#!/usr/bin/env bash
# Проверки логики без Minecraft (генератор &k, уведомление об обновлении, выгрузка логов) для всех трёх версий мода.
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
  javac -encoding UTF-8 -d "$d" $src/UpdateNotice.java tests/logic/src/UpdateNoticeTest.java
  java -Dstdout.encoding=UTF-8 -cp "$d" ru.fspirat.hexgen.UpdateNoticeTest || fail=1
done
# Выгрузка лога (LogUpload без Minecraft) — логгер чата внутри FSTWEAK.
for v in ${VERSIONS:-1.21.11 26.1.2 26.2}; do
  src=hexgen-$v/src/main/java/ru/fspirat/hexgen/fslog
  d=$OUT/log-$v; mkdir -p "$d"
  javac -encoding UTF-8 -d "$d" $src/LogUpload.java tests/logic/src/LogUploadTest.java
  echo "== hexgen-$v LogUpload"; java -Dstdout.encoding=UTF-8 -cp "$d" ru.fspirat.hexgen.fslog.LogUploadTest || fail=1
done
exit $fail
