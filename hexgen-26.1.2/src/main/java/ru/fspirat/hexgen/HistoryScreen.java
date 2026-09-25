package ru.fspirat.hexgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Последние скопированные и выполненные команды: нажатие загружает команду в генератор. */
public class HistoryScreen extends Screen {
    private static final int W = 340;
    private static final int ROW = 20;

    private final Screen parent;
    private final Predicate<String> load;
    private final List<Button> rows = new ArrayList<>();
    private int left;
    private int top;
    private int h;

    public HistoryScreen(Screen parent, Predicate<String> load) {
        super(Component.literal(HexUi.TITLE));
        this.parent = parent;
        this.load = load;
    }

    @Override
    protected void init() {
        List<String> hist = HexConfig.HISTORY;
        h = 16 + Math.max(1, hist.size()) * ROW + 26;
        left = (this.width - W) / 2;
        top = Math.max(8, (this.height - h) / 2);
        rows.clear();

        for (int i = 0; i < hist.size(); i++) {
            String cmd = hist.get(i);
            Button b = Button.builder(Component.empty(), btn -> {
                this.minecraft.setScreen(this.parent);
                load.test(cmd);
            }).bounds(left, top + 16 + i * ROW, W, ROW - 2).tooltip(HexUi.tip(cmd)).build();
            rows.add(b);
            this.addRenderableWidget(b);
        }

        Button clear = Button.builder(Component.literal(HexUi.tr("history.clear")), b -> {
            HexConfig.HISTORY.clear();
            HexConfig.save();
            this.clearWidgets();
            this.init();
        }).bounds(left, top + h - 20, 150, 20).build();
        clear.active = !hist.isEmpty();
        this.addRenderableWidget(clear);

        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("back")), b -> this.onClose())
                .bounds(left + W - 150, top + h - 20, 150, 20).build());
    }

    /** Цветной предпросмотр команды, обрезанный по ширине. */
    private MutableComponent previewOf(String cmd, int maxW) {
        HexCore.Parsed p = HexCore.parse(cmd);
        MutableComponent root = Component.empty();
        if (p == null) return root.append(Component.literal(cmd));
        String prefix = p.command() == 3 ? "/sponsor " : HexCore.COMMANDS[p.command()];
        root.append(Component.literal(prefix).withStyle(Style.EMPTY.withColor(0x808080)));
        int w = this.font.width(prefix);

        List<HexCore.StyledGlyph> glyphs;
        if (p.command() == 3) {
            glyphs = new ArrayList<>();
            String sc = HexCore.toSmallCaps(HexCore.SPONSOR_PREFIX);
            List<Integer> cols = HexCore.sponsorColors(p.stops());
            int[] cps = sc.codePoints().toArray();
            for (int i = 0; i < cps.length; i++) glyphs.add(new HexCore.StyledGlyph(new String(Character.toChars(cps[i])), cols.get(i), 0));
            glyphs.add(new HexCore.StyledGlyph(" ", 0xFFFFFF, 0));
            for (char c : "nickname".toCharArray()) glyphs.add(new HexCore.StyledGlyph(String.valueOf(c), HexCore.rgb(p.nickHex()), 0));
        } else {
            glyphs = HexCore.styled(p.text(), p.stops(), p.mask(), p.colors(), 0);
        }
        List<HexCore.StyledGlyph> fitted = new ArrayList<>();
        for (HexCore.StyledGlyph g : glyphs) {
            int cw = this.font.width(g.ch()) + ((g.bits() & HexCore.BOLD) != 0 ? 1 : 0);
            if (w + cw > maxW - this.font.width("…")) {
                root.append(HexUi.styled(fitted));
                return root.append(Component.literal("…").withStyle(Style.EMPTY.withColor(0x808080)));
            }
            w += cw;
            fitted.add(g);
        }
        return root.append(HexUi.styled(fitted));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        HexUi.drawPanel(g, left, top, W, h);
        super.extractRenderState(g, mouseX, mouseY, delta);

        Component t = HexUi.gradientTitle(HexUi.tr("history"));
        g.text(this.font, t, (this.width - this.font.width(t)) / 2, top + 1, 0xFFFFFFFF, true);

        if (HexConfig.HISTORY.isEmpty()) {
            g.text(this.font, Component.literal(HexUi.tr("history.empty")),
                    left, top + 22, 0xFF707070, false);
        }
        for (int i = 0; i < rows.size() && i < HexConfig.HISTORY.size(); i++) {
            Button b = rows.get(i);
            g.text(this.font, previewOf(HexConfig.HISTORY.get(i), W - 10), b.getX() + 5, b.getY() + 5, 0xFFFFFFFF, true);
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
