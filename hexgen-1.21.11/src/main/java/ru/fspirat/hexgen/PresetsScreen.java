package ru.fspirat.hexgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphics;
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
    private boolean deleting = false;
    private int left;
    private int top;

    public PresetsScreen(Screen parent, Consumer<String[]> apply) {
        super(Component.literal("Пресеты"));
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

        Button del = Button.builder(Component.literal(deleting ? "Удаление: вкл" : "Удаление: выкл"), b -> {
            deleting = !deleting;
            rebuild();
        }).bounds(left, top + H - 20, 150, 20).tooltip(HexUi.tip("Когда включено, нажатие на свой пресет удаляет его")).build();
        del.active = !own.isEmpty();
        this.addRenderableWidget(del);

        this.addRenderableWidget(Button.builder(Component.literal("Назад"), b -> this.onClose())
                .bounds(left + W - 150, top + H - 20, 150, 20).build());
    }

    private void add(HexCore.Preset p, int x, int y, boolean own) {
        Button b = Button.builder(Component.literal(p.name()), btn -> {
            if (own && deleting) {
                HexConfig.USER_PRESETS.remove(p);
                HexConfig.save();
                if (HexConfig.USER_PRESETS.isEmpty()) deleting = false;
                rebuild();
                return;
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
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        HexUi.drawPanel(g, left, top, W, H);
        super.render(g, mouseX, mouseY, delta);

        Component t = HexUi.gradientTitle("Пресеты");
        g.drawString(this.font, t, (this.width - this.font.width(t)) / 2, top + 1, 0xFFFFFFFF, true);
        g.drawString(this.font, Component.literal("Готовые"), left, top + 14, 0xFFA0A0A0, false);
        g.drawString(this.font, Component.literal(deleting ? "Свои — нажмите, чтобы удалить" : "Свои"), left, top + 118,
                deleting ? 0xFFFF5555 : 0xFFA0A0A0, false);
        if (HexConfig.USER_PRESETS.isEmpty()) {
            g.drawString(this.font, Component.literal("Пока пусто — нажмите «★ Сохранить» в генераторе"),
                    left, top + 134, 0xFF707070, false);
        }

        // Полоска градиента внизу каждой кнопки.
        for (int i = 0; i < buttons.size(); i++) {
            Button b = buttons.get(i);
            HexUi.drawGradient(g, b.getX() + 3, b.getY() + b.getHeight() - 4, b.getWidth() - 6, 2, List.of(presets.get(i).colors()));
        }
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
