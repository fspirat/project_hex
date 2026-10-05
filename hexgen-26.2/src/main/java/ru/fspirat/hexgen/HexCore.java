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

    // Форматы: 0–3 — AgeMagic (как раньше), 4–10 — другие RGB-форматы.
    public static final int NICKNAME = 4, CHAT = 5, LEGACY = 6, CONSOLE = 7, BBCODE = 8, MINIMESSAGE = 9, BIRDFLOP = 10;
    public static final int FIRST_OTHER = NICKNAME;
    public static final String[] COMMANDS = {"/itemname ", "/itemlore ", "", "sponsor", "", "", "", "", "", "", "", "/sponsor editprefix "};
    /** Короткие подписи для кнопки в главном окне. */
    public static final String[] COMMAND_LABELS = {"/itemname", "/itemlore", "no command", "/sponsor prefix",
        "Nickname &#", "Chat <#>", "Legacy &x", "Console §x", "BBCode", "MiniMessage", "BirdFlop", "/sponsor editprefix"};
    /** Полные названия в окне выбора формата. */
    public static final String[] FORMAT_NAMES = {"/itemname (AgeMagic)", "/itemlore (AgeMagic)", "no command (AgeMagic)",
        "/sponsor prefix (AgeMagic)", "Nickname &#rrggbb", "Chat <#rrggbb>", "Legacy &x&r&r&g&g&b&b",
        "Console §x§r§r§g§g§b§b", "BBCode [COLOR=#rrggbb]", "MiniMessage", "BirdFlop", "/sponsor editprefix (AgeMagic)"};
    /** Лимит длины: для /itemname и /itemlore считается только текст после команды, остальное — лимит чата. */
    public static final int[] LIMITS = {64, 65, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256};
    /** /sponsor editprefix: свой текст префикса градиентом и цвет ника (MiniMessage). */
    public static final int SPONSOR_EDIT = 11;
    /** Шаблон BirdFlop по умолчанию: $1…$6 — цифры цвета, $f — коды формата, $c — символ. */
    public static final String DEFAULT_BIRDFLOP = "&#$1$2$3$4$5$6$f$c";

    public static boolean isAgeMagic(int format) {
        return format < FIRST_OTHER || format == SPONSOR_EDIT;
    }

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

    /** Готовые палитры — те же, что на сайте (SiteData), имя — ключ перевода preset.<key>. */
    public static final List<Preset> PRESETS = SiteData.PALETTES.stream()
            .map(p -> new Preset("preset." + p.key(), p.colors())).toList();

    private static final Map<Integer, String> SMALL = new HashMap<>();
    static {
        String latin = "abcdefghijklmnopqrstuvwxyz";
        String latinSc = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";
        for (int i = 0; i < latin.length(); i++) SMALL.put((int) latin.charAt(i), String.valueOf(latinSc.charAt(i)));
        String ru = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
        String ruSc = "ᴀбʙгдᴇᴇжзийᴋлᴍʜᴏпᴘᴄтуȹxцчшщъыьэюя";
        for (int i = 0; i < ru.length(); i++) SMALL.put((int) ru.charAt(i), String.valueOf(ruSc.charAt(i)));
    }

    /** Шрифты, как на сайте: 0 обычный, 1 Small Caps, дальше — стили из Unicode (меняется только латиница). */
    public static final String[] FONTS = {"normal", "small", "fraktur", "frakturBold", "scriptBold", "script", "double",
        "currency", "asian", "sansItalic", "mono", "sansBold"};
    private static final String[] FONT_LABELS = {null, "ꜱᴍᴀʟʟ ᴄᴀᴘꜱ", "𝔉𝔯𝔞𝔨𝔱𝔲𝔯", "𝕱𝖗𝖆𝖐𝖙𝖚𝖗", "𝓢𝓬𝓻𝓲𝓹𝓽", "𝒮𝒸𝓇𝒾𝓅𝓉", "𝔻𝕠𝕦𝕓𝕝𝕖",
        "₵ɄⱤⱤɆ₦₵Ɏ", "卂丂丨卂几", "𝘐𝘵𝘢𝘭𝘪𝘤", "𝙼𝚘𝚗𝚘", "𝗕𝗼𝗹𝗱"};
    /** Математические буквы: код заглавной A, строчной a, цифры 0 (0 — нет); исключения — буквы из «Буквоподобных символов». */
    private static final int[][] MATH = {
        null, null,
        {0x1D504, 0x1D51E, 0}, {0x1D56C, 0x1D586, 0}, {0x1D4D0, 0x1D4EA, 0}, {0x1D49C, 0x1D4B6, 0}, {0x1D538, 0x1D552, 0x1D7D8},
        null, null,
        {0x1D608, 0x1D622, 0}, {0x1D670, 0x1D68A, 0x1D7F6}, {0x1D5D4, 0x1D5EE, 0x1D7EC}};
    private static final String[] MATH_EX = {null, null, "CℭHℌIℑRℜZℨ", null, null, "BℬEℰFℱHℋIℐLℒMℳRℛe𝑒g𝑔o𝑜", "CℂHℍNℕPℙQℚRℝZℤ",
        null, null, null, null, null};
    private static final String[] MAP_FONT = {null, null, null, null, null, null, null,
        "₳฿₵ĐɆ₣₲ⱧłJ₭Ⱡ₥₦Ø₱QⱤ₴₮ɄV₩ӾɎⱫ", "卂乃匚ᗪ乇千Ꮆ卄丨ﾌҜㄥ爪几ㄖ卩Ɋ尺丂ㄒㄩᐯ山乂ㄚ乙", null, null, null};

    public static String fontLabel(int font) {
        return font <= 0 || font >= FONTS.length ? I18nHolder.normal() : FONT_LABELS[font];
    }

    /** Переводит текст в шрифт; буквы, которых в шрифте нет, остаются как есть (по одному символу на символ). */
    public static String applyFont(String text, int font) {
        if (font <= 0 || font >= FONTS.length) return text;
        if (font == 1) return toSmallCaps(text);
        StringBuilder out = new StringBuilder();
        int[] mapped = MAP_FONT[font] == null ? null : MAP_FONT[font].codePoints().toArray();
        int[] ex = MATH_EX[font] == null ? null : MATH_EX[font].codePoints().toArray();
        text.codePoints().forEach(c -> {
            if (mapped != null) {
                int lc = Character.toLowerCase(c);
                out.appendCodePoint(lc >= 'a' && lc <= 'z' ? mapped[lc - 'a'] : c);
                return;
            }
            if (ex != null) for (int i = 0; i + 1 < ex.length; i += 2) if (ex[i] == c) { out.appendCodePoint(ex[i + 1]); return; }
            int[] m = MATH[font];
            if (c >= 'A' && c <= 'Z') out.appendCodePoint(m[0] + c - 'A');
            else if (c >= 'a' && c <= 'z') out.appendCodePoint(m[1] + c - 'a');
            else if (m[2] != 0 && c >= '0' && c <= '9') out.appendCodePoint(m[2] + c - '0');
            else out.appendCodePoint(c);
        });
        return out.toString();
    }

    /** Подпись «Обычный шрифт» на языке игры (HexCore не зависит от интерфейса — берём через HexUi). */
    private static final class I18nHolder {
        static String normal() { return HexUi.tr("font.normal"); }
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
    /** Формат, общий для всех символов (для команд, где формат задаётся на весь текст). */
    public static int commonBits(int[] mask, int fallback) {
        if (mask == null || mask.length == 0) return fallback;
        int b = 0xF;
        for (int m : mask) b &= m;
        return b;
    }

    // --- /sponsor editprefix ---
    private static String mmColorTag(List<String> stops) {
        if (stops.size() == 1) return "<#" + stops.get(0) + ">";
        StringBuilder sb = new StringBuilder("<gradient");
        for (String s : stops) sb.append(":#").append(s);
        return sb.append('>').toString();
    }

    /** /sponsor editprefix <gradient:#A:#B>префикс<#ник>: формат префикса на ник не переходит. */
    public static String editPrefixCommand(String text, List<String> stops, int bits, String nickHex) {
        StringBuilder sb = new StringBuilder(COMMANDS[SPONSOR_EDIT]).append(mmColorTag(stops));
        for (int i = 0; i < 4; i++) if ((bits & (1 << i)) != 0) sb.append('<').append(MM_TAGS[i]).append('>');
        text.codePoints().forEach(c -> sb.append(mmEscape(new String(Character.toChars(c)))));
        for (int i = 3; i >= 0; i--) if ((bits & (1 << i)) != 0) sb.append("</").append(MM_TAGS[i]).append('>');
        return sb.append("<#").append(nickHex).append('>').toString();
    }

    // --- Цвета /colors (обычные игроки): текст делится на равные части, у каждой свой &-код ---
    public static int legacyIndex(String hex) {
        int best = 15;
        long bd = Long.MAX_VALUE;
        int c = valid(hex) ? rgb(hex) : 0xFFFFFF;
        for (int i = 0; i < 16; i++) {
            int l = rgb(SiteData.LEGACY_HEX[i]);
            long dr = ((c >> 16) & 255) - ((l >> 16) & 255), dg = ((c >> 8) & 255) - ((l >> 8) & 255), db = (c & 255) - (l & 255);
            long d = dr * dr + dg * dg + db * db;
            if (d < bd) { bd = d; best = i; }
        }
        return best;
    }

    public static String nearestLegacyHex(String hex) {
        return SiteData.LEGACY_HEX[legacyIndex(hex)];
    }

    public static char legacyCode(String hex) {
        return SiteData.LEGACY_CODES.charAt(legacyIndex(hex));
    }

    private static List<String> classicParts(String text, int n) {
        return n <= 1 ? List.of(text) : segments(text, n + 1);
    }

    /** &c&lТек&6&lст — как на сайте в режиме «цвета /colors». */
    public static String classicBody(String text, List<String> stops, int bits) {
        String fmt = formatCodes(bits);
        List<String> parts = classicParts(text, stops.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i).isEmpty()) continue;
            sb.append('&').append(legacyCode(stops.get(i))).append(fmt).append(parts.get(i));
        }
        return sb.toString();
    }

    public static List<StyledGlyph> classicGlyphs(String text, List<String> stops, int[] mask, int fallbackBits) {
        List<StyledGlyph> out = new ArrayList<>();
        List<String> parts = classicParts(text, stops.size());
        int idx = 0;
        for (int k = 0; k < parts.size(); k++) {
            int rgb = rgb(nearestLegacyHex(stops.get(k)));
            int[] cps = parts.get(k).codePoints().toArray();
            for (int cp : cps) {
                int bits = mask != null && idx < mask.length ? mask[idx] : fallbackBits;
                out.add(new StyledGlyph(new String(Character.toChars(cp)), rgb, bits));
                idx++;
            }
        }
        return out;
    }

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
    public record Parsed(int command, String text, List<String> stops, int[] mask, int[] colors, String nickHex, String prefix) {}

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
            if (i == SPONSOR_EDIT) continue;   // editprefix — MiniMessage, разбирается как другой формат
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
            return new Parsed(3, "", stops, new int[0], new int[0], tags.get(tags.size() - 1), "");
        }
        // Теги AgeMagic — {#rrggbb…}; без них это один из других форматов.
        if (!s.contains("{#")) {
            int other = detectOther(s);
            return other < 0 ? null : parseOther(s, other);
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
        return finish(command, text.toString(), stops, mask, colors, null, "");
    }

    // ------------------------------------------------------------ другие форматы

    private static final String[] BB_TAGS = {"B", "I", "U", "S"};
    private static final String[] MM_TAGS = {"b", "i", "u", "st"};

    private static String codes(int bits, char sign) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) if ((bits & (1 << i)) != 0) sb.append(sign).append("lonm".charAt(i));
        return sb.toString();
    }

    /** Цвет в синтаксисе формата (для «буквенных» форматов, где цвет ставится перед каждым символом). */
    private static String colorCode(int format, int rgb) {
        String h = hex(rgb);
        return switch (format) {
            case NICKNAME -> "&#" + h;
            case CHAT -> "<#" + h + ">";
            case LEGACY, CONSOLE -> {
                char sign = format == CONSOLE ? '§' : '&';
                StringBuilder sb = new StringBuilder().append(sign).append('x');
                for (char c : h.toCharArray()) sb.append(sign).append(c);
                yield sb.toString();
            }
            default -> "";
        };
    }

    private static String mmEscape(String ch) {
        return ch.replace("\\", "\\\\").replace("<", "\\<");
    }

    /** Открыть/закрыть теги формата при смене формата между символами (BBCode, MiniMessage). */
    private static void switchTags(StringBuilder sb, int from, int to, boolean bb) {
        if (from == to) return;
        for (int i = 3; i >= 0; i--) if ((from & (1 << i)) != 0) sb.append(bb ? "[/" + BB_TAGS[i] + "]" : "</" + MM_TAGS[i] + ">");
        for (int i = 0; i < 4; i++) if ((to & (1 << i)) != 0) sb.append(bb ? "[" + BB_TAGS[i] + "]" : "<" + MM_TAGS[i] + ">");
    }

    /** Текст в одном из других RGB-форматов (без команды — её добавляет вызывающий). */
    public static String formatText(int format, String text, List<String> stops, int[] mask, int[] colors, int fallback, String template) {
        List<StyledGlyph> glyphs = styled(text, stops, mask, colors, fallback);
        int uniform = uniformBits(mask, fallback);
        StringBuilder sb = new StringBuilder();

        if (format == MINIMESSAGE && uniform >= 0 && !hasOverrides(colors) && stops.size() >= 2) {
            // Родной градиент MiniMessage — короче, чем цвет на каждую букву.
            sb.append("<gradient");
            for (String st : stops) sb.append(":#").append(st);
            sb.append('>');
            switchTags(sb, 0, uniform, false);
            sb.append(mmEscape(text));
            switchTags(sb, uniform, 0, false);
            return sb.append("</gradient>").toString();
        }

        if (format == MINIMESSAGE || format == BBCODE) {
            boolean bb = format == BBCODE;
            int open = 0;
            for (StyledGlyph g : glyphs) {
                switchTags(sb, open, g.bits(), bb);
                open = g.bits();
                boolean plainSpace = g.ch().equals(" ") && (g.bits() & (UNDERLINE | STRIKE)) == 0;
                if (plainSpace) sb.append(' ');
                else if (bb) sb.append("[COLOR=#").append(hex(g.rgb())).append(']').append(g.ch()).append("[/COLOR]");
                else sb.append("<#").append(hex(g.rgb())).append('>').append(mmEscape(g.ch()));
            }
            switchTags(sb, open, 0, bb);
            return sb.toString();
        }

        if (format == BIRDFLOP) {
            String tpl = template == null || template.isEmpty() ? DEFAULT_BIRDFLOP : template;
            for (StyledGlyph g : glyphs) {
                String h = hex(g.rgb());
                String part = tpl;
                for (int i = 0; i < 6; i++) part = part.replace("$" + (i + 1), String.valueOf(h.charAt(i)));
                part = part.replace("$f", codes(g.bits(), '&')).replace("$c", g.ch());
                sb.append(part);
            }
            return sb.toString();
        }

        // Nickname, Chat, Legacy, Console: цвет и коды перед каждым символом.
        char sign = format == CONSOLE ? '§' : '&';
        int prevBits = -1;
        for (StyledGlyph g : glyphs) {
            if (g.ch().equals(" ") && g.bits() == prevBits && (g.bits() & (UNDERLINE | STRIKE)) == 0) {
                sb.append(' ');
                continue;
            }
            sb.append(colorCode(format, g.rgb())).append(codes(g.bits(), sign)).append(g.ch());
            prevBits = g.bits();
        }
        return sb.toString();
    }

    private static final java.util.regex.Pattern OTHER_TOKEN = java.util.regex.Pattern.compile(
            "(?i)&#([0-9a-f]{6})"                                  // 1  Nickname
            + "|<#([0-9a-f]{6})>"                                  // 2  Chat / MiniMessage
            + "|[&§]x((?:[&§][0-9a-f]){6})"                        // 3  Legacy / Console
            + "|\\[color=#([0-9a-f]{6})\\]"                          // 4  BBCode
            + "|<color:#([0-9a-f]{6})>"                            // 5  MiniMessage
            + "|<gradient((?::#[0-9a-f]{6})+)(?::[-0-9.]+)?>"      // 6  MiniMessage gradient
            + "|(</gradient>|\\[/color\\]|</color>|</#[0-9a-f]{6}>)" // 7  закрытие цвета
            + "|<(/?)(b|bold|i|italic|u|underlined|st|strikethrough)>" // 8, 9  MiniMessage формат
            + "|\\[(/?)([bius])\\]"                                  // 10, 11  BBCode формат
            + "|[&§]([0-9a-fk-or])");                              // 12  коды & и §

    private static int formatBit(String tag) {
        return switch (tag.toLowerCase(Locale.ROOT)) {
            case "b", "bold" -> BOLD;
            case "i", "italic" -> ITALIC;
            case "u", "underlined" -> UNDERLINE;
            case "st", "strikethrough", "s" -> STRIKE;
            default -> 0;
        };
    }

    /** Определяет, в каком из других форматов записан текст, или -1. */
    private static int detectOther(String s) {
        String l = s.toLowerCase(Locale.ROOT);
        if (l.contains("<gradient:") || l.contains("<color:#") || l.contains("</") || l.contains("\\<")) return MINIMESSAGE;
        if (l.contains("[color=#")) return BBCODE;
        if (l.matches("(?s).*§x(§[0-9a-f]){6}.*")) return CONSOLE;
        if (l.matches("(?s).*&x(&[0-9a-f]){6}.*")) return LEGACY;
        if (l.matches("(?s).*<#[0-9a-f]{6}>.*")) return CHAT;
        if (l.matches("(?s).*&#[0-9a-f]{6}.*")) return NICKNAME;
        return -1;
    }

    /** Разбор других форматов. Команда перед текстом (например «/nick ») возвращается в prefix. */
    private static Parsed parseOther(String s, int format) {
        java.util.regex.Matcher m = OTHER_TOKEN.matcher(s);
        String prefix = "";
        if (s.startsWith("/") && m.find()) {
            prefix = s.substring(0, m.start());
            s = s.substring(m.start());
            m = OTHER_TOKEN.matcher(s);
        }
        // В «буквенных» форматах новый цвет сбрасывает жирность и т. п., в MiniMessage и BBCode — нет.
        boolean colorResets = format != MINIMESSAGE && format != BBCODE;
        StringBuilder text = new StringBuilder();
        List<Integer> mask = new ArrayList<>(), colors = new ArrayList<>();
        List<String> stops = new ArrayList<>();
        boolean gradient = false;
        int solid = -1, bits = 0, tags = 0, pos = 0;
        while (true) {
            boolean found = m.find();
            int end = found ? m.start() : s.length();
            String chunk = s.substring(pos, end);
            if (format == MINIMESSAGE) chunk = chunk.replace("\\<", "<").replace("\\\\", "\\");
            for (int cp : chunk.codePoints().toArray()) {
                text.appendCodePoint(cp);
                mask.add(bits);
                colors.add(gradient ? -1 : solid);
            }
            if (!found) break;
            pos = m.end();
            String hex = m.group(1) != null ? m.group(1) : m.group(2) != null ? m.group(2)
                    : m.group(3) != null ? m.group(3).replaceAll("[&§]", "") : m.group(4) != null ? m.group(4) : m.group(5);
            if (hex != null) {
                tags++;
                solid = rgb(hex.toUpperCase(Locale.ROOT));
                gradient = false;
                if (colorResets) bits = 0;
            } else if (m.group(6) != null) {
                tags++;
                stops.clear();
                for (String st : m.group(6).split(":#")) if (!st.isEmpty()) stops.add(st.toUpperCase(Locale.ROOT));
                gradient = true;
            } else if (m.group(7) != null) {
                if (m.group(7).equalsIgnoreCase("</gradient>")) gradient = false;
                else solid = -1;
            } else if (m.group(9) != null || m.group(11) != null) {
                boolean close = !(m.group(9) != null ? m.group(8) : m.group(10)).isEmpty();
                int bit = formatBit(m.group(9) != null ? m.group(9) : m.group(11));
                bits = close ? bits & ~bit : bits | bit;
            } else if (m.group(12) != null) {
                char c = Character.toLowerCase(m.group(12).charAt(0));
                int idx = "lonm".indexOf(c);
                if (idx >= 0) bits |= 1 << idx;
                else if (c == 'r') { bits = 0; solid = -1; }
            }
        }
        if (tags == 0 || text.length() == 0) return null;
        return finish(format, text.toString(), stops, mask, colors, null, prefix);
    }

    /** Буквы с цветами (-1 — без цвета) и форматом, например из названия предмета. */
    public static Parsed fromGlyphs(int format, String text, List<Integer> mask, List<Integer> colors) {
        return finish(format, text, new ArrayList<>(), mask, colors, null, "");
    }

    // ------------------------------------------------------------ обмен пресетами

    /**
     * Код пресета для чата: ✦Имя[B04DFF,FF8FE0] (короче и понятнее без мода).
     * Старый формат FSTWEAK{Имя|B04DFF,FF8FE0} тоже распознаётся.
     */
    private static final String HEX_LIST = "([0-9A-Fa-f]{6}(?:\\s?,\\s?[0-9A-Fa-f]{6}){0,5})";
    /**
     * Метка — ✦ или значок из ресурспака сервера (некоторые серверы подменяют ✦ своим символом из
     * области U+E000–U+F8FF); после метки и внутри скобок допускаются пробелы.
     */
    private static final String MARK = "(?:✦|[\\uE000-\\uF8FF])\\s?";
    public static final java.util.regex.Pattern PRESET_CODE = java.util.regex.Pattern.compile(
            "FSTWEAK\\{([^{}|]{1,24})\\|" + HEX_LIST + "\\}|" + MARK + "([^\\[\\]✦{}|\\n\\uE000-\\uF8FF]{1,24}?)\\s?\\[\\s?" + HEX_LIST + "\\s?\\]");

    public static String presetCode(String name, String[] colors) {
        String n = name.replaceAll("[\\[\\]✦{}|]", "").strip();
        if (n.length() > 24) n = n.substring(0, 24).strip();
        return "✦" + (n.isEmpty() ? "Preset" : n) + "[" + String.join(",", colors) + "]";
    }

    /** Пресет из найденного кода (любого из двух форматов). */
    public static Preset presetOf(java.util.regex.Matcher m) {
        String name = m.group(1) != null ? m.group(1) : m.group(3);
        String colors = m.group(2) != null ? m.group(2) : m.group(4);
        return new Preset(name.strip(), colors.replaceAll("\\s", "").toUpperCase(Locale.ROOT).split(","));
    }

    /** Разбирает «Имя|HEX,HEX» (тело кода пресета, команда /fstweak preset). Null — если формат неверный. */
    public static Preset parsePresetBody(String body) {
        java.util.regex.Matcher m = PRESET_CODE.matcher("FSTWEAK{" + body.strip() + "}");
        if (!m.matches()) return null;
        return presetOf(m);
    }

    /** Первый код пресета в тексте или null. */
    public static Preset findPreset(String text) {
        java.util.regex.Matcher m = PRESET_CODE.matcher(text == null ? "" : text);
        return m.find() ? presetOf(m) : null;
    }

    /** Общий хвост разбора: если градиента нет — крайние цвета становятся его точками, цвета букв остаются как есть. */
    private static Parsed finish(int format, String text, List<String> stops, List<Integer> mask, List<Integer> colors,
                                 String nickHex, String prefix) {
        int[] cm = colors.stream().mapToInt(Integer::intValue).toArray();
        if (stops.size() < 2) {
            int first = -1, last = -1;
            for (int c : cm) if (c >= 0) { if (first < 0) first = c; last = c; }
            stops.clear();
            stops.add(hex(first < 0 ? 0xFFFFFF : first));
            stops.add(hex(last < 0 ? 0xFFFFFF : last));
        }
        while (stops.size() > 6) stops.remove(stops.size() - 2);
        return new Parsed(format, text, stops, mask.stream().mapToInt(Integer::intValue).toArray(), cm, nickHex, prefix);
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
