package ru.fspirat.hexgen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Всё, что редактируется в генераторе. Живёт между открытиями окна; копии нужны для Ctrl+Z. */
public final class HexState {
    public static HexState S = new HexState();

    /**
     * Текст поля как его набрал игрок — вместе с кодами &k/&r (HexCore.inline). Пусто до первого открытия окна —
     * тогда подставляется пример на языке игры. Маска и свои цвета ниже — по видимым символам (без кодов).
     */
    public String text = "";
    public int command = 0;
    /** Шрифт текста: индекс в HexCore.FONTS (0 — обычный, 1 — Small Caps, дальше — стили из Unicode). */
    public int font = 0;
    /** Цвета /colors (обычные игроки): вместо HEX — классические &-коды, текст делится на равные части. */
    public boolean classic = false;
    public List<String> stops = new ArrayList<>(List.of("B04DFF", "FF8FE0"));
    /** Формат каждого видимого символа (биты HexCore.BOLD…OBFUSCATED), заданный кнопками и импортом. */
    public int[] mask = HexCore.spliceMask("", text, null, 0);
    /** Свой цвет символа (RGB) или -1, если символ берёт цвет из общего градиента. */
    public int[] colors = HexCore.spliceMask("", text, null, -1);
    /** Формат для нового текста, когда поле пустое. */
    public int defaultBits = 0;
    public String nickName = "nickname";
    public String nickHex = "FF8FE0";
    public String lastEndSynced = null;

    public HexState copy() {
        HexState c = new HexState();
        c.text = text;
        c.command = command;
        c.font = font;
        c.classic = classic;
        c.stops = new ArrayList<>(stops);
        c.mask = mask.clone();
        c.colors = colors.clone();
        c.defaultBits = defaultBits;
        c.nickName = nickName;
        c.nickHex = nickHex;
        c.lastEndSynced = lastEndSynced;
        return c;
    }

    /** Меняет текст, сохраняя формат и свои цвета уцелевших символов (сравниваются видимые символы, без кодов &k/&r). */
    public void setText(String v) {
        String oldVis = HexCore.visibleText(text), newVis = HexCore.visibleText(v);
        mask = HexCore.spliceMask(oldVis, newVis, mask, defaultBits);
        colors = HexCore.spliceMask(oldVis, newVis, colors, -1);
        text = v;
    }

    private String inlineKey = null;
    private HexCore.Inline inlineCache = null;

    /** Разбор текста поля на видимые символы и коды (кэш: текст меняется редко, а читается каждый кадр). */
    public HexCore.Inline inline() {
        if (inlineCache == null || !text.equals(inlineKey)) {
            inlineCache = HexCore.inline(text);
            inlineKey = text;
        }
        return inlineCache;
    }

    /** Видимый текст без кодов &k/&r. */
    public String visibleText() {
        return inline().visible();
    }

    /** Итоговый формат каждого видимого символа: маска | obfuscated от кодов &k в поле. */
    public int[] effectiveMask() {
        return HexCore.effectiveMask(mask, inline());
    }

    /**
     * Загружает текст с форматом и цветами по каждому символу (импорт, предмет в руке). Если в тексте попались
     * последовательности, похожие на коды (&k, &r), маска и цвета переносятся на видимые символы — длины совпадают.
     */
    public void load(String rawText, int[] cpMask, int[] cpColors) {
        HexCore.Inline in = HexCore.inline(rawText);
        int n = in.length();
        int[] m = new int[n], c = new int[n];
        for (int i = 0; i < n; i++) {
            int cp = rawText.codePointCount(0, in.rawAt()[i]);
            m[i] = cpMask != null && cp < cpMask.length ? cpMask[cp] : defaultBits;
            c[i] = cpColors != null && cp < cpColors.length ? cpColors[cp] : -1;
        }
        text = rawText;
        mask = m;
        colors = c;
    }

    /**
     * Переносит коды &k/&r из текста в маску (символы остаются «бегущими», коды из поля исчезают).
     * Нужно, чтобы кнопка «Обфускация» могла снять эффект с части текста. False — кодов не было.
     */
    public boolean materializeInline() {
        if (!HexCore.hasInlineCodes(text)) return false;
        int[] eff = effectiveMask();
        text = visibleText();
        mask = eff;
        return true;
    }

    public boolean sameAs(HexState o) {
        return text.equals(o.text) && command == o.command && font == o.font && classic == o.classic && stops.equals(o.stops)
                && java.util.Arrays.equals(mask, o.mask) && java.util.Arrays.equals(colors, o.colors)
                && defaultBits == o.defaultBits && nickName.equals(o.nickName) && nickHex.equals(o.nickHex);
    }

    // --- Отмена / повтор ---
    private static final int LIMIT = 60;
    private static final Deque<HexState> UNDO = new ArrayDeque<>();
    private static final Deque<HexState> REDO = new ArrayDeque<>();
    private static long lastTyping = 0;

    /** Запомнить состояние перед изменением. */
    public static void push() {
        if (!UNDO.isEmpty() && UNDO.peek().sameAs(S)) return;
        UNDO.push(S.copy());
        while (UNDO.size() > LIMIT) UNDO.removeLast();
        REDO.clear();
    }

    /** Для набора текста: одна точка отмены на серию нажатий. */
    public static void pushTyping() {
        long now = System.currentTimeMillis();
        if (now - lastTyping > 800) push();
        lastTyping = now;
    }

    public static boolean undo() {
        if (UNDO.isEmpty()) return false;
        REDO.push(S.copy());
        S = UNDO.pop();
        lastTyping = 0;
        return true;
    }

    public static boolean redo() {
        if (REDO.isEmpty()) return false;
        UNDO.push(S.copy());
        S = REDO.pop();
        lastTyping = 0;
        return true;
    }
}
