package ru.fspirat.hexgen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Настройки FSTWEAK. */
public class SettingsScreen extends Screen {
    private static final int W = 260;
    private static final int H = 170;

    private final Screen parent;
    private int left;
    private int top;
    /** Кнопка, ждущая повторного нажатия для подтверждения удаления. */
    private String confirming = null;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Настройки"));
        this.parent = parent;
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    @Override
    protected void init() {
        left = (this.width - W) / 2;
        top = Math.max(8, (this.height - H) / 2);
        int y = top + 16;

        this.addRenderableWidget(Button.builder(Component.literal("Подсказки: " + (HexConfig.showHints ? "вкл" : "выкл")), b -> {
            HexConfig.showHints = !HexConfig.showHints;
            HexConfig.save();
            rebuild();
        }).bounds(left, y, W, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Вернуть кнопку в инвентаре на место"), b -> {
            HexConfig.buttonOffsetX = HexConfig.DEFAULT_OFFSET_X;
            HexConfig.buttonOffsetY = HexConfig.DEFAULT_OFFSET_Y;
            HexConfig.save();
            b.setMessage(Component.literal("Готово ✔"));
        }).bounds(left, y + 24, W, 20).build());

        Button hist = Button.builder(Component.literal("history".equals(confirming)
                ? "Точно очистить? Нажмите ещё раз"
                : "Очистить историю (" + HexConfig.HISTORY.size() + ")"), b -> {
            if ("history".equals(confirming)) {
                HexConfig.HISTORY.clear();
                HexConfig.save();
                confirming = null;
            } else {
                confirming = "history";
            }
            rebuild();
        }).bounds(left, y + 48, W, 20).build();
        hist.active = !HexConfig.HISTORY.isEmpty();
        this.addRenderableWidget(hist);

        Button presets = Button.builder(Component.literal("presets".equals(confirming)
                ? "Точно удалить все? Нажмите ещё раз"
                : "Удалить свои пресеты (" + HexConfig.USER_PRESETS.size() + ")"), b -> {
            if ("presets".equals(confirming)) {
                HexConfig.USER_PRESETS.clear();
                HexConfig.save();
                confirming = null;
            } else {
                confirming = "presets";
            }
            rebuild();
        }).bounds(left, y + 72, W, 20).build();
        presets.active = !HexConfig.USER_PRESETS.isEmpty();
        this.addRenderableWidget(presets);

        this.addRenderableWidget(Button.builder(Component.literal("Готово"), b -> this.onClose())
                .bounds(left + W / 2 - 60, top + H - 20, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        HexUi.drawPanel(g, left, top, W, H);
        super.render(g, mouseX, mouseY, delta);

        Component t = HexUi.gradientTitle("Настройки");
        g.drawString(this.font, t, (this.width - this.font.width(t)) / 2, top + 1, 0xFFFFFFFF, true);

        Component key = HexGenClient.OPEN_KEY.getTranslatedKeyMessage();
        g.drawString(this.font, Component.literal("Открыть генератор из игры: ").append(key), left, top + 116, 0xFFA0A0A0, false);
        g.drawString(this.font, Component.literal("Сменить: Настройки → Управление → «Разное»"), left, top + 128, 0xFF707070, false);
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
