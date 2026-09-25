package ru.fspirat.hexgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Выбор формата: команды AgeMagic и другие RGB-форматы. */
public class FormatScreen extends Screen {
    private static final int W = 300;
    private static final int H = 226;
    private static final int COL = 148;

    private final Screen parent;
    private final IntConsumer select;
    private final List<Button> buttons = new ArrayList<>();
    private int left;
    private int top;

    public FormatScreen(Screen parent, IntConsumer select) {
        super(Component.literal(HexUi.tr("format.title")));
        this.parent = parent;
        this.select = select;
    }

    @Override
    protected void init() {
        left = (this.width - W) / 2;
        top = Math.max(8, (this.height - H) / 2);
        buttons.clear();

        // AgeMagic: /itemname, /itemlore, /sponsor prefix, no command.
        int[] ageMagic = {0, 1, 3, 2};
        for (int i = 0; i < ageMagic.length; i++) {
            addFormat(ageMagic[i], left + (i % 2) * (COL + 4), top + 24 + (i / 2) * 22);
        }
        for (int f = HexCore.FIRST_OTHER; f < HexCore.FORMAT_NAMES.length; f++) {
            int i = f - HexCore.FIRST_OTHER;
            addFormat(f, left + (i % 2) * (COL + 4), top + 80 + (i / 2) * 22);
        }

        EditBox prefix = new SelectableEditBox(this.font, left, top + 182, COL, 18, Component.literal(HexUi.tr("format.prefix")));
        prefix.setMaxLength(64);
        prefix.setValue(HexConfig.formatPrefix);
        prefix.setResponder(v -> {
            HexConfig.formatPrefix = v;
            HexConfig.save();
        });
        prefix.setTooltip(HexUi.tip(HexUi.tr("format.prefix.hint")));
        this.addRenderableWidget(prefix);

        EditBox template = new SelectableEditBox(this.font, left + COL + 4, top + 182, COL, 18, Component.literal(HexUi.tr("format.template")));
        template.setMaxLength(128);
        template.setValue(HexConfig.birdflopTemplate);
        template.setResponder(v -> {
            HexConfig.birdflopTemplate = v.isEmpty() ? HexCore.DEFAULT_BIRDFLOP : v;
            HexConfig.save();
        });
        template.setTooltip(HexUi.tip(HexUi.tr("format.template.hint")));
        this.addRenderableWidget(template);

        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("done")), b -> this.onClose())
                .bounds(left + W / 2 - 60, top + H - 20, 120, 20).build());
    }

    private void addFormat(int format, int x, int y) {
        Button b = Button.builder(Component.literal(HexUi.visible(HexCore.FORMAT_NAMES[format])), btn -> {
            select.accept(format);
        }).bounds(x, y, COL, 20).build();
        b.setTooltip(null);
        buttons.add(b);
        this.addRenderableWidget(b);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        HexUi.drawPanel(g, left, top, W, H);
        super.extractRenderState(g, mouseX, mouseY, delta);

        Component t = HexUi.gradientTitle(HexUi.tr("format.title"));
        g.text(this.font, t, (this.width - this.font.width(t)) / 2, top + 1, 0xFFFFFFFF, true);
        g.text(this.font, Component.literal("AgeMagic"), left, top + 14, 0xFFA0A0A0, false);
        g.text(this.font, Component.literal(HexUi.tr("format.other")), left, top + 70, 0xFFA0A0A0, false);
        g.text(this.font, Component.literal(HexUi.tr("format.prefix")), left, top + 172, 0xFFA0A0A0, false);
        g.text(this.font, Component.literal(HexUi.tr("format.template")), left + COL + 4, top + 172, 0xFFA0A0A0, false);

        // Текущий формат — в жёлтой рамке.
        int current = HexState.S.command;
        int[] order = {0, 1, 3, 2, 4, 5, 6, 7, 8, 9, 10};
        for (int i = 0; i < buttons.size(); i++) {
            if (order[i] != current) continue;
            Button b = buttons.get(i);
            int x0 = b.getX() - 1, y0 = b.getY() - 1, x1 = b.getX() + b.getWidth() + 1, y1 = b.getY() + b.getHeight() + 1;
            g.fill(x0, y0, x1, y0 + 1, 0xFFFFD24D);
            g.fill(x0, y1 - 1, x1, y1, 0xFFFFD24D);
            g.fill(x0, y0, x0 + 1, y1, 0xFFFFD24D);
            g.fill(x1 - 1, y0, x1, y1, 0xFFFFD24D);
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
