package ru.fspirat.hexgen;

import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Окно выбора символов: каждый клик вставляет символ в текст. */
public class SymbolsScreen extends Screen {
    private static final int COLS = 12;
    private static final int CELL = 22;
    private static final int W = COLS * CELL - 2;

    private final Screen parent;
    private final Consumer<String> insert;
    private int left;
    private int top;
    private int h;

    public SymbolsScreen(Screen parent, Consumer<String> insert) {
        super(Component.literal("Символы"));
        this.parent = parent;
        this.insert = insert;
    }

    @Override
    protected void init() {
        int rows = (HexCore.SYMBOLS.length + COLS - 1) / COLS;
        h = 44 + rows * CELL + 26;
        left = (this.width - W) / 2;
        top = Math.max(4, (this.height - h) / 2);

        for (int i = 0; i < HexCore.SYMBOLS.length; i++) {
            final String sym = HexCore.SYMBOLS[i];
            this.addRenderableWidget(Button.builder(Component.literal(sym), b -> insert.accept(sym))
                    .bounds(left + (i % COLS) * CELL, top + 40 + (i / COLS) * CELL, CELL - 2, CELL - 2)
                    .tooltip(Tooltip.create(Component.literal("Вставить " + sym)))
                    .build());
        }

        this.addRenderableWidget(Button.builder(Component.literal("Готово"), b -> this.onClose())
                .bounds(left + W / 2 - 60, top + h - 20, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        HexGenScreen.drawPanel(g, left, top, W, h);
        super.render(g, mouseX, mouseY, delta);

        Component t = Component.literal("Символы");
        g.drawString(this.font, t, (this.width - this.font.width(t)) / 2, top + 4, 0xFFFFFFFF, true);

        // Текущий текст, чтобы было видно, что вставилось.
        g.fill(left, top + 18, left + W, top + 34, 0xC0000000);
        String text = HexGenScreen.currentText();
        String shown = text;
        while (!shown.isEmpty() && this.font.width(shown) > W - 8) shown = shown.substring(1);
        g.drawString(this.font, Component.literal(shown.length() < text.length() ? "…" + shown.substring(1) : shown),
                left + 4, top + 22, 0xFFFFFFFF, false);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
