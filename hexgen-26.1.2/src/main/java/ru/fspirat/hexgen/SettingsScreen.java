package ru.fspirat.hexgen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Настройки FSTWEAK. */
public class SettingsScreen extends Screen {
    private static final int W = 260;
    private static final int H = 214;

    private final Screen parent;
    private int left;
    private int top;
    /** Кнопка, ждущая повторного нажатия для подтверждения удаления. */
    private String confirming = null;

    public SettingsScreen(Screen parent) {
        super(Component.literal(HexUi.tr("settings")));
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

        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("settings.hints", HexUi.onOff(HexConfig.showHints))), b -> {
            HexConfig.showHints = !HexConfig.showHints;
            HexConfig.save();
            rebuild();
        }).bounds(left, y, W, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("settings.animation", HexUi.onOff(HexConfig.animatePreview))), b -> {
            HexConfig.animatePreview = !HexConfig.animatePreview;
            HexConfig.save();
            rebuild();
        }).bounds(left, y + 24, W, 20).tooltip(HexUi.tip(HexUi.tr("settings.animation.hint"))).build());

        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("settings.theme", HexUi.tr(HexConfig.THEMES[HexConfig.theme]))), b -> {
            HexConfig.theme = (HexConfig.theme + 1) % HexConfig.THEMES.length;
            HexConfig.save();
            rebuild();
        }).bounds(left, y + 48, W, 20).tooltip(HexUi.tip(HexUi.tr("settings.theme.hint"))).build());

        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("settings.reset_button")), b -> {
            HexConfig.buttonOffsetX = HexConfig.DEFAULT_OFFSET_X;
            HexConfig.buttonOffsetY = HexConfig.DEFAULT_OFFSET_Y;
            HexConfig.save();
            b.setMessage(Component.literal(HexUi.tr("settings.reset_done")));
        }).bounds(left, y + 72, W, 20).build());

        Button hist = Button.builder(Component.literal("history".equals(confirming)
                ? HexUi.tr("settings.clear_history.confirm")
                : HexUi.tr("settings.clear_history", HexConfig.HISTORY.size())), b -> {
            if ("history".equals(confirming)) {
                HexConfig.HISTORY.clear();
                HexConfig.save();
                confirming = null;
            } else {
                confirming = "history";
            }
            rebuild();
        }).bounds(left, y + 96, W, 20).build();
        hist.active = !HexConfig.HISTORY.isEmpty();
        this.addRenderableWidget(hist);

        Button presets = Button.builder(Component.literal("presets".equals(confirming)
                ? HexUi.tr("settings.delete_presets.confirm")
                : HexUi.tr("settings.delete_presets", HexConfig.USER_PRESETS.size())), b -> {
            if ("presets".equals(confirming)) {
                HexConfig.USER_PRESETS.clear();
                HexConfig.save();
                confirming = null;
            } else {
                confirming = "presets";
            }
            rebuild();
        }).bounds(left, y + 120, W, 20).build();
        presets.active = !HexConfig.USER_PRESETS.isEmpty();
        this.addRenderableWidget(presets);

        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("done")), b -> this.onClose())
                .bounds(left + W / 2 - 60, top + H - 20, 120, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        HexUi.drawPanel(g, left, top, W, H);
        super.extractRenderState(g, mouseX, mouseY, delta);

        Component t = HexUi.gradientTitle(HexUi.tr("settings"));
        g.text(this.font, t, (this.width - this.font.width(t)) / 2, top + 1, 0xFFFFFFFF, true);

        Component key = HexGenClient.OPEN_KEY.getTranslatedKeyMessage();
        g.text(this.font, Component.literal(HexUi.tr("settings.hotkey")).append(key), left, top + 164, 0xFFA0A0A0, false);
        g.text(this.font, Component.literal(HexUi.tr("settings.hotkey.change")), left, top + 176, 0xFF707070, false);
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
