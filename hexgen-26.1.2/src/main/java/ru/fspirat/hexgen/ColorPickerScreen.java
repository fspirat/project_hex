package ru.fspirat.hexgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Палитра: выбор цвета ползунками HSV, быстрыми образцами или вводом HEX. */
public class ColorPickerScreen extends Screen {
    private static final List<String> RECENT = new ArrayList<>();
    private static final int MAX_RECENT = 15;

    private static final int W = 300;
    private static final int H = 214;
    private static final int CELL = 20;

    private final Screen parent;
    private final String original;
    private final Consumer<String> onPick;

    private double hue;
    private double sat;
    private double val;
    private boolean syncing = false;

    private int left;
    private int top;
    private Slider hueSlider;
    private Slider satSlider;
    private Slider valSlider;
    private EditBox hexBox;
    private final List<Button> swatchButtons = new ArrayList<>();
    private final List<String> swatchColors = new ArrayList<>();

    public ColorPickerScreen(Screen parent, String current, Consumer<String> onPick) {
        super(Component.literal(HexUi.TITLE));
        this.parent = parent;
        this.original = HexCore.valid(current) ? current : "FFFFFF";
        this.onPick = onPick;
        double[] hsv = HexCore.rgbToHsv(HexCore.rgb(this.original));
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];
    }

    private int currentRgb() {
        return HexCore.hsvToRgb(hue, sat, val);
    }

    /** Цвет изменён ползунком — обновляем поле HEX, не трогая сами ползунки. */
    private void fromSliders() {
        if (hexBox == null) return;
        syncing = true;
        hexBox.setValue(HexCore.hex(currentRgb()));
        syncing = false;
    }

    /** Цвет задан извне (HEX или образец) — выставляем ползунки. */
    private void setColor(String hex, boolean updateBox) {
        if (!HexCore.valid(hex)) return;
        double[] hsv = HexCore.rgbToHsv(HexCore.rgb(hex));
        // У серых и чёрных оттенок не определён — сохраняем прежний.
        if (hsv[1] > 0 && hsv[2] > 0) hue = hsv[0];
        if (hsv[2] > 0) sat = hsv[1];
        val = hsv[2];
        syncing = true;
        if (hueSlider != null) hueSlider.set(hue / 360.0);
        if (satSlider != null) satSlider.set(sat);
        if (valSlider != null) valSlider.set(val);
        if (updateBox && hexBox != null) hexBox.setValue(hex);
        syncing = false;
    }

    @Override
    protected void init() {
        left = (this.width - W) / 2;
        top = Math.max(4, (this.height - H) / 2);
        swatchButtons.clear();
        swatchColors.clear();

        int sx = left, sw = 196;
        hueSlider = new Slider(sx, top + 20, sw, hue / 360.0,
                v -> HexUi.tr("picker.hue", Math.round(v * 360)),
                v -> { hue = v * 360; fromSliders(); });
        satSlider = new Slider(sx, top + 48, sw, sat,
                v -> HexUi.tr("picker.saturation", Math.round(v * 100)),
                v -> { sat = v; fromSliders(); });
        valSlider = new Slider(sx, top + 76, sw, val,
                v -> HexUi.tr("picker.brightness", Math.round(v * 100)),
                v -> { val = v; fromSliders(); });
        this.addRenderableWidget(hueSlider);
        this.addRenderableWidget(satSlider);
        this.addRenderableWidget(valSlider);

        hexBox = new EditBox(this.font, left + 208, top + 78, W - 208, 16, Component.literal("HEX"));
        hexBox.setMaxLength(7);
        hexBox.setValue(HexCore.hex(currentRgb()));
        hexBox.setResponder(v -> {
            if (syncing) return;
            String c = HexCore.clean(v);
            if (HexCore.valid(c)) setColor(c, false);
        });
        this.addRenderableWidget(hexBox);

        // Быстрая палитра: 2 ряда по 15.
        int cols = W / CELL;
        for (int i = 0; i < HexCore.SWATCHES.length; i++) {
            addSwatch(HexCore.SWATCHES[i], left + (i % cols) * CELL, top + 116 + (i / cols) * CELL);
        }
        // Недавние цвета.
        for (int i = 0; i < RECENT.size(); i++) {
            addSwatch(RECENT.get(i), left + i * CELL, top + 168);
        }

        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("done")), b -> {
            String hex = HexCore.hex(currentRgb());
            RECENT.remove(hex);
            RECENT.add(0, hex);
            while (RECENT.size() > MAX_RECENT) RECENT.remove(RECENT.size() - 1);
            onPick.accept(hex);
            this.onClose();
        }).bounds(left, top + H - 20, W / 2 - 2, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("cancel")), b -> this.onClose())
                .bounds(left + W / 2 + 2, top + H - 20, W / 2 - 2, 20).build());
    }

    private void addSwatch(String hex, int x, int y) {
        Button b = Button.builder(Component.empty(), btn -> setColor(hex, true))
                .bounds(x, y, CELL - 2, CELL - 2)
                .tooltip(HexUi.tip("#" + hex))
                .build();
        swatchButtons.add(b);
        swatchColors.add(hex);
        this.addRenderableWidget(b);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        HexUi.drawPanel(g, left, top, W, H);
        super.extractRenderState(g, mouseX, mouseY, delta);

        Component t = HexUi.gradientTitle(HexUi.tr("picker.title"));
        g.text(this.font, t, (this.width - this.font.width(t)) / 2, top + 4, 0xFFFFFFFF, true);

        // Полоски под ползунками показывают, как изменится цвет.
        int sx = left, sw = 196;
        for (int x = 0; x < sw; x++) {
            double f = x / (double) (sw - 1);
            g.fill(sx + x, top + 41, sx + x + 1, top + 44, 0xFF000000 | HexCore.hsvToRgb(f * 360, 1, 1));
            g.fill(sx + x, top + 69, sx + x + 1, top + 72, 0xFF000000 | HexCore.hsvToRgb(hue, f, Math.max(val, 0.15)));
            g.fill(sx + x, top + 97, sx + x + 1, top + 100, 0xFF000000 | HexCore.hsvToRgb(hue, sat, f));
        }

        // Сравнение: новый цвет сверху, исходный снизу.
        int px = left + 208, pw = W - 208;
        g.fill(px - 1, top + 19, px + pw + 1, top + 73, 0xFF000000);
        g.fill(px, top + 20, px + pw, top + 46, 0xFF000000 | currentRgb());
        g.fill(px, top + 46, px + pw, top + 72, 0xFF000000 | HexCore.rgb(original));
        g.text(this.font, Component.literal(HexUi.tr("picker.new")), px + 3, top + 23, labelColor(currentRgb()), false);
        g.text(this.font, Component.literal(HexUi.tr("picker.old")), px + 3, top + 49, labelColor(HexCore.rgb(original)), false);

        g.text(this.font, Component.literal(HexUi.tr("picker.quick")), left, top + 106, 0xFFA0A0A0, false);
        g.text(this.font, Component.literal(RECENT.isEmpty() ? HexUi.tr("picker.recent_empty") : HexUi.tr("picker.recent")),
                left, top + 158, 0xFFA0A0A0, false);

        String cur = HexCore.hex(currentRgb());
        for (int i = 0; i < swatchButtons.size(); i++) {
            Button b = swatchButtons.get(i);
            String hex = swatchColors.get(i);
            HexUi.drawSwatch(g, b.getX(), b.getY(), b.getWidth(), b.getHeight(),
                    HexCore.rgb(hex), b.isHovered(), hex.equals(cur));
        }
    }

    private static int labelColor(int rgb) {
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

    private final class Slider extends AbstractSliderButton {
        private final DoubleFunction<String> label;
        private final DoubleConsumer onChange;

        Slider(int x, int y, int w, double value, DoubleFunction<String> label, DoubleConsumer onChange) {
            super(x, y, w, 20, Component.empty(), value);
            this.label = label;
            this.onChange = onChange;
            updateMessage();
        }

        void set(double v) {
            this.value = Math.max(0, Math.min(1, v));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (label != null) setMessage(Component.literal(label.apply(value)));
        }

        @Override
        protected void applyValue() {
            if (onChange != null && !syncing) onChange.accept(value);
        }
    }
}
