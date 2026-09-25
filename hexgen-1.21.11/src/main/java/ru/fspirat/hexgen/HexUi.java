package ru.fspirat.hexgen;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Общая отрисовка для окон мода. */
public final class HexUi {
    private HexUi() {}

    public static final String TITLE = "HEX GENERATOR";
    public static final List<String> TITLE_STOPS = List.of("B04DFF", "FF8FE0");

    /** Перевод строки мода на язык игры (assets/hexgen/lang): ключ без префикса «fstweak.». */
    public static String tr(String key, Object... args) {
        return I18n.get("fstweak." + key, args);
    }

    public static String onOff(boolean on) {
        return tr(on ? "on" : "off");
    }

    /** Имя пресета: у готовых это ключ перевода, у своих — то, что ввёл игрок. */
    public static String presetName(HexCore.Preset p) {
        return p.name().startsWith("preset.") ? tr(p.name()) : p.name();
    }

    public static String commandLabel(int command) {
        String label = HexCore.COMMAND_LABELS[command];
        return label != null ? label : tr("command.none");
    }

    /** Подсказка для кнопки или null, если подсказки выключены в настройках. */
    public static Tooltip tip(String text) {
        return HexConfig.showHints ? Tooltip.create(Component.literal(text)) : null;
    }

    /** Текст, раскрашенный градиентом (жирным); с включённой анимацией градиент переливается. */
    public static MutableComponent gradientTitle(String text) {
        MutableComponent c = Component.empty();
        int[] cps = text.codePoints().toArray();
        double phase = HexCore.animationPhase();
        for (int i = 0; i < cps.length; i++) {
            double t = cps.length > 1 ? i / (double) (cps.length - 1) : 0;
            int rgb = HexConfig.animatePreview ? HexCore.animatedColorAt(TITLE_STOPS, t, phase) : HexCore.colorAt(TITLE_STOPS, t);
            c.append(Component.literal(new String(Character.toChars(cps[i]))).withStyle(Style.EMPTY.withColor(rgb).withBold(true)));
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

    /** Подложка окна в выбранной теме, чтобы интерфейс не сливался с миром. */
    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        int x0 = x - 8, y0 = y - 6, x1 = x + w + 8, y1 = y + h + 6;
        List<String> stops = HexState.S.stops;
        boolean gradient = HexConfig.theme == 2 && stops.size() >= 1 && stops.stream().allMatch(HexCore::valid);
        switch (HexConfig.theme) {
            case 1 -> {
                g.fill(x0, y0, x1, y1, 0xE8080808);
                border(g, x0, y0, x1, y1, 0xFF3A3A3A);
            }
            case 2 -> {
                g.fill(x0, y0, x1, y1, 0xD8101014);
                if (!gradient) {
                    border(g, x0, y0, x1, y1, 0xFF5A2A8A);
                    break;
                }
                // Рамка цвета текущего градиента: слева первый цвет, справа последний.
                int w0 = x1 - x0;
                for (int i = 0; i < w0; i++) {
                    int c = 0xFF000000 | HexCore.colorAt(stops, w0 > 1 ? i / (double) (w0 - 1) : 0);
                    g.fill(x0 + i, y0, x0 + i + 1, y0 + 1, c);
                    g.fill(x0 + i, y1 - 1, x0 + i + 1, y1, c);
                }
                g.fill(x0, y0, x0 + 1, y1, 0xFF000000 | HexCore.rgb(stops.get(0)));
                g.fill(x1 - 1, y0, x1, y1, 0xFF000000 | HexCore.rgb(stops.get(stops.size() - 1)));
            }
            default -> {
                g.fill(x0, y0, x1, y1, 0xD0101014);
                border(g, x0, y0, x1, y1, 0xFF5A2A8A);
            }
        }
    }

    private static void border(GuiGraphics g, int x0, int y0, int x1, int y1, int c) {
        g.fill(x0, y0, x1, y0 + 1, c);
        g.fill(x0, y1 - 1, x1, y1, c);
        g.fill(x0, y0, x0 + 1, y1, c);
        g.fill(x1 - 1, y0, x1, y1, c);
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
