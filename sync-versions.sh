#!/usr/bin/env bash
# Генерирует исходники для Minecraft 26.x из основной версии (hexgen-1.21.11).
# Код правится только в hexgen-1.21.11, затем: ./sync-versions.sh
# Различия 26.x: GuiGraphics → GuiGraphicsExtractor, render → extractRenderState,
# drawString → text, renderItem → item, setScreen идёт через minecraft.gui (26.2 и 26.3),
# afterRender → afterExtract, KeyBindingHelper → KeyMappingHelper, ClientCommandManager → ClientCommands,
# сообщения в чат — addClientSystemMessage (в 26.2+ чат — через gui.hud). Логгер чата /log — пакет fslog, правится тоже здесь.
# В 26.3 окно игры на SDL вместо GLFW: коды клавиш берутся из InputConstants (они верные в каждой версии),
# а RawInput.java для 26.3 лежит отдельно — hexgen-26.3/platform/.
set -euo pipefail
cd "$(dirname "$0")"

SRC=hexgen-1.21.11/src/main
for TARGET in hexgen-26.3 hexgen-26.2 hexgen-26.1.2; do
  DST=$TARGET/src/main
  mkdir -p "$DST/java/ru/fspirat/hexgen/mixin" "$DST/java/ru/fspirat/hexgen/fslog" "$DST/resources/assets/hexgen/lang"
  for f in "$SRC"/java/ru/fspirat/hexgen/*.java "$SRC"/java/ru/fspirat/hexgen/mixin/*.java "$SRC"/java/ru/fspirat/hexgen/fslog/*.java; do
    rel=${f#"$SRC"/}
    sed -e 's/import net.minecraft.client.gui.GuiGraphics;/import net.minecraft.client.gui.GuiGraphicsExtractor;/' \
        -e 's/\bGuiGraphics g\b/GuiGraphicsExtractor g/g' \
        -e 's/public void render(GuiGraphicsExtractor/public void extractRenderState(GuiGraphicsExtractor/' \
        -e 's/super\.render(g,/super.extractRenderState(g,/' \
        -e 's/g\.drawString(/g.text(/g' \
        -e 's/g\.renderItem(/g.item(/g' \
        -e 's/ScreenEvents\.afterRender(screen)/ScreenEvents.afterExtract(screen)/' \
        -e 's/import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;/import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;/' \
        -e 's/KeyBindingHelper\.registerKeyBinding(/KeyMappingHelper.registerKeyMapping(/' \
        -e 's/\bClientCommandManager\b/ClientCommands/g' \
        -e 's/gui\.getChat()\.addMessage(/gui.getChat().addClientSystemMessage(/g' \
        "$f" > "$DST/$rel"
    # В 26.2+ окна открываются через minecraft.gui, а чат — через gui.hud; в 26.1 — как раньше.
    if [ "$TARGET" != hexgen-26.1.2 ]; then
      sed -i -e 's/this\.minecraft\.setScreen(/this.minecraft.gui.setScreen(/g' \
             -e 's/client\.setScreen(/client.gui.setScreen(/g' \
             -e 's/gui\.getChat()/gui.hud.getChat()/g' "$DST/$rel"
    fi
  done
  # Файлы, которые в этой версии устроены иначе (26.3: окно на SDL вместо GLFW), лежат в $TARGET/platform.
  if [ -d "$TARGET/platform" ]; then cp "$TARGET"/platform/*.java "$DST/java/ru/fspirat/hexgen/"; fi
  # В 26.3 окно «Открыть ссылку?» принимает URI, а не строку.
  if [ "$TARGET" = hexgen-26.3 ]; then
    sed -i 's/ConfirmLinkScreen\.confirmLinkNow(this, url)/ConfirmLinkScreen.confirmLinkNow(this, java.net.URI.create(url))/' \
      "$DST/java/ru/fspirat/hexgen/fslog/LogViewScreen.java"
  fi
  cp "$SRC"/resources/assets/hexgen/icon.png "$DST/resources/assets/hexgen/icon.png"
  cp "$SRC"/resources/assets/hexgen/lang/*.json "$DST/resources/assets/hexgen/lang/"
done

# Проверки в живой игре (src/gametest) гоняются ещё и на самой новой версии — 26.3.
# В 26.x текущее окно, оверлей и всплывашки — у minecraft.gui, чат — у gui.hud; в тестовом API Fabric клиентский мир — у getConnection().
GT=hexgen-1.21.11/src/gametest
for TARGET in hexgen-26.3; do
  DST=$TARGET/src/gametest
  rm -rf "$DST" && mkdir -p "$DST/java/ru/fspirat/hexgen/shots" "$DST/resources"
  for f in "$GT"/java/ru/fspirat/hexgen/shots/*.java; do
    sed -e 's/\bmc\.screen\b/mc.gui.screen()/g' \
        -e 's/\bmc\.setScreen(/mc.gui.setScreen(/g' \
        -e 's/\bmc\.getToastManager()/mc.gui.toastManager()/g' \
        -e 's/\bmc\.getOverlay()/mc.gui.overlay()/g' \
        -e 's/\bmc\.gui\.getChat()/mc.gui.hud.getChat()/g' \
        -e 's/\.getClientWorld()/.getConnection()/g' \
        "$f" > "$DST/java/ru/fspirat/hexgen/shots/${f##*/}"
  done
  cp "$GT"/resources/fabric.mod.json "$DST/resources/"
done
echo "OK: hexgen-26.3 (с проверками в игре), hexgen-26.2 и hexgen-26.1.2 обновлены"
