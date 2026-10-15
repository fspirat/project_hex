package ru.fspirat.hexgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Проверки логики &k (obfuscated) в генераторе — без Minecraft. Запуск: tests/logic/run.sh */
public final class HexCoreTest {
    private static int failed = 0, passed = 0;

    private static void check(boolean ok, String what) {
        if (ok) passed++;
        else { failed++; System.out.println("FAIL: " + what); }
    }

    private static void eq(Object a, Object b, String what) {
        check(java.util.Objects.equals(a, b), what + "\n   ожидалось: " + b + "\n   получено:  " + a);
    }

    private static final List<String> STOPS = List.of("B04DFF", "FF8FE0");
    private static final int K = HexCore.OBFUSCATED;

    private static int[] fill(int n, int v) { int[] a = new int[n]; Arrays.fill(a, v); return a; }

    private static HexState typed(String... steps) {
        HexState s = new HexState();
        for (String v : steps) s.setText(v);
        return s;
    }

    public static void main(String[] args) {
        // --- биты: старые значения не изменились, добавился 16
        eq(HexCore.BOLD + "," + HexCore.ITALIC + "," + HexCore.UNDERLINE + "," + HexCore.STRIKE + "," + K, "1,2,4,8,16", "значения битов");
        eq(HexCore.formatCodes(15), "&l&o&n&m", "старые четыре кода");
        eq(HexCore.formatCodes(31), "&l&o&n&m&k", "все пять кодов");
        eq(HexCore.commonBits(new int[]{31, 31}, 0), 31, "commonBits учитывает &k");
        eq(HexCore.commonBits(new int[]{17, 1}, 0), 1, "commonBits: &k только у части");

        // --- разбор кодов в поле
        HexCore.Inline a = HexCore.inline("&k12345");
        eq(a.visible(), "12345", "&k12345: видимый текст");
        eq(Arrays.toString(a.bits()), Arrays.toString(fill(5, K)), "&k12345: все пять символов obfuscated");
        eq(HexCore.inline("&K12345").visible(), "12345", "&K12345: заглавная K");
        eq(Arrays.toString(HexCore.inline("&K12345").bits()), Arrays.toString(fill(5, K)), "&K12345: биты");
        eq(Arrays.toString(HexCore.inline("§k12").bits()), Arrays.toString(fill(2, K)), "§k тоже код");
        HexCore.Inline price = HexCore.inline("Цена: &k12345&r монет");
        eq(price.visible(), "Цена: 12345 монет", "&r: видимый текст");
        int[] want = new int[17];
        for (int i = 6; i < 11; i++) want[i] = K;
        eq(Arrays.toString(price.bits()), Arrays.toString(want), "&r: эффект только у 12345");
        eq(HexCore.inline("abc&").visible(), "abc&", "незавершённый код остаётся текстом");
        eq(HexCore.inline("&").visible(), "&", "одиночный &");
        eq(HexCore.inline("a&xb").visible(), "a&xb", "&x — не код поля");
        HexCore.Inline emoji = HexCore.inline("&k😀я");
        eq(emoji.visible(), "😀я", "Unicode вне BMP и кириллица");
        eq(emoji.length(), 2, "длина в символах, не в UTF-16");
        HexCore.Inline pos = HexCore.inline("ab&kcd");
        eq(pos.visibleIndex(4), 2, "курсор после &k → 2 видимых символа");
        eq(pos.visibleIndex(3), 2, "курсор внутри кода → 2");
        eq(pos.visibleIndex(6), 4, "курсор в конце");
        eq(pos.rawOffset(2), 4, "видимый символ 2 начинается после кода");
        eq(pos.rawOffset(4), 6, "конец текста");

        // --- набор по одной букве: маска остаётся по видимым символам
        HexState s = typed("&", "&k", "&k1", "&k12", "&k123", "&k1234", "&k12345");
        eq(s.text, "&k12345", "исходный текст с кодом сохраняется в модели");
        eq(s.mask.length, 5, "маска — по видимым символам");
        eq(s.colors.length, 5, "свои цвета — по видимым символам");
        eq(Arrays.toString(s.effectiveMask()), Arrays.toString(fill(5, K)), "итоговая маска: все obfuscated");
        HexState del = typed("&k12345", "&12345");
        eq(del.visibleText(), "&12345", "удалили k — & снова текст");
        eq(Arrays.toString(del.effectiveMask()), Arrays.toString(fill(6, 0)), "без кода эффекта нет");
        HexState paste = typed("Цена: ", "Цена: &k12345&r монет");
        eq(paste.visibleText(), "Цена: 12345 монет", "вставка строки с кодами");
        eq(Arrays.toString(paste.effectiveMask()), Arrays.toString(want), "вставка: эффект только у 12345");

        // --- генерация AgeMagic
        HexState g = typed("&k12345");
        String out = HexCore.gradientCommand("/itemname ", g.visibleText(), STOPS, g.effectiveMask(), g.colors, 0);
        eq(out, "/itemname {#B04DFF>}&k12345{#FF8FE0<}", "&k12345 → короткий градиент с &k, исходные цифры");
        HexState bold = typed("&k12345");
        Arrays.fill(bold.mask, HexCore.BOLD);
        String outB = HexCore.gradientCommand("", bold.visibleText(), STOPS, bold.effectiveMask(), bold.colors, 0);
        eq(outB, "{#B04DFF>}&l&k12345{#FF8FE0<}", "&k + жирный");
        String outP = HexCore.gradientCommand("/itemname ", paste.visibleText(), STOPS, paste.effectiveMask(), paste.colors, 0);
        check(outP.contains("}&k1") && outP.contains("}&k5") && !outP.contains("&kЦ") && !outP.contains("&kм"),
                "«Цена: &k12345&r монет»: &k только перед цифрами → " + outP);
        HexCore.Parsed back = HexCore.parse(outP);
        check(back != null, "повторный импорт сгенерированной строки");
        if (back != null) {
            eq(back.text(), "Цена: 12345 монет", "импорт: текст");
            eq(Arrays.toString(back.mask()), Arrays.toString(want), "импорт: эффект только у 12345");
            eq(back.command(), 0, "импорт: команда /itemname");
        }
        HexCore.Parsed back2 = HexCore.parse(out);
        check(back2 != null && Arrays.equals(back2.mask(), fill(5, K)) && back2.text().equals("12345"), "импорт короткого градиента с &k");
        HexCore.Parsed manual = HexCore.parse("{#B04DFF>}Цена: &k12345&r монет{#FF8FE0<}");
        check(manual != null && Arrays.equals(manual.mask(), want), "импорт вручную написанной строки с &k…&r");
        HexCore.Parsed sec = HexCore.parse("{#FF0000}§k12");
        check(sec != null && Arrays.equals(sec.mask(), fill(2, K)), "импорт §k");

        // --- старые стили без изменений
        HexState old = typed("AB");
        Arrays.fill(old.mask, HexCore.BOLD | HexCore.STRIKE);
        eq(HexCore.gradientCommand("", old.visibleText(), STOPS, old.effectiveMask(), old.colors, 0), "{#B04DFF>}&l&mAB{#FF8FE0<}", "жирный+зачёркнутый как раньше");

        // --- другие форматы
        String mm = HexCore.formatText(HexCore.MINIMESSAGE, "12345", STOPS, fill(5, K), fill(5, -1), 0, null);
        eq(mm, "<gradient:#B04DFF:#FF8FE0><obf>12345</obf></gradient>", "MiniMessage: тег obf с закрытием");
        String mmPart = HexCore.formatText(HexCore.MINIMESSAGE, paste.visibleText(), STOPS, paste.effectiveMask(), paste.colors, 0, null);
        check(count(mmPart, "<obf>") == 1 && count(mmPart, "</obf>") == 1 && mmPart.indexOf("<obf>") < mmPart.indexOf("</obf>"),
                "MiniMessage по символам: один <obf>…</obf> → " + mmPart);
        HexCore.Parsed mmBack = HexCore.parse(mmPart);
        check(mmBack != null && Arrays.equals(mmBack.mask(), want), "MiniMessage: импорт <obf>");
        HexCore.Parsed mmLong = HexCore.parse("<gradient:#B04DFF:#FF8FE0><obfuscated>12</obfuscated></gradient>");
        check(mmLong != null && Arrays.equals(mmLong.mask(), fill(2, K)), "MiniMessage: импорт <obfuscated>");
        String edit = HexCore.editPrefixCommand("12", STOPS, K | HexCore.BOLD, "FFFFFF");
        eq(edit, "/sponsor editprefix <gradient:#B04DFF:#FF8FE0><b><obf>12</obf></b><#FFFFFF>", "editprefix: <obf> закрыт до цвета ника");

        String bb = HexCore.formatText(HexCore.BBCODE, "12", STOPS, fill(2, K | HexCore.BOLD), fill(2, -1), 0, null);
        check(!bb.contains("obf") && !bb.contains("&k") && bb.contains("1[/COLOR]") && bb.startsWith("[B]") && bb.endsWith("[/B]"),
                "BBCode: эффекта нет, текст и жирный сохранены → " + bb);
        check(!HexCore.supportsObfuscated(HexCore.BBCODE) && HexCore.supportsObfuscated(HexCore.LEGACY), "supportsObfuscated");

        String legacy = HexCore.formatText(HexCore.LEGACY, "12", STOPS, fill(2, K), fill(2, -1), 0, null);
        check(legacy.startsWith("&x&B&0&4&D&F&F&k1") && count(legacy, "&k") == 2, "Legacy: &k после каждого цвета → " + legacy);
        HexCore.Parsed lb = HexCore.parse(legacy);
        check(lb != null && Arrays.equals(lb.mask(), fill(2, K)), "Legacy: импорт");
        String console = HexCore.formatText(HexCore.CONSOLE, "12", STOPS, fill(2, K), fill(2, -1), 0, null);
        check(count(console, "§k") == 2, "Console: §k → " + console);
        HexCore.Parsed cb = HexCore.parse(console);
        check(cb != null && Arrays.equals(cb.mask(), fill(2, K)), "Console: импорт §k");
        String nick = HexCore.formatText(HexCore.NICKNAME, "12", STOPS, fill(2, K), fill(2, -1), 0, null);
        check(nick.startsWith("&#B04DFF&k1"), "Nickname: &#…&k → " + nick);
        String bird = HexCore.formatText(HexCore.BIRDFLOP, "12", STOPS, fill(2, K), fill(2, -1), 0, null);
        check(bird.startsWith("&#B04DFF&k1"), "BirdFlop: $f даёт &k → " + bird);
        String classic = HexCore.classicBody("12", List.of("FF5555"), K);
        eq(classic, "&c&k12", "/colors: &k после цвета");

        // --- материализация кодов (кнопка «Обфускация») и загрузка текста с похожими на коды кусками
        HexState mat = typed("Цена: &k12345&r монет");
        check(mat.materializeInline(), "materializeInline: коды были");
        eq(mat.text, "Цена: 12345 монет", "materializeInline: коды убраны из поля");
        eq(Arrays.toString(mat.mask), Arrays.toString(want), "materializeInline: эффект перенесён в маску");
        check(!mat.materializeInline(), "повторно — нечего переносить");
        HexState ld = new HexState();
        ld.load("a&kb", new int[]{1, 2, 4, 8}, new int[]{-1, -1, -1, 0xFF0000});
        eq(ld.visibleText(), "ab", "load: код в тексте");
        eq(Arrays.toString(ld.mask), "[1, 8]", "load: маска перенесена на видимые символы");
        eq(Arrays.toString(ld.colors), "[-1, 16711680]", "load: цвета перенесены");

        // --- предпросмотр: градиент считается по видимым символам
        List<HexCore.StyledGlyph> gl = HexCore.styled(g.visibleText(), STOPS, g.effectiveMask(), g.colors, 0);
        eq(gl.size(), 5, "предпросмотр: пять символов, код не виден");
        eq(gl.get(0).rgb(), 0xB04DFF, "первый видимый символ — первый цвет градиента");
        eq(gl.get(4).rgb(), 0xFF8FE0, "последний — последний цвет");
        check(gl.stream().allMatch(x -> (x.bits() & K) != 0), "все с obfuscated");
        List<HexCore.StyledGlyph> an = HexCore.animate(gl, STOPS, g.colors, 0.3);
        check(an.stream().allMatch(x -> (x.bits() & K) != 0), "анимация градиента не снимает obfuscated");

        // --- маска при правке посередине
        HexState mid = typed("&k12345");
        mid.setText("&k12X345");
        eq(mid.mask.length, 6, "вставка внутри — длина маски");
        mid.setText("&k12345");
        eq(mid.mask.length, 5, "удаление — длина маски");

        System.out.println("HexCore: " + passed + " ok, " + failed + " fail");
        if (failed > 0) System.exit(1);
    }

    private static int count(String s, String sub) {
        int n = 0, i = 0;
        while ((i = s.indexOf(sub, i)) >= 0) { n++; i += sub.length(); }
        return n;
    }
}
