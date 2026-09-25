package ru.fspirat.hexgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Готовые и свои пресеты градиентов. */
public class PresetsScreen extends Screen {
    private static final int W = 340;
    private static final int H = 218;
    private static final int COLS = 4;
    private static final int CW = 82;
    private static final int STEP = 86;

    private final Screen parent;
    private final Consumer<String[]> apply;
    private final List<Button> buttons = new ArrayList<>();
    private final List<HexCore.Preset> presets = new ArrayList<>();
    private static final String[] MODES = {"presets.mode.apply", "presets.mode.rename", "presets.mode.move", "presets.mode.delete"};
    private static final String[] MODE_HINTS = {
        "presets.own", "presets.own.rename", "presets.own.move", "presets.own.delete"
    };
    /** Что делает нажатие на свой пресет: 0 применить, 1 переименовать, 2 переместить, 3 удалить. */
    private int mode = 0;
    /** Пресет, выбранный для перемещения (индекс в своих), или -1. */
    private int moving = -1;
    private int left;
    private int top;

    public PresetsScreen(Screen parent, Consumer<String[]> apply) {
        super(Component.literal(HexUi.tr("presets")));
        this.parent = parent;
        this.apply = apply;
    }

    @Override
    protected void init() {
        left = (this.width - W) / 2;
        top = Math.max(8, (this.height - H) / 2);
        buttons.clear();
        presets.clear();

        for (int i = 0; i < HexCore.PRESETS.size(); i++) {
            HexCore.Preset p = HexCore.PRESETS.get(i);
            add(p, left + (i % COLS) * STEP, top + 24 + (i / COLS) * 22, false);
        }
        List<HexCore.Preset> own = HexConfig.USER_PRESETS;
        for (int i = 0; i < own.size(); i++) {
            add(own.get(i), left + (i % COLS) * STEP, top + 128 + (i / COLS) * 22, true);
        }

        if (own.isEmpty()) mode = 0;
        Button modeButton = Button.builder(Component.literal(HexUi.tr("presets.mode_button", HexUi.tr(MODES[mode]))), b -> {
            mode = (mode + 1) % MODES.length;
            moving = -1;
            rebuild();
        }).bounds(left, top + H - 20, 150, 20)
                .tooltip(HexUi.tip(HexUi.tr("presets.mode.hint"))).build();
        modeButton.active = !own.isEmpty();
        this.addRenderableWidget(modeButton);

        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("back")), b -> this.onClose())
                .bounds(left + W - 150, top + H - 20, 150, 20).build());
    }

    private void add(HexCore.Preset p, int x, int y, boolean own) {
        Button b = Button.builder(Component.literal(HexUi.presetName(p)), btn -> {
            if (own) {
                int idx = HexConfig.USER_PRESETS.indexOf(p);
                switch (mode) {
                    case 1 -> {
                        this.minecraft.gui.setScreen(new NamePresetScreen(this, p.colors(), p.name(), p.name(), name -> {}));
                        return;
                    }
                    case 2 -> {
                        if (moving < 0) moving = idx;
                        else {
                            HexConfig.movePreset(moving, idx);
                            moving = -1;
                        }
                        rebuild();
                        return;
                    }
                    case 3 -> {
                        HexConfig.USER_PRESETS.remove(p);
                        HexConfig.save();
                        rebuild();
                        return;
                    }
                    default -> { }
                }
            }
            apply.accept(p.colors());
            this.onClose();
        }).bounds(x, y, CW, 20).tooltip(HexUi.tip(String.join(" → ", p.colors()))).build();
        buttons.add(b);
        presets.add(p);
        this.addRenderableWidget(b);
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        HexUi.drawPanel(g, left, top, W, H);
        super.extractRenderState(g, mouseX, mouseY, delta);

        Component t = HexUi.gradientTitle(HexUi.tr("presets"));
        g.text(this.font, t, (this.width - this.font.width(t)) / 2, top + 1, 0xFFFFFFFF, true);
        g.text(this.font, Component.literal(HexUi.tr("presets.builtin")), left, top + 14, 0xFFA0A0A0, false);
        String hint = mode == 2 && moving >= 0 ? HexUi.tr("presets.own.move_target") : HexUi.tr(MODE_HINTS[mode]);
        g.text(this.font, Component.literal(hint), left, top + 118, mode == 3 ? 0xFFFF5555 : mode == 0 ? 0xFFA0A0A0 : 0xFFFFD24D, false);
        if (HexConfig.USER_PRESETS.isEmpty()) {
            g.text(this.font, Component.literal(HexUi.tr("presets.empty")),
                    left, top + 134, 0xFF707070, false);
        }

        // Полоска градиента внизу каждой кнопки; выбранный для перемещения — в жёлтой рамке.
        for (int i = 0; i < buttons.size(); i++) {
            Button b = buttons.get(i);
            if (moving >= 0 && moving < HexConfig.USER_PRESETS.size() && presets.get(i) == HexConfig.USER_PRESETS.get(moving)) {
                g.fill(b.getX() - 1, b.getY() - 1, b.getX() + b.getWidth() + 1, b.getY(), 0xFFFFD24D);
                g.fill(b.getX() - 1, b.getY() + b.getHeight(), b.getX() + b.getWidth() + 1, b.getY() + b.getHeight() + 1, 0xFFFFD24D);
                g.fill(b.getX() - 1, b.getY(), b.getX(), b.getY() + b.getHeight(), 0xFFFFD24D);
                g.fill(b.getX() + b.getWidth(), b.getY(), b.getX() + b.getWidth() + 1, b.getY() + b.getHeight(), 0xFFFFD24D);
            }
            HexUi.drawGradient(g, b.getX() + 3, b.getY() + b.getHeight() - 4, b.getWidth() - 6, 2, List.of(presets.get(i).colors()));
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
