package ru.fspirat.hexgen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Окно HEX-генератора внутри игры. */
public class HexGenScreen extends Screen {
    // Состояние хранится статически, чтобы не сбрасываться между открытиями окна.
    private static String text = "Пример текста";
    private static int command = 0;
    private static boolean smallCaps = false;
    /** Формат каждого символа текста (биты HexCore.BOLD…STRIKE). */
    private static int[] mask = HexCore.spliceMask("", text, null, 0);
    /** Формат для нового текста, когда поле пустое. */
    private static int defaultBits = 0;
    /** Позиция курсора в тексте — сюда вставляются символы из окна «Символы». */
    private static int cursor = text.length();
    private static final List<String> STOPS = new ArrayList<>(List.of("B04DFF", "FF8FE0"));
    private static int preset = 0;
    private static String nickName = "nickname";
    private static String nickHex = "FF8FE0";
    private static String lastEndSynced = null;

    private static final int W = 340;
    private static final int H = 228;

    // Вертикальная разметка окна (смещения от top).
    private static final int Y_PREVIEW = 14;
    private static final int Y_TEXT = 42;
    private static final int Y_FORMAT = 66;
    private static final int Y_COLORS_LABEL = 94;
    private static final int Y_COLORS = 105;
    private static final int Y_COLOR_BUTTONS = 138;
    private static final int Y_RESULT = 164;
    private static final int Y_BOTTOM = 196;
    private static final String[] TITLE_STOPS = {"B04DFF", "FF8FE0"};

    private final Screen parent;
    private int left;
    private int top;
    private EditBox textBox;
    private EditBox nickColorBox;
    private final Button[] formatButtons = new Button[4];
    private final List<Button> swatchButtons = new ArrayList<>();
    private String status = "";
    private int statusTicks = 0;

    public HexGenScreen(Screen parent) {
        super(Component.literal("FSHEX GENERATOR"));
        this.parent = parent;
    }

    private boolean sponsor() {
        return "sponsor".equals(HexCore.COMMANDS[command]);
    }

    private int minStops() { return sponsor() ? 1 : 2; }
    private int maxStops() { return sponsor() ? 7 : 6; }

    private boolean stopsValid() {
        for (String s : STOPS) if (!HexCore.valid(s)) return false;
        return true;
    }

    private void normalizeStops() {
        while (STOPS.size() < minStops()) STOPS.add(STOPS.isEmpty() ? "FFFFFF" : STOPS.get(STOPS.size() - 1));
        while (STOPS.size() > maxStops()) STOPS.remove(STOPS.size() - 2);
    }

    /** Цвет ника повторяет конечный цвет, пока его не поменяли вручную. */
    private void syncNick(boolean force) {
        String end = STOPS.get(STOPS.size() - 1);
        if (!HexCore.valid(end)) return;
        if (force || !end.equals(lastEndSynced)) {
            nickHex = end;
            lastEndSynced = end;
            if (nickColorBox != null) nickColorBox.setValue(end);
        }
    }

    private void rebuild() {
        if (textBox != null) cursor = textBox.getCursorPosition();
        this.clearWidgets();
        this.init();
    }

    private void flash(String msg) {
        status = msg;
        statusTicks = 60;
    }

    @Override
    protected void init() {
        normalizeStops();
        if (sponsor()) syncNick(lastEndSynced == null);
        left = (this.width - W) / 2;
        top = Math.max(4, (this.height - H) / 2);
        int y1 = top + Y_TEXT, y2 = top + Y_FORMAT, y3 = top + Y_COLORS, y4 = top + Y_COLOR_BUTTONS, y6 = top + Y_BOTTOM;
        swatchButtons.clear();

        // --- Строка 1: текст (или ник) + команда ---
        if (!sponsor()) {
            textBox = new EditBox(this.font, left, y1, 214, 18, Component.literal("Текст"));
            textBox.setMaxLength(256);
            textBox.setValue(text);
            textBox.setResponder(v -> {
                mask = HexCore.spliceMask(text, v, mask, defaultBits);
                text = v;
            });
            textBox.setCursorPosition(Math.min(cursor, text.length()));
            textBox.setHighlightPos(textBox.getCursorPosition());
            textBox.setTooltip(Tooltip.create(Component.literal("Выделите часть текста и нажмите L / O / N / M, чтобы отформатировать только её")));
            this.addRenderableWidget(textBox);
            nickColorBox = null;
        } else {
            textBox = null;
            EditBox nick = new EditBox(this.font, left, y1, 150, 18, Component.literal("Ник"));
            nick.setMaxLength(16);
            nick.setValue(nickName);
            nick.setResponder(v -> nickName = v);
            nick.setTooltip(Tooltip.create(Component.literal("Ник для предпросмотра (в команду не попадает)")));
            this.addRenderableWidget(nick);

            nickColorBox = new EditBox(this.font, left + 156, y1, 58, 18, Component.literal("Цвет ника"));
            nickColorBox.setMaxLength(7);
            nickColorBox.setValue(nickHex);
            nickColorBox.setResponder(v -> nickHex = HexCore.clean(v));
            nickColorBox.setTooltip(Tooltip.create(Component.literal("Цвет ника (= конечный цвет, можно поменять)")));
            this.addRenderableWidget(nickColorBox);
            addSwatchButton(left + 156, y1 + 20, 58, () -> nickHex, v -> nickHex = v);
        }
        this.addRenderableWidget(Button.builder(Component.literal(HexCore.COMMAND_LABELS[command]), b -> {
            command = (command + 1) % HexCore.COMMANDS.length;
            normalizeStops();
            if (sponsor()) syncNick(true);
            rebuild();
        }).bounds(left + 220, y1 - 1, 120, 20).tooltip(Tooltip.create(Component.literal("Команда — нажмите, чтобы сменить"))).build());

        // --- Строка 2: шрифт, форматирование, символы ---
        if (!sponsor()) {
            this.addRenderableWidget(Button.builder(Component.literal(smallCaps ? "ꜱᴍᴀʟʟ ᴄᴀᴘꜱ" : "Обычный шрифт"), b -> {
                smallCaps = !smallCaps;
                rebuild();
            }).bounds(left, y2, 96, 20).build());

            String[] tips = {"&l Жирный", "&o Курсив", "&n Подчёркнутый", "&m Зачёркнутый"};
            for (int i = 0; i < 4; i++) {
                final int bit = 1 << i;
                formatButtons[i] = Button.builder(Component.empty(), b -> toggleFormat(bit))
                        .bounds(left + 100 + i * 22, y2, 20, 20)
                        .tooltip(Tooltip.create(Component.literal(tips[i] + "\nБез выделения — весь текст, с выделением — только выделенная часть")))
                        .build();
                this.addRenderableWidget(formatButtons[i]);
            }
            refreshFormatButtons();

            this.addRenderableWidget(Button.builder(Component.literal("✦ Символы"), b -> {
                cursor = textBox.getCursorPosition();
                this.minecraft.gui.setScreen(new SymbolsScreen(this, this::insertSymbol));
            }).bounds(left + 190, y2, W - 190, 20)
                    .tooltip(Tooltip.create(Component.literal("Вставить символ в текст (в позицию курсора)")))
                    .build());
        }

        // --- Строка 3: цвета (поле HEX + полоска-кнопка палитры под ним) ---
        int stride = colorStride(), boxW = stride - 4;
        for (int i = 0; i < STOPS.size(); i++) {
            final int idx = i;
            EditBox hex = new EditBox(this.font, left + i * stride, y3, boxW, 16, Component.literal("Цвет " + (i + 1)));
            hex.setMaxLength(7);
            hex.setValue(STOPS.get(i));
            hex.setResponder(v -> {
                STOPS.set(idx, HexCore.clean(v));
                if (sponsor() && idx == STOPS.size() - 1) syncNick(false);
            });
            this.addRenderableWidget(hex);
            addSwatchButton(left + i * stride, y3 + 18, boxW, () -> STOPS.get(idx), v -> {
                STOPS.set(idx, v);
                if (sponsor() && idx == STOPS.size() - 1) syncNick(false);
            });
        }

        // --- Строка 4: кнопки цветов ---
        Button add = Button.builder(Component.literal("+ Цвет"), b -> {
            if (STOPS.size() >= maxStops()) return;
            if (STOPS.size() == 1) STOPS.add(STOPS.get(0));
            else {
                String a = STOPS.get(STOPS.size() - 2), c = STOPS.get(STOPS.size() - 1);
                String mid = HexCore.valid(a) && HexCore.valid(c) ? HexCore.hex(HexCore.mix(HexCore.rgb(a), HexCore.rgb(c), 0.5)) : "FFFFFF";
                STOPS.add(STOPS.size() - 1, mid);
            }
            rebuild();
        }).bounds(left, y4, 52, 20).build();
        add.active = STOPS.size() < maxStops();
        this.addRenderableWidget(add);

        Button remove = Button.builder(Component.literal("− Цвет"), b -> {
            if (STOPS.size() <= minStops()) return;
            STOPS.remove(STOPS.size() - 2 >= 0 ? STOPS.size() - 2 : 0);
            if (sponsor()) syncNick(false);
            rebuild();
        }).bounds(left + 55, y4, 52, 20).build();
        remove.active = STOPS.size() > minStops();
        this.addRenderableWidget(remove);

        this.addRenderableWidget(Button.builder(Component.literal("⇄"), b -> {
            java.util.Collections.reverse(STOPS);
            if (sponsor()) syncNick(false);
            rebuild();
        }).bounds(left + 110, y4, 22, 20).tooltip(Tooltip.create(Component.literal("Поменять порядок"))).build());

        this.addRenderableWidget(Button.builder(Component.literal("Случайный"), b -> {
            STOPS.clear();
            STOPS.addAll(HexCore.randomGradient());
            normalizeStops();
            if (sponsor()) syncNick(false);
            rebuild();
        }).bounds(left + 135, y4, 70, 20).build());

        HexCore.Preset p = HexCore.PRESETS.get(preset);
        this.addRenderableWidget(Button.builder(Component.literal("Пресет: " + p.name()), b -> {
            HexCore.Preset cur = HexCore.PRESETS.get(preset);
            STOPS.clear();
            STOPS.addAll(List.of(cur.colors()));
            preset = (preset + 1) % HexCore.PRESETS.size();
            normalizeStops();
            if (sponsor()) syncNick(false);
            rebuild();
        }).bounds(left + 208, y4, 132, 20).tooltip(Tooltip.create(Component.literal("Нажмите, чтобы применить пресет и перейти к следующему"))).build());

        // --- Нижние кнопки ---
        this.addRenderableWidget(Button.builder(Component.literal("Скопировать"), b -> {
            String out = output();
            if (out.isEmpty()) { flash("Сначала исправьте цвета"); return; }
            this.minecraft.keyboardHandler.setClipboard(out);
            flash("Скопировано в буфер обмена");
        }).bounds(left, y6, 110, 20).build());

        Button run = Button.builder(Component.literal("Выполнить"), b -> {
            String out = output();
            if (out.isEmpty() || !out.startsWith("/")) { flash("Выберите команду"); return; }
            if (out.length() > 256) { flash("Команда длиннее 256 символов"); return; }
            if (this.minecraft.player != null) {
                this.minecraft.player.connection.sendCommand(out.substring(1));
                flash("Команда отправлена");
            }
        }).bounds(left + 115, y6, 110, 20).tooltip(Tooltip.create(Component.literal("Сразу выполнить команду на сервере"))).build();
        run.active = !HexCore.COMMANDS[command].isEmpty();
        this.addRenderableWidget(run);

        this.addRenderableWidget(Button.builder(Component.literal("Назад"), b -> this.onClose())
                .bounds(left + 230, y6, 110, 20).build());
    }

    /** Выделение в поле текста в символах (code points): {начало, конец} или null. */
    private int[] selection() {
        if (textBox == null) return null;
        String sel = textBox.getHighlighted();
        if (sel.isEmpty()) return null;
        String v = textBox.getValue();
        int c = textBox.getCursorPosition();
        int start = v.startsWith(sel, c) ? c : c - sel.length();
        if (start < 0 || start + sel.length() > v.length()) return null;
        int a = v.codePointCount(0, start);
        return new int[]{a, a + sel.codePointCount(0, sel.length())};
    }

    /** Включает/выключает формат для выделения или для всего текста. */
    private void toggleFormat(int bit) {
        int[] sel = selection();
        int a = 0, b = mask.length;
        if (sel != null) {
            a = Math.max(0, sel[0]);
            b = Math.min(mask.length, sel[1]);
        } else {
            defaultBits ^= bit;
        }
        if (b > a) {
            boolean all = true;
            for (int i = a; i < b; i++) if ((mask[i] & bit) == 0) all = false;
            for (int i = a; i < b; i++) mask[i] = all ? mask[i] & ~bit : mask[i] | bit;
            if (sel == null) defaultBits = all ? defaultBits & ~bit : defaultBits | bit;
        }
        refreshFormatButtons();
    }

    /** Зелёный — формат у всего текста, жёлтый — у части, серый — нет. */
    private void refreshFormatButtons() {
        String[] letters = {"L", "O", "N", "M"};
        for (int i = 0; i < 4; i++) {
            if (formatButtons[i] == null) continue;
            int bit = 1 << i, count = 0;
            for (int m : mask) if ((m & bit) != 0) count++;
            boolean on = mask.length == 0 ? (defaultBits & bit) != 0 : count == mask.length;
            int color = on ? 0x55FF55 : count > 0 ? 0xFFD24D : 0xAAAAAA;
            Style st = Style.EMPTY.withColor(color)
                    .withBold(i == 0).withItalic(i == 1).withUnderlined(i == 2).withStrikethrough(i == 3);
            formatButtons[i].setMessage(Component.literal(letters[i]).withStyle(st));
        }
    }

    /** Вставка символа из окна «Символы» в позицию курсора. */
    private void insertSymbol(String sym) {
        int c = Math.max(0, Math.min(cursor, text.length()));
        String v = text.substring(0, c) + sym + text.substring(c);
        if (v.length() > 256) return;
        mask = HexCore.spliceMask(text, v, mask, defaultBits);
        text = v;
        cursor = c + sym.length();
    }

    static String currentText() {
        return text;
    }

    /** Шаг между полями цветов: все поля всегда помещаются в ширину окна. */
    private int colorStride() {
        return (W + 4) / maxStops();
    }

    /** Цветная полоска, по нажатию открывающая палитру. */
    private void addSwatchButton(int x, int y, int w, java.util.function.Supplier<String> get, java.util.function.Consumer<String> set) {
        Button b = Button.builder(Component.empty(), btn -> {
                    if (textBox != null) cursor = textBox.getCursorPosition();
                    this.minecraft.gui.setScreen(new ColorPickerScreen(this, get.get(), set));
                })
                .bounds(x, y, w, 8)
                .tooltip(Tooltip.create(Component.literal("Открыть палитру")))
                .build();
        swatchButtons.add(b);
        this.addRenderableWidget(b);
    }

    /** Тёмная подложка окна, чтобы интерфейс не сливался с миром. */
    static void drawPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        int x0 = x - 8, y0 = y - 6, x1 = x + w + 8, y1 = y + h + 6;
        g.fill(x0, y0, x1, y1, 0xD0101014);
        g.fill(x0, y0, x1, y0 + 1, 0xFF5A2A8A);
        g.fill(x0, y1 - 1, x1, y1, 0xFF5A2A8A);
        g.fill(x0, y0, x0 + 1, y1, 0xFF5A2A8A);
        g.fill(x1 - 1, y0, x1, y1, 0xFF5A2A8A);
    }

    /** Образец цвета с рамкой; белая рамка — при наведении или если цвет выбран. */
    static void drawSwatch(GuiGraphicsExtractor g, int x, int y, int w, int h, int rgb, boolean hovered, boolean selected) {
        int border = hovered ? 0xFFFFFFFF : selected ? 0xFFFFFF55 : 0xFF000000;
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF000000 | rgb);
    }

    /** Готовая команда или пустая строка, если цвета некорректны. */
    private String output() {
        if (!stopsValid()) return "";
        if (sponsor()) {
            if (!HexCore.valid(nickHex)) return "";
            return HexCore.sponsorCommand(STOPS, nickHex);
        }
        String t = smallCaps ? HexCore.toSmallCaps(text) : text;
        return HexCore.gradientCommand(HexCore.COMMANDS[command], t, STOPS, mask, defaultBits);
    }

    /** Предпросмотр, отрисованный настоящим шрифтом Minecraft. */
    private MutableComponent preview() {
        MutableComponent root = Component.empty();
        if (!stopsValid()) return Component.literal("Неверный HEX-цвет").withStyle(Style.EMPTY.withColor(0xFF5555));
        if (sponsor()) {
            String sc = HexCore.toSmallCaps(HexCore.SPONSOR_PREFIX);
            List<Integer> cols = HexCore.sponsorColors(STOPS);
            int[] cps = sc.codePoints().toArray();
            for (int i = 0; i < cps.length; i++) {
                root.append(Component.literal(new String(Character.toChars(cps[i]))).withStyle(Style.EMPTY.withColor(cols.get(i))));
            }
            root.append(Component.literal(" "));
            int nc = HexCore.valid(nickHex) ? HexCore.rgb(nickHex) : 0xFFFFFF;
            root.append(Component.literal(nickName.isEmpty() ? "nickname" : nickName).withStyle(Style.EMPTY.withColor(nc)));
            return root;
        }
        String t = smallCaps ? HexCore.toSmallCaps(text) : text;
        List<HexCore.Glyph> glyphs = HexCore.gradientGlyphs(t, STOPS);
        for (int i = 0; i < glyphs.size(); i++) {
            HexCore.Glyph g = glyphs.get(i);
            int bits = i < mask.length ? mask[i] : defaultBits;
            Style st = Style.EMPTY.withColor(g.rgb()).withBold((bits & HexCore.BOLD) != 0).withItalic((bits & HexCore.ITALIC) != 0)
                    .withUnderlined((bits & HexCore.UNDERLINE) != 0).withStrikethrough((bits & HexCore.STRIKE) != 0);
            root.append(Component.literal(g.ch()).withStyle(st));
        }
        return root;
    }

    private String fit(String s, int maxW) {
        if (this.font.width(s) <= maxW) return s;
        String e = "…";
        int end = s.length();
        while (end > 0 && this.font.width(s.substring(0, end) + e) > maxW) end--;
        return s.substring(0, end) + e;
    }

    @Override
    public void tick() {
        super.tick();
        if (statusTicks > 0 && --statusTicks == 0) status = "";
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        drawPanel(g, left, top, W, H);
        super.extractRenderState(g, mouseX, mouseY, delta);

        // Заголовок с градиентом
        MutableComponent title = Component.empty();
        for (HexCore.Glyph gl : HexCore.gradientGlyphs("FSHEX GENERATOR", List.of(TITLE_STOPS))) {
            title.append(Component.literal(gl.ch()).withStyle(Style.EMPTY.withColor(gl.rgb()).withBold(true)));
        }
        g.text(this.font, title, (this.width - this.font.width(title)) / 2, top + 2, 0xFFFFFFFF, true);

        // Окно предпросмотра в стиле подсказки предмета
        int px = left, py = top + Y_PREVIEW, pw = W, ph = 22;
        g.fill(px, py, px + pw, py + ph, 0xF0100010);
        g.fill(px + 1, py + 1, px + pw - 1, py + 2, 0x505000FF);
        g.fill(px + 1, py + ph - 2, px + pw - 1, py + ph - 1, 0x5028007F);
        g.fill(px + 1, py + 1, px + 2, py + ph - 1, 0x505000FF);
        g.fill(px + pw - 2, py + 1, px + pw - 1, py + ph - 1, 0x505000FF);
        MutableComponent pv = preview();
        int tw = this.font.width(pv);
        g.text(this.font, pv, px + Math.max(4, (pw - tw) / 2), py + 7, 0xFFFFFFFF, true);

        // Подпись над цветами
        String colorsLabel = (sponsor() ? "Цвета префикса (1–7)" : "Цвета (2–6)") + " — нажмите на полоску, чтобы открыть палитру";
        g.text(this.font, Component.literal(fit(colorsLabel, W)), left, top + Y_COLORS_LABEL, 0xFFA0A0A0, false);

        // Полоски-кнопки палитры: цвет поля (или тёмно-красный, если HEX неверный)
        int n = sponsor() ? 1 : 0;
        for (int i = 0; i < swatchButtons.size(); i++) {
            Button b = swatchButtons.get(i);
            String s = sponsor() ? (i == 0 ? nickHex : STOPS.get(i - n)) : STOPS.get(i);
            int col = HexCore.valid(s) ? HexCore.rgb(s) : 0x550000;
            drawSwatch(g, b.getX(), b.getY(), b.getWidth(), b.getHeight(), col, b.isHovered(), false);
        }

        // Результат
        int ry = top + Y_RESULT;
        g.text(this.font, Component.literal("Результат:"), left, ry, 0xFFA0A0A0, false);
        g.fill(left, ry + 10, left + W, ry + 26, 0xC0000000);
        String out = output();
        String shown = out.isEmpty() ? "Каждый цвет — 6 символов HEX, например FF8F5A" : out;
        g.text(this.font, Component.literal(fit(shown, W - 8)), left + 4, ry + 14, out.isEmpty() ? 0xFFFF5555 : 0xFFFFFFFF, false);

        if (!status.isEmpty()) {
            Component st = Component.literal(status);
            g.text(this.font, st, (this.width - this.font.width(st)) / 2, top + Y_BOTTOM + 22, 0xFF55FF55, true);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
