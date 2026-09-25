package ru.fspirat.hexgen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Всё, что редактируется в генераторе. Живёт между открытиями окна; копии нужны для Ctrl+Z. */
public final class HexState {
    public static HexState S = new HexState();

    public String text = "Пример текста";
    public int command = 0;
    public boolean smallCaps = false;
    public List<String> stops = new ArrayList<>(List.of("B04DFF", "FF8FE0"));
    /** Формат каждого символа (биты HexCore.BOLD…STRIKE). */
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
        c.smallCaps = smallCaps;
        c.stops = new ArrayList<>(stops);
        c.mask = mask.clone();
        c.colors = colors.clone();
        c.defaultBits = defaultBits;
        c.nickName = nickName;
        c.nickHex = nickHex;
        c.lastEndSynced = lastEndSynced;
        return c;
    }

    /** Меняет текст, сохраняя формат и свои цвета уцелевших символов. */
    public void setText(String v) {
        mask = HexCore.spliceMask(text, v, mask, defaultBits);
        colors = HexCore.spliceMask(text, v, colors, -1);
        text = v;
    }

    public boolean sameAs(HexState o) {
        return text.equals(o.text) && command == o.command && smallCaps == o.smallCaps && stops.equals(o.stops)
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
