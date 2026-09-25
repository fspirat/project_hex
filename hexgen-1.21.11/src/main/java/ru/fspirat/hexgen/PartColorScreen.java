package ru.fspirat.hexgen;

import java.util.List;
import java.util.function.BiConsumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Свой цвет или градиент для выделенной части текста. */
public class PartColorScreen extends Screen {
    private static final int W = 260;
    private static final int H = 128;
    private static boolean gradient = true;

    private final Screen parent;
    /** (начало, конец): конец = null — один цвет; начало = null — убрать свой цвет. */
    private final BiConsumer<Integer, Integer> apply;
    private String from;
    private String to;
    private int left;
    private int top;
    private Button fromButton;
    private Button toButton;

    public PartColorScreen(Screen parent, String from, String to, BiConsumer<Integer, Integer> apply) {
        super(Component.literal(HexUi.tr("part_color")));
        this.parent = parent;
        this.from = from;
        this.to = to;
        this.apply = apply;
    }

    @Override
    protected void init() {
        left = (this.width - W) / 2;
        top = Math.max(8, (this.height - H) / 2);
        int half = (W - 6) / 2;

        fromButton = Button.builder(Component.empty(),
                b -> this.minecraft.setScreen(new ColorPickerScreen(this, from, v -> from = v)))
                .bounds(left, top + 28, gradient ? half : W, 22).tooltip(HexUi.tip(HexUi.tr("open_palette"))).build();
        this.addRenderableWidget(fromButton);

        toButton = Button.builder(Component.empty(),
                b -> this.minecraft.setScreen(new ColorPickerScreen(this, to, v -> to = v)))
                .bounds(left + half + 6, top + 28, half, 22).tooltip(HexUi.tip(HexUi.tr("open_palette"))).build();
        toButton.visible = gradient;
        this.addRenderableWidget(toButton);

        this.addRenderableWidget(Button.builder(Component.literal(gradient ? HexUi.tr("part.mode_gradient") : HexUi.tr("part.mode_solid")), b -> {
            gradient = !gradient;
            this.clearWidgets();
            this.init();
        }).bounds(left, top + 56, W, 20).build());

        int bw = (W - 8) / 3;
        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("part.apply")), b -> {
            apply.accept(HexCore.rgb(from), gradient ? HexCore.rgb(to) : null);
            this.onClose();
        }).bounds(left, top + H - 20, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("part.remove")), b -> {
            apply.accept(null, null);
            this.onClose();
        }).bounds(left + bw + 4, top + H - 20, bw, 20)
                .tooltip(HexUi.tip(HexUi.tr("part.remove.hint"))).build());
        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("cancel")), b -> this.onClose())
                .bounds(left + (bw + 4) * 2, top + H - 20, bw, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        HexUi.drawPanel(g, left, top, W, H);
        super.render(g, mouseX, mouseY, delta);

        Component t = HexUi.gradientTitle(HexUi.tr("part.title"));
        g.drawString(this.font, t, (this.width - this.font.width(t)) / 2, top + 1, 0xFFFFFFFF, true);
        g.drawString(this.font, Component.literal(gradient ? HexUi.tr("part.start") : HexUi.tr("part.color")), left, top + 17, 0xFFA0A0A0, false);
        if (gradient) g.drawString(this.font, Component.literal(HexUi.tr("part.end")), toButton.getX(), top + 17, 0xFFA0A0A0, false);

        HexUi.drawSwatch(g, fromButton.getX(), fromButton.getY(), fromButton.getWidth(), fromButton.getHeight(),
                HexCore.rgb(from), fromButton.isHovered(), false);
        g.drawString(this.font, Component.literal("#" + from), fromButton.getX() + 5, fromButton.getY() + 7, label(from), false);
        if (gradient) {
            HexUi.drawSwatch(g, toButton.getX(), toButton.getY(), toButton.getWidth(), toButton.getHeight(),
                    HexCore.rgb(to), toButton.isHovered(), false);
            g.drawString(this.font, Component.literal("#" + to), toButton.getX() + 5, toButton.getY() + 7, label(to), false);
        }

        HexUi.drawGradient(g, left, top + 84, W, 6, gradient ? List.of(from, to) : List.of(from, from));
    }

    private static int label(String hex) {
        int rgb = HexCore.rgb(hex);
        double lum = 0.299 * ((rgb >> 16) & 255) + 0.587 * ((rgb >> 8) & 255) + 0.114 * (rgb & 255);
        return lum > 150 ? 0xFF000000 : 0xFFFFFFFF;
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
