package ru.fspirat.hexgen;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Общая отрисовка для окон мода. */
public final class HexUi {
    private HexUi() {}

    public static final String TITLE = "HEX GENERATOR";

    /** «fstweak 1.1» — версия берётся из fabric.mod.json (то же, что видно в Mod Menu). */
    public static final String VERSION_LABEL = "fstweak " + net.fabricmc.loader.api.FabricLoader.getInstance()
            .getModContainer("hexgen").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
    public static final List<String> TITLE_STOPS = List.of("B04DFF", "FF8FE0");
    /** Заголовок в теме FSPIRAT — зелёный, как на сайте. */
    public static final List<String> FSPIRAT_STOPS = List.of("7FBF3A", "D4FF9A");

    // Цвета сайта FSPIRAT
    public static final int C_SURFACE = 0xFF080B08, C_CARD = 0xFF0D120D, C_CARD_HI = 0xFF151C14, C_LINE = 0xFF273322,
            C_LINE_HI = 0xFF34452D, C_TEXT = 0xFFE8EEE6, C_MUTED = 0xFF7C8578, C_GREEN = 0xFF7FBF3A, C_GREEN_HI = 0xFF9BE052,
            C_GREEN_LT = 0xFFD4FF9A;

    /** Тема FSPIRAT (по умолчанию): свои кнопки и поля в стиле сайта. */
    public static boolean fspirat() {
        return HexConfig.theme == 0;
    }

    public static List<String> titleStops() {
        return fspirat() ? FSPIRAT_STOPS : TITLE_STOPS;
    }

    /** Главные кнопки окна (зелёные). */
    private static final Set<Button> ACCENT = Collections.newSetFromMap(new WeakHashMap<>());

    public static Button accent(Button b) {
        ACCENT.add(b);
        return b;
    }

    /** Перевод строки мода на язык игры (assets/hexgen/lang): ключ без префикса «fstweak.». */
    public static String tr(String key, Object... args) {
        return I18n.get("fstweak." + key, args);
    }

    /** Зажат ли Shift (читаем напрямую — так одинаково во всех версиях игры). */
    public static boolean shiftDown() {
        long w = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
        return w != 0 && (org.lwjgl.glfw.GLFW.glfwGetKey(w, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(w, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS);
    }

    public static String onOff(boolean on) {
        return tr(on ? "on" : "off");
    }

    /** Имя пресета: у готовых это ключ перевода, у своих — то, что ввёл игрок. */
    public static String presetName(HexCore.Preset p) {
        return p.name().startsWith("preset.") ? tr(p.name()) : p.name();
    }

    public static String commandLabel(int command) {
        return visible(HexCore.COMMAND_LABELS[command]);
    }

    /**
     * Строка для показа на экране: «§» игра считает кодом форматирования и съедает,
     * поэтому показываем похожий символ «⸹». В буфер обмена уходит настоящий «§».
     */
    public static String visible(String s) {
        return s.replace('§', '⸹');
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
            List<String> stops = titleStops();
            int rgb = HexConfig.animatePreview ? HexCore.animatedColorAt(stops, t, phase) : HexCore.colorAt(stops, t);
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
    public static void drawPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        int x0 = x - 8, y0 = y - 6, x1 = x + w + 8, y1 = y + h + 6;
        List<String> stops = HexState.S.stops;
        boolean gradient = HexConfig.theme == 3 && stops.size() >= 1 && stops.stream().allMatch(HexCore::valid);
        switch (HexConfig.theme) {
            case 0 -> {
                // как панели сайта: лёгкое зелёное свечение, тёмная карточка, тонкая рамка и зелёная метка сверху
                g.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, 0x147FBF3A);
                g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0x247FBF3A);
                g.fill(x0, y0, x1, y1, 0xF20B100B);
                g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + (y1 - y0) / 3, 0x0EFFFFFF);
                border(g, x0, y0, x1, y1, C_LINE_HI);
                g.fill(x0, y0, x0 + 28, y0 + 2, C_GREEN);
                g.fill(x1 - 3, y0 + 3, x1 - 1, y0 + 5, C_LINE_HI);
                g.fill(x1 - 6, y0 + 3, x1 - 4, y0 + 5, C_LINE_HI);
            }
            case 2 -> {
                g.fill(x0, y0, x1, y1, 0xE8080808);
                border(g, x0, y0, x1, y1, 0xFF3A3A3A);
            }
            case 3 -> {
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

    /**
     * Кнопки и поля в стиле FSPIRAT. Рисуются поверх стандартных виджетов каждый кадр, поэтому
     * окно выглядит одинаково в обычном Minecraft и в LabyMod. Кнопки без надписи (образцы цвета,
     * иконка предмета) окно рисует само — их не трогаем.
     */
    public static void skin(Screen screen, GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (!fspirat()) return;
        Font font = Minecraft.getInstance().font;
        for (GuiEventListener e : screen.children()) {
            if (e instanceof EditBox box) {
                if (box.isVisible()) {
                    int c = box.isFocused() ? C_GREEN : box.isHovered() ? C_LINE_HI : C_LINE;
                    border(g, box.getX(), box.getY(), box.getX() + box.getWidth(), box.getY() + box.getHeight(), c);
                }
                continue;
            }
            if (!(e instanceof Button b) || !b.visible || b instanceof MovableButton) continue;
            boolean accentBtn = ACCENT.contains(b);
            // на зелёной кнопке свои цвета надписи не нужны — тёмный текст читается лучше
            Component msg = accentBtn ? Component.literal(b.getMessage().getString()) : b.getMessage();
            if (msg.getString().isEmpty()) continue;
            // Стандартную кнопку делаем почти прозрачной (текст новых версий рисуется поверх всего),
            // а сверху рисуем свою. Почти, а не совсем: полностью прозрачный текст игра рисует непрозрачным.
            b.setAlpha(0.03f);
            int x = b.getX(), y = b.getY(), w = b.getWidth(), h = b.getHeight();
            boolean hover = b.active && b.isHovered(), accent = accentBtn;
            int bg = !b.active ? 0xFF0A0D0A : accent ? (hover ? C_GREEN_HI : C_GREEN) : hover ? C_CARD_HI : C_CARD;
            int edge = !b.active ? 0xFF1A2117 : accent ? bg : hover ? C_GREEN : C_LINE;
            g.fill(x, y, x + w, y + h, edge);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
            if (!accent) g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x50000000);
            int color = !b.active ? 0xFF5E665A : accent ? 0xFF071006 : hover ? C_GREEN_LT : C_TEXT;
            int tw = font.width(msg), ty = y + (h - 8) / 2;
            if (tw <= w - 6) {
                g.text(font, msg, x + (w - tw) / 2, ty, color, !accent);
            } else {
                String s = msg.getString();
                while (s.length() > 1 && font.width(s + "…") > w - 6) s = s.substring(0, s.length() - 1);
                String fitted = s + "…";
                g.text(font, Component.literal(fitted), x + (w - font.width(fitted)) / 2, ty, color, !accent);
            }
        }
    }

    private static void border(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int c) {
        g.fill(x0, y0, x1, y0 + 1, c);
        g.fill(x0, y1 - 1, x1, y1, c);
        g.fill(x0, y0, x0 + 1, y1, c);
        g.fill(x1 - 1, y0, x1, y1, c);
    }

    /** Иконка кнопки в инвентаре: белая четырёхлучевая звёздочка 16×16 (как иконка предмета). */
    private static final String[] SPARKLE = {
        ".......##.......",
        ".......##.......",
        ".......##.......",
        "......####......",
        "......####......",
        ".....######.....",
        "...##########...",
        "################",
        "################",
        "...##########...",
        ".....######.....",
        "......####......",
        "......####......",
        ".......##.......",
        ".......##.......",
        ".......##......."
    };

    public static void drawSparkle(GuiGraphicsExtractor g, int x, int y) {
        // Сначала тень со сдвигом на пиксель, потом сама звёздочка — рядами, чтобы не рисовать по пикселю.
        for (int pass = 0; pass < 2; pass++) {
            int off = pass == 0 ? 1 : 0, color = pass == 0 ? 0xFF3A3A3A : 0xFFFFFFFF;
            for (int row = 0; row < SPARKLE.length; row++) {
                String line = SPARKLE[row];
                int start = line.indexOf('#'), end = line.lastIndexOf('#') + 1;
                g.fill(x + start + off, y + row + off, x + end + off, y + row + 1 + off, color);
            }
        }
    }

    /** Образец цвета с рамкой; белая рамка — при наведении, жёлтая — если цвет выбран. */
    public static void drawSwatch(GuiGraphicsExtractor g, int x, int y, int w, int h, int rgb, boolean hovered, boolean selected) {
        int border = hovered ? 0xFFFFFFFF : selected ? 0xFFFFFF55 : 0xFF000000;
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF000000 | rgb);
    }

    /** Горизонтальная полоса градиента. */
    public static void drawGradient(GuiGraphicsExtractor g, int x, int y, int w, int h, List<String> stops) {
        for (String s : stops) if (!HexCore.valid(s)) return;
        for (int i = 0; i < w; i++) {
            int rgb = HexCore.colorAt(stops, w > 1 ? i / (double) (w - 1) : 0);
            g.fill(x + i, y, x + i + 1, y + h, 0xFF000000 | rgb);
        }
    }

    /** Рамка «как у подсказки предмета». */
    public static void drawTooltipBox(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xF0100010);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x505000FF);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x5028007F);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, 0x505000FF);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0x505000FF);
    }
}
