package ru.fspirat.hexgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** Логика генератора без зависимостей от Minecraft — та же, что на fspirat.ru/hex_generator. */
public final class HexCore {
    private HexCore() {}

    public static final String[] COMMANDS = {"/itemname ", "/itemlore ", "", "sponsor"};
    public static final String[] COMMAND_LABELS = {"/itemname", "/itemlore", "Без команды", "/sponsor prefix"};
    public static final String[] SYMBOLS = {"✦", "★", "❖", "➤", "⚔", "❤", "•", "❀"};
    public static final String SPONSOR_PREFIX = "sponsor";

    public record Preset(String name, String[] colors) {}

    public static final List<Preset> PRESETS = List.of(
        new Preset("Особая", new String[]{"B04DFF", "FF8FE0"}),
        new Preset("Закат", new String[]{"FAE8F3", "FF8F5A"}),
        new Preset("Пламя", new String[]{"FFE259", "FF7A00", "D10000"}),
        new Preset("Океан", new String[]{"00F0FF", "0066FF"}),
        new Preset("Изумруд", new String[]{"C6FF8A", "1FAA59"}),
        new Preset("Лёд", new String[]{"FFFFFF", "9BE7FF", "4A9BFF"}),
        new Preset("Эндер", new String[]{"1B0033", "8A2BE2", "E0B0FF"}),
        new Preset("Золото", new String[]{"FFF6B7", "FFC300", "B8860B"}),
        new Preset("Радуга", new String[]{"FF4D4D", "FFD24D", "4DFF88", "4DB8FF", "B84DFF"}),
        new Preset("Легендарная", new String[]{"FFB000", "FF6A00"}),
        new Preset("Мифическая", new String[]{"FF3CAC", "784BA0", "2B86C5"}),
        new Preset("Сакура", new String[]{"FFD1E8", "FF7EB9"}),
        new Preset("Северное сияние", new String[]{"43E97B", "38F9D7", "7F7FFF"}),
        new Preset("Незер", new String[]{"FF4E00", "7A0000"}),
        new Preset("Неон", new String[]{"00FFF0", "FF00E5"}),
        new Preset("Киберпанк", new String[]{"FCEE09", "FF2A6D", "05D9E8"})
    );

    private static final Map<Integer, String> SMALL = new HashMap<>();
    static {
        String latin = "abcdefghijklmnopqrstuvwxyz";
        String latinSc = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";
        for (int i = 0; i < latin.length(); i++) SMALL.put((int) latin.charAt(i), String.valueOf(latinSc.charAt(i)));
        String ru = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
        String ruSc = "ᴀбʙгдᴇᴇжзийᴋлᴍʜᴏпᴘᴄтуȹxцчшщъыьэюя";
        for (int i = 0; i < ru.length(); i++) SMALL.put((int) ru.charAt(i), String.valueOf(ruSc.charAt(i)));
    }

    public static String toSmallCaps(String text) {
        StringBuilder sb = new StringBuilder();
        text.toLowerCase(Locale.ROOT).codePoints().forEach(cp -> {
            String r = SMALL.get(cp);
            sb.append(r != null ? r : new String(Character.toChars(cp)));
        });
        return sb.toString();
    }

    public static String clean(String hex) {
        String s = hex == null ? "" : hex.replaceAll("[^0-9a-fA-F]", "");
        if (s.length() > 6) s = s.substring(0, 6);
        return s.toUpperCase(Locale.ROOT);
    }

    public static boolean valid(String hex) {
        return hex != null && hex.matches("[0-9A-F]{6}");
    }

    public static int rgb(String hex) {
        return Integer.parseInt(hex, 16);
    }

    public static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    public static String hex(int rgb) {
        return String.format(Locale.ROOT, "%06X", rgb & 0xFFFFFF);
    }

    /** Цвет в точке t (0..1) вдоль всего градиента. */
    public static int colorAt(List<String> stops, double t) {
        if (stops.size() == 1) return rgb(stops.get(0));
        double seg = t * (stops.size() - 1);
        int k = Math.min((int) Math.floor(seg), stops.size() - 2);
        return mix(rgb(stops.get(k)), rgb(stops.get(k + 1)), seg - k);
    }

    /** Делит текст на (n-1) равных частей, как на сайте. */
    public static List<String> segments(String text, int n) {
        int[] cps = text.codePoints().toArray();
        int parts = Math.max(1, n - 1);
        List<String> out = new ArrayList<>();
        for (int k = 0; k < parts; k++) {
            int a = (int) Math.round(cps.length * (double) k / parts);
            int b = (int) Math.round(cps.length * (double) (k + 1) / parts);
            out.add(new String(cps, a, b - a));
        }
        return out;
    }

    public record Glyph(String ch, int rgb) {}

    /** Буквы с цветами для предпросмотра обычного градиента. */
    public static List<Glyph> gradientGlyphs(String text, List<String> stops) {
        List<Glyph> out = new ArrayList<>();
        List<String> segs = segments(text, stops.size());
        for (int k = 0; k < segs.size(); k++) {
            int[] cps = segs.get(k).codePoints().toArray();
            int n = cps.length;
            for (int i = 0; i < n; i++) {
                int c = mix(rgb(stops.get(k)), rgb(stops.get(Math.min(k + 1, stops.size() - 1))), n > 1 ? (double) i / (n - 1) : 0);
                out.add(new Glyph(new String(Character.toChars(cps[i])), c));
            }
        }
        return out;
    }

    public static String formatCodes(boolean[] fmt) {
        String[] c = {"&l", "&o", "&n", "&m"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) if (fmt[i]) sb.append(c[i]);
        return sb.toString();
    }

    /** Формат {#AAAAAA>}текст{#BBBBBB<>}текст{#CCCCCC<} для /itemname, /itemlore и «без команды». */
    public static String gradientCommand(String prefix, String text, List<String> stops, boolean[] fmt) {
        String codes = formatCodes(fmt);
        List<String> segs = segments(text, stops.size());
        StringBuilder sb = new StringBuilder(prefix);
        for (int k = 0; k < segs.size(); k++) {
            sb.append("{#").append(stops.get(k)).append(k == 0 ? ">" : "<>").append("}").append(codes).append(segs.get(k));
        }
        sb.append("{#").append(stops.get(stops.size() - 1)).append("<}");
        return sb.toString();
    }

    /** Цвета 7 букв префикса sponsor. */
    public static List<Integer> sponsorColors(List<String> stops) {
        List<Integer> out = new ArrayList<>();
        int n = SPONSOR_PREFIX.length();
        for (int i = 0; i < n; i++) out.add(colorAt(stops, n > 1 ? (double) i / (n - 1) : 0));
        return out;
    }

    /** Формат /sponsor prefix {#..}{#..}{#..}{#..}{#..}{#..}{#..} {#ник} */
    public static String sponsorCommand(List<String> stops, String nickHex) {
        StringBuilder sb = new StringBuilder("/sponsor prefix ");
        for (int c : sponsorColors(stops)) sb.append("{#").append(hex(c)).append("}");
        sb.append(" {#").append(nickHex).append("}");
        return sb.toString();
    }

    /** HSV → RGB: h в градусах (0..360), s и v в 0..1. */
    public static int hsvToRgb(double h, double s, double v) {
        h = ((h % 360) + 360) % 360;
        double c = v * s, x = c * (1 - Math.abs((h / 60) % 2 - 1)), m = v - c;
        double r, g, b;
        if (h < 60) { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        return ((int) Math.round((r + m) * 255) << 16) | ((int) Math.round((g + m) * 255) << 8) | (int) Math.round((b + m) * 255);
    }

    /** RGB → {h (0..360), s (0..1), v (0..1)}. */
    public static double[] rgbToHsv(int rgb) {
        double r = ((rgb >> 16) & 255) / 255.0, g = ((rgb >> 8) & 255) / 255.0, b = (rgb & 255) / 255.0;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        double h = 0;
        if (d > 0) {
            if (max == r) h = 60 * (((g - b) / d) % 6);
            else if (max == g) h = 60 * ((b - r) / d + 2);
            else h = 60 * ((r - g) / d + 4);
        }
        if (h < 0) h += 360;
        return new double[]{h, max == 0 ? 0 : d / max, max};
    }

    /** Цвета быстрой палитры: 16 цветов Minecraft + популярные оттенки. */
    public static final String[] SWATCHES = {
        "000000", "0000AA", "00AA00", "00AAAA", "AA0000", "AA00AA", "FFAA00", "AAAAAA",
        "555555", "5555FF", "55FF55", "55FFFF", "FF5555", "FF55FF", "FFFF55",
        "FFFFFF", "FF8FE0", "FF3CAC", "B04DFF", "8A2BE2", "1B0033", "2B86C5", "0066FF",
        "00F0FF", "1FAA59", "C6FF8A", "FFF6B7", "FFC300", "FF7A00", "D10000"
    };

    private static final Random RNG = new Random();

    private static String hsl(double h, double s, double l) {
        s /= 100; l /= 100;
        double a = s * Math.min(l, 1 - l);
        int[] ch = new int[3];
        int[] ns = {0, 8, 4};
        for (int i = 0; i < 3; i++) {
            double k = (ns[i] + h / 30) % 12;
            double f = l - a * Math.max(-1, Math.min(k - 3, Math.min(9 - k, 1)));
            ch[i] = (int) Math.round(f * 255);
        }
        return hex((ch[0] << 16) | (ch[1] << 8) | ch[2]);
    }

    public static List<String> randomGradient() {
        int n = RNG.nextDouble() < 0.65 ? 2 : 3;
        double h = RNG.nextDouble() * 360, step = 35 + RNG.nextDouble() * 70;
        int dir = RNG.nextBoolean() ? 1 : -1;
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(hsl(((h + dir * step * i) % 360 + 360) % 360, 75 + RNG.nextDouble() * 25, 55 + RNG.nextDouble() * 20));
        }
        return out;
    }
}
