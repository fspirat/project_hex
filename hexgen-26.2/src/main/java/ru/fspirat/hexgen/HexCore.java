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
    /** Подписи команд; null — перевести ключ command.none. */
    public static final String[] COMMAND_LABELS = {"/itemname", "/itemlore", null, "/sponsor prefix"};
    /** Лимит длины для каждой команды: для /itemname и /itemlore считается только текст после команды. */
    public static final int[] LIMITS = {64, 65, 256, 256};

    /** Длина, которую сервер сравнивает с лимитом. */
    public static int measuredLength(String out, int command) {
        if (command <= 1 && out.startsWith(COMMANDS[command])) return out.length() - COMMANDS[command].length();
        return out.length();
    }

    /** Фаза анимации 0..1: градиент «бежит» по тексту и плавно возвращается. */
    public static double animationPhase() {
        return (System.currentTimeMillis() % 4000L) / 4000.0;
    }

    /** Цвет в точке t (0..1) со сдвигом фазы — туда и обратно, без скачков. */
    public static int animatedColorAt(List<String> stops, double t, double phase) {
        double u = (t / 2 + phase) % 1.0;
        double tri = u < 0.5 ? u * 2 : 2 - u * 2;
        return colorAt(stops, tri);
    }

    /** Те же буквы, но общий градиент сдвинут по фазе (свои цвета символов не трогаются). */
    public static List<StyledGlyph> animate(List<StyledGlyph> glyphs, List<String> stops, int[] colors, double phase) {
        List<StyledGlyph> out = new ArrayList<>(glyphs.size());
        int n = glyphs.size();
        for (int i = 0; i < n; i++) {
            StyledGlyph g = glyphs.get(i);
            boolean own = colors != null && i < colors.length && colors[i] >= 0;
            int rgb = own ? g.rgb() : animatedColorAt(stops, n > 1 ? i / (double) (n - 1) : 0, phase);
            out.add(new StyledGlyph(g.ch(), rgb, g.bits()));
        }
        return out;
    }
    public static final String[] SYMBOLS = {
        "✦", "★", "❖", "➤", "⚔", "❤", "•", "❀",
        "☆", "✧", "✪", "✯", "❂", "✺", "✿", "❁", "❋", "☀", "☁", "☂", "☃", "☄", "☾", "⚡",
        "☠", "☢", "⚠", "♛", "♚", "♠", "♣", "♥", "♦", "♡", "❥", "♪", "♫", "☯", "✔", "✘",
        "➜", "«", "»", "◆", "◇", "▲", "●", "⚜"
    };

    /** Биты форматирования: &l, &o, &n, &m. */
    public static final int BOLD = 1, ITALIC = 2, UNDERLINE = 4, STRIKE = 8;
    private static final String[] FORMAT_CODES = {"&l", "&o", "&n", "&m"};
    public static final String SPONSOR_PREFIX = "sponsor";

    public record Preset(String name, String[] colors) {}

    public static final List<Preset> PRESETS = List.of(
        new Preset("preset.special", new String[]{"B04DFF", "FF8FE0"}),
        new Preset("preset.sunset", new String[]{"FAE8F3", "FF8F5A"}),
        new Preset("preset.flame", new String[]{"FFE259", "FF7A00", "D10000"}),
        new Preset("preset.ocean", new String[]{"00F0FF", "0066FF"}),
        new Preset("preset.emerald", new String[]{"C6FF8A", "1FAA59"}),
        new Preset("preset.ice", new String[]{"FFFFFF", "9BE7FF", "4A9BFF"}),
        new Preset("preset.ender", new String[]{"1B0033", "8A2BE2", "E0B0FF"}),
        new Preset("preset.gold", new String[]{"FFF6B7", "FFC300", "B8860B"}),
        new Preset("preset.rainbow", new String[]{"FF4D4D", "FFD24D", "4DFF88", "4DB8FF", "B84DFF"}),
        new Preset("preset.legendary", new String[]{"FFB000", "FF6A00"}),
        new Preset("preset.mythic", new String[]{"FF3CAC", "784BA0", "2B86C5"}),
        new Preset("preset.sakura", new String[]{"FFD1E8", "FF7EB9"}),
        new Preset("preset.aurora", new String[]{"43E97B", "38F9D7", "7F7FFF"}),
        new Preset("preset.nether", new String[]{"FF4E00", "7A0000"}),
        new Preset("preset.neon", new String[]{"00FFF0", "FF00E5"}),
        new Preset("preset.cyberpunk", new String[]{"FCEE09", "FF2A6D", "05D9E8"})
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
        // Посимвольно, чтобы число символов не менялось (важно для маски форматирования).
        text.codePoints().map(Character::toLowerCase).forEach(cp -> {
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

    public static String formatCodes(int bits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) if ((bits & (1 << i)) != 0) sb.append(FORMAT_CODES[i]);
        return sb.toString();
    }

    /**
     * Переносит маску форматирования (по одному значению на символ) на изменённый текст:
     * общее начало и конец сохраняются, вставленные символы берут формат соседа слева.
     */
    public static int[] spliceMask(String oldText, String newText, int[] oldMask, int fallback) {
        int[] o = oldText.codePoints().toArray();
        int[] n = newText.codePoints().toArray();
        if (oldMask == null || oldMask.length != o.length) {
            int[] m = new int[n.length];
            java.util.Arrays.fill(m, fallback);
            return m;
        }
        int p = 0;
        while (p < o.length && p < n.length && o[p] == n[p]) p++;
        int sfx = 0;
        while (sfx < o.length - p && sfx < n.length - p && o[o.length - 1 - sfx] == n[n.length - 1 - sfx]) sfx++;
        int inherit = p > 0 ? oldMask[p - 1] : (o.length > 0 ? oldMask[0] : fallback);
        int[] m = new int[n.length];
        System.arraycopy(oldMask, 0, m, 0, p);
        for (int i = p; i < n.length - sfx; i++) m[i] = inherit;
        System.arraycopy(oldMask, o.length - sfx, m, n.length - sfx, sfx);
        return m;
    }

    /** Общий формат всех символов или -1, если формат разный. */
    public static int uniformBits(int[] mask, int fallback) {
        if (mask.length == 0) return fallback;
        for (int b : mask) if (b != mask[0]) return -1;
        return mask[0];
    }

    /** Буква с итоговым цветом и форматом. */
    public record StyledGlyph(String ch, int rgb, int bits) {}

    /**
     * Буквы с цветами и форматом: цвет берётся из своего цвета символа (colors[i] >= 0),
     * иначе из общего градиента.
     */
    public static List<StyledGlyph> styled(String text, List<String> stops, int[] mask, int[] colors, int fallbackBits) {
        List<StyledGlyph> out = new ArrayList<>();
        List<Glyph> glyphs = gradientGlyphs(text, stops);
        for (int i = 0; i < glyphs.size(); i++) {
            Glyph g = glyphs.get(i);
            int rgb = colors != null && i < colors.length && colors[i] >= 0 ? colors[i] : g.rgb();
            int bits = mask != null && i < mask.length ? mask[i] : fallbackBits;
            out.add(new StyledGlyph(g.ch(), rgb, bits));
        }
        return out;
    }

    public static boolean hasOverrides(int[] colors) {
        if (colors == null) return false;
        for (int c : colors) if (c >= 0) return true;
        return false;
    }

    /**
     * Команда с учётом маски и своих цветов: если формат одинаковый и своих цветов нет,
     * получается короткий градиент, иначе каждый символ пишется своим цветом {#RRGGBB} со своими кодами.
     */
    public static String gradientCommand(String prefix, String text, List<String> stops, int[] mask, int[] colors, int fallback) {
        int uniform = uniformBits(mask, fallback);
        if (uniform >= 0 && !hasOverrides(colors)) return gradientCommand(prefix, text, stops, uniform);
        StringBuilder sb = new StringBuilder(prefix);
        int prevBits = -1, prevRgb = -1;
        for (StyledGlyph g : styled(text, stops, mask, colors, fallback)) {
            // Пробел без подчёркивания можно не красить заново.
            if (g.ch().equals(" ") && g.bits() == prevBits && (g.bits() & (UNDERLINE | STRIKE)) == 0) {
                sb.append(' ');
                continue;
            }
            if (g.rgb() == prevRgb && g.bits() == prevBits) {
                sb.append(g.ch());
                continue;
            }
            sb.append("{#").append(hex(g.rgb())).append("}").append(formatCodes(g.bits())).append(g.ch());
            prevBits = g.bits();
            prevRgb = g.rgb();
        }
        return sb.toString();
    }

    /** Результат разбора готовой команды (импорт и история). */
    public record Parsed(int command, String text, List<String> stops, int[] mask, int[] colors, String nickHex) {}

    private static final java.util.regex.Pattern TOKEN =
            java.util.regex.Pattern.compile("\\{#([0-9A-Fa-f]{6})(>|<>|<)?\\}|&([0-9a-fk-orA-FK-OR])");

    /** Разбирает команду /itemname, /itemlore, /sponsor prefix или просто текст с тегами. Null — если это не похоже на градиент. */
    public static Parsed parse(String raw) {
        if (raw == null) return null;
        String s = raw.strip();
        if (s.isEmpty()) return null;
        int command = 2;
        for (int i = 0; i < COMMANDS.length; i++) {
            String c = COMMANDS[i];
            if (!c.isEmpty() && !c.equals("sponsor") && s.startsWith(c)) {
                command = i;
                s = s.substring(c.length());
                break;
            }
        }
        if (s.startsWith("/sponsor prefix ")) {
            List<String> tags = new ArrayList<>();
            java.util.regex.Matcher m = TOKEN.matcher(s);
            while (m.find()) if (m.group(1) != null) tags.add(m.group(1).toUpperCase(Locale.ROOT));
            if (tags.size() < 2) return null;
            List<String> stops = new ArrayList<>(tags.subList(0, Math.min(7, tags.size() - 1)));
            return new Parsed(3, "", stops, new int[0], new int[0], tags.get(tags.size() - 1));
        }

        StringBuilder text = new StringBuilder();
        List<Integer> mask = new ArrayList<>(), colors = new ArrayList<>();
        List<String> stops = new ArrayList<>();
        boolean gradient = false;
        int solid = -1, bits = 0, tags = 0;
        java.util.regex.Matcher m = TOKEN.matcher(s);
        int pos = 0;
        while (true) {
            boolean found = m.find();
            int end = found ? m.start() : s.length();
            for (int cp : s.substring(pos, end).codePoints().toArray()) {
                text.appendCodePoint(cp);
                mask.add(bits);
                colors.add(gradient ? -1 : solid);
            }
            if (!found) break;
            pos = m.end();
            if (m.group(1) != null) {
                tags++;
                String hex = m.group(1).toUpperCase(Locale.ROOT);
                String kind = m.group(2);
                bits = 0;
                if (kind == null) {
                    gradient = false;
                    solid = rgb(hex);
                } else {
                    stops.add(hex);
                    gradient = !kind.equals("<");
                    solid = -1;
                }
            } else {
                char c = Character.toLowerCase(m.group(3).charAt(0));
                int idx = "lonm".indexOf(c);
                if (idx >= 0) bits |= 1 << idx;
                else if (c == 'r') { bits = 0; solid = -1; }
            }
        }
        if (tags == 0 || text.length() == 0) return null;

        int[] cm = colors.stream().mapToInt(Integer::intValue).toArray();
        if (stops.size() < 2) {
            // Градиента нет — берём крайние цвета как основу, а сами цвета оставляем посимвольно.
            int first = -1, last = -1;
            for (int c : cm) if (c >= 0) { if (first < 0) first = c; last = c; }
            stops.clear();
            stops.add(hex(first < 0 ? 0xFFFFFF : first));
            stops.add(hex(last < 0 ? 0xFFFFFF : last));
        }
        while (stops.size() > 6) stops.remove(stops.size() - 2);
        return new Parsed(command, text.toString(), stops, mask.stream().mapToInt(Integer::intValue).toArray(), cm, null);
    }

    /** Формат {#AAAAAA>}текст{#BBBBBB<>}текст{#CCCCCC<} для /itemname, /itemlore и «без команды». */
    public static String gradientCommand(String prefix, String text, List<String> stops, int bits) {
        String codes = formatCodes(bits);
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
