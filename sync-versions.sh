#!/usr/bin/env bash
# Генерирует исходники для Minecraft 26.x из основной версии (hexgen-1.21.11).
# Код правится только в hexgen-1.21.11, затем: ./sync-versions.sh
# Различия 26.x: GuiGraphics → GuiGraphicsExtractor, render → extractRenderState,
# drawString → text, renderItem → item, setScreen идёт через minecraft.gui (только 26.2),
# afterRender → afterExtract, KeyBindingHelper → KeyMappingHelper, ClientCommandManager → ClientCommands.
set -euo pipefail
cd "$(dirname "$0")"

SRC=hexgen-1.21.11/src/main
for TARGET in hexgen-26.2 hexgen-26.1.2; do
  DST=$TARGET/src/main
  mkdir -p "$DST/java/ru/fspirat/hexgen/mixin" "$DST/resources/assets/hexgen/lang"
  for f in "$SRC"/java/ru/fspirat/hexgen/*.java "$SRC"/java/ru/fspirat/hexgen/mixin/*.java; do
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
    # В 26.2 окна открываются через minecraft.gui, а чат — через gui.hud; в 26.1 — как раньше.
    if [ "$TARGET" = hexgen-26.2 ]; then
      sed -i -e 's/this\.minecraft\.setScreen(/this.minecraft.gui.setScreen(/g' \
             -e 's/client\.setScreen(/client.gui.setScreen(/g' \
             -e 's/gui\.getChat()/gui.hud.getChat()/g' "$DST/$rel"
    fi
  done
  cp "$SRC"/resources/assets/hexgen/icon.png "$DST/resources/assets/hexgen/icon.png"
  cp "$SRC"/resources/assets/hexgen/lang/*.json "$DST/resources/assets/hexgen/lang/"
done
echo "OK: hexgen-26.2 и hexgen-26.1.2 обновлены"
