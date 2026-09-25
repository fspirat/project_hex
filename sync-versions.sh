#!/usr/bin/env bash
# Генерирует исходники для Minecraft 26.x из основной версии (hexgen-1.21.11).
# Код правится только в hexgen-1.21.11, затем: ./sync-versions.sh
# Различия 26.x: GuiGraphics → GuiGraphicsExtractor, render → extractRenderState,
# drawString → text, renderItem → item, setScreen идёт через minecraft.gui,
# afterRender → afterExtract, KeyBindingHelper → KeyMappingHelper.
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
        -e 's/this\.minecraft\.setScreen(/this.minecraft.gui.setScreen(/g' \
        -e 's/client\.setScreen(/client.gui.setScreen(/g' \
        -e 's/ScreenEvents\.afterRender(screen)/ScreenEvents.afterExtract(screen)/' \
        -e 's/import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;/import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;/' \
        -e 's/KeyBindingHelper\.registerKeyBinding(/KeyMappingHelper.registerKeyMapping(/' \
        "$f" > "$DST/$rel"
  done
  cp "$SRC"/resources/assets/hexgen/icon.png "$DST/resources/assets/hexgen/icon.png"
  cp "$SRC"/resources/assets/hexgen/lang/*.json "$DST/resources/assets/hexgen/lang/"
done
echo "OK: hexgen-26.2 и hexgen-26.1.2 обновлены"
