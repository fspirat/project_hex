#!/usr/bin/env bash
# Генерирует исходники FSLOG для Minecraft 26.x из основной версии (fslog-1.21.11).
# Код правится только в fslog-1.21.11, затем: ./sync-fslog.sh
# Различия 26.x: ClientCommandManager → ClientCommands, сообщения в чат — addClientSystemMessage,
# в 26.2 чат — через gui.hud.
set -euo pipefail
cd "$(dirname "$0")"

SRC=fslog-1.21.11/src/main
for TARGET in fslog-26.2 fslog-26.1.2; do
  DST=$TARGET/src/main
  rm -rf "$DST/java/ru/fspirat/fslog" "$DST/resources"
  mkdir -p "$DST/java/ru/fspirat/fslog"
  for f in "$SRC"/java/ru/fspirat/fslog/*.java; do
    out="$DST/java/ru/fspirat/fslog/$(basename "$f")"
    sed -e 's/\bClientCommandManager\b/ClientCommands/g' \
        -e 's/gui\.getChat()\.addMessage(/gui.getChat().addClientSystemMessage(/g' "$f" > "$out"
    if [ "$TARGET" = fslog-26.2 ]; then sed -i 's/gui\.getChat()/gui.hud.getChat()/g' "$out"; fi
  done
  cp -r "$SRC/resources" "$DST/resources"
  mc=${TARGET#fslog-}; [ "$mc" = 26.1.2 ] && mc=26.1
  sed -i -e "s/\"minecraft\": \"~1.21.11\"/\"minecraft\": \"~$mc\"/" -e 's/"java": ">=21"/"java": ">=25"/' "$DST/resources/fabric.mod.json"
done
echo "OK: fslog-26.2 и fslog-26.1.2 обновлены"
