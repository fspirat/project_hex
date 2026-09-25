package ru.fspirat.hexgen;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Общая отрисовка для окон мода. */
public final class HexUi {
    private HexUi() {}

    public static final String TITLE = "HEX GENERATOR";
    public static final List<String> TITLE_STOPS = List.of("B04DFF", "FF8FE0");

    /** Подсказка для кнопки или null, если подсказки выключены в настройках. */
    public static Tooltip tip(String text) {
        return HexConfig.showHints ? Tooltip.create(Component.literal(text)) : null;
    }

    /** Текст, раскрашенный градиентом (жирным). */
    public static MutableComponent gradientTitle(String text) {
        MutableComponent c = Component.empty();
        for (HexCore.Glyph gl : HexCore.gradientGlyphs(text, TITLE_STOPS)) {
            c.append(Component.literal(gl.ch()).withStyle(Style.EMPTY.withColor(gl.rgb()).withBold(true)));
        }
        return c;
    }

    /** Строка с цветами и форматом каждого символа. */
    public static MutableComponent styled(List<HexCore.StyledGlyph> glyphs) {
        MutableComponent root = Component.empty();
        for (HexCore.StyledGlyph g : glyphs) {
            int bits = g.bits();
            Style st = Style.EMPTY.withColor(g.rgb())
                    .withBold((bits & HexCore.BOLD) != 0).withItalic((bits & HexCore.ITALIC) != 0)
                    .withUnderlined((bits & HexCore.UNDERLINE) != 0).withStrikethrough((bits & HexCore.STRIKE) != 0);
            root.append(Component.literal(g.ch()).withStyle(st));
        }
        return root;
    }

    /** Тёмная подложка окна, чтобы интерфейс не сливался с миром. */
    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        int x0 = x - 8, y0 = y - 6, x1 = x + w + 8, y1 = y + h + 6;
        g.fill(x0, y0, x1, y1, 0xD0101014);
        g.fill(x0, y0, x1, y0 + 1, 0xFF5A2A8A);
        g.fill(x0, y1 - 1, x1, y1, 0xFF5A2A8A);
        g.fill(x0, y0, x0 + 1, y1, 0xFF5A2A8A);
        g.fill(x1 - 1, y0, x1, y1, 0xFF5A2A8A);
    }

    /** Образец цвета с рамкой; белая рамка — при наведении, жёлтая — если цвет выбран. */
    public static void drawSwatch(GuiGraphics g, int x, int y, int w, int h, int rgb, boolean hovered, boolean selected) {
        int border = hovered ? 0xFFFFFFFF : selected ? 0xFFFFFF55 : 0xFF000000;
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF000000 | rgb);
    }

    /** Горизонтальная полоса градиента. */
    public static void drawGradient(GuiGraphics g, int x, int y, int w, int h, List<String> stops) {
        for (String s : stops) if (!HexCore.valid(s)) return;
        for (int i = 0; i < w; i++) {
            int rgb = HexCore.colorAt(stops, w > 1 ? i / (double) (w - 1) : 0);
            g.fill(x + i, y, x + i + 1, y + h, 0xFF000000 | rgb);
        }
    }

    /** Рамка «как у подсказки предмета». */
    public static void drawTooltipBox(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xF0100010);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x505000FF);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x5028007F);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, 0x505000FF);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0x505000FF);
    }
}
