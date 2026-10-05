#!/usr/bin/env bash
# FSLOG: исходники правятся только в fslog-1.21.11, затем: ./sync-fslog.sh
#  1) отдельный мод для Minecraft 26.x (fslog-26.2, fslog-26.1.2);
#  2) встроенная копия в FSTWEAK (hexgen-*): пакет ru.fspirat.hexgen.fslog — чтобы не конфликтовать
#     с отдельным FSLOG, если стоят оба (тогда встроенный просто не включается).
# Различия 26.x: ClientCommandManager → ClientCommands, сообщения в чат — addClientSystemMessage,
# в 26.2 чат — через gui.hud.
set -euo pipefail
cd "$(dirname "$0")"

SRC=fslog-1.21.11/src/main

port() {  # port <файл> <версия: 1.21.11|26.1.2|26.2>
  if [ "$2" != 1.21.11 ]; then
    sed -i -e 's/\bClientCommandManager\b/ClientCommands/g' \
           -e 's/gui\.getChat()\.addMessage(/gui.getChat().addClientSystemMessage(/g' "$1"
  fi
  if [ "$2" = 26.2 ]; then sed -i 's/gui\.getChat()/gui.hud.getChat()/g' "$1"; fi
}

for V in 26.2 26.1.2; do
  DST=fslog-$V/src/main
  rm -rf "$DST/java/ru/fspirat/fslog" "$DST/resources"
  mkdir -p "$DST/java/ru/fspirat/fslog"
  for f in "$SRC"/java/ru/fspirat/fslog/*.java; do
    out="$DST/java/ru/fspirat/fslog/$(basename "$f")"; cp "$f" "$out"; port "$out" "$V"
  done
  cp -r "$SRC/resources" "$DST/resources"
  mc=$V; [ "$mc" = 26.1.2 ] && mc=26.1
  sed -i -e "s/\"minecraft\": \"~1.21.11\"/\"minecraft\": \"~$mc\"/" -e 's/"java": ">=21"/"java": ">=25"/' "$DST/resources/fabric.mod.json"
done

for V in 1.21.11 26.2 26.1.2; do
  DST=hexgen-$V/src/main/java/ru/fspirat/hexgen/fslog
  rm -rf "$DST"; mkdir -p "$DST"
  for f in "$SRC"/java/ru/fspirat/fslog/*.java; do
    [ "$(basename "$f")" = FsLogClient.java ] && continue
    out="$DST/$(basename "$f")"
    sed -e 's/^package ru\.fspirat\.fslog;/package ru.fspirat.hexgen.fslog;/' \
        -e 's/version("fslog")/version("hexgen")/g' "$f" > "$out"
    port "$out" "$V"
  done
done

# Переводы FSLOG — в общие файлы FSTWEAK
python3 - <<'PY'
import json, collections, glob, os
src = 'fslog-1.21.11/src/main/resources/assets/fslog/lang'
for v in ('1.21.11', '26.2', '26.1.2'):
    for f in glob.glob(src + '/*.json'):
        dst = f'hexgen-{v}/src/main/resources/assets/hexgen/lang/' + os.path.basename(f)
        if not os.path.exists(dst): continue
        d = json.load(open(dst, encoding='utf-8'), object_pairs_hook=collections.OrderedDict)
        d.update(json.load(open(f, encoding='utf-8')))
        open(dst, 'w', encoding='utf-8').write(json.dumps(d, ensure_ascii=False, indent=2) + '\n')
PY
echo "OK: fslog-26.x и встроенная копия в hexgen-* обновлены"
