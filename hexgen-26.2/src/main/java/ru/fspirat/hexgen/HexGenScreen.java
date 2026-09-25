package ru.fspirat.hexgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/** Окно HEX-генератора внутри игры. */
public class HexGenScreen extends Screen {
    private static final int W = 340;
    private static final int H = 232;
    /** Максимальная длина текста в поле (сам лимит команды — HexCore.LIMITS). */
    private static final int TEXT_MAX = 256;

    // Вертикальная разметка окна (смещения от top).
    private static final int Y_PREVIEW = 14;
    private static final int Y_TEXT = 42;
    private static final int Y_FORMAT = 66;
    private static final int Y_COLORS_LABEL = 94;
    private static final int Y_COLORS = 105;
    private static final int Y_GRADIENT = 134;
    private static final int Y_COLOR_BUTTONS = 142;
    private static final int Y_RESULT = 168;
    private static final int Y_BOTTOM = 200;

    /** Курсор и второй край выделения в поле текста (в символах UTF-16) — переживают перестройку окна. */
    private static int cursor = HexState.S.text.length();
    private static int highlight = cursor;

    private final Screen parent;
    private int left;
    private int top;
    private SelectableEditBox textBox;
    private EditBox nickColorBox;
    private final Button[] formatButtons = new Button[4];
    private final List<Button> swatchButtons = new ArrayList<>();
    private String status = "";
    private int statusColor = 0xFF55FF55;
    private int statusTicks = 0;

    private static boolean sampleSet = false;

    public HexGenScreen(Screen parent) {
        super(Component.literal(HexUi.TITLE));
        this.parent = parent;
        if (!sampleSet) {
            sampleSet = true;
            if (HexState.S.text.isEmpty()) {
                HexState.S.setText(HexUi.tr("sample_text"));
                cursor = highlight = HexState.S.text.length();
            }
        }
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private static HexState s() {
        return HexState.S;
    }

    private boolean sponsor() {
        return "sponsor".equals(HexCore.COMMANDS[s().command]);
    }

    private int minStops() { return sponsor() ? 1 : 2; }
    private int maxStops() { return sponsor() ? 7 : 6; }

    private boolean stopsValid() {
        for (String c : s().stops) if (!HexCore.valid(c)) return false;
        return true;
    }

    private void normalizeStops() {
        List<String> st = s().stops;
        while (st.size() < minStops()) st.add(st.isEmpty() ? "FFFFFF" : st.get(st.size() - 1));
        while (st.size() > maxStops()) st.remove(st.size() - 2);
    }

    /** Цвет ника повторяет конечный цвет, пока его не поменяли вручную. */
    private void syncNick(boolean force) {
        String end = s().stops.get(s().stops.size() - 1);
        if (!HexCore.valid(end)) return;
        if (force || !end.equals(s().lastEndSynced)) {
            s().nickHex = end;
            s().lastEndSynced = end;
            if (nickColorBox != null) nickColorBox.setValue(end);
        }
    }

    /** Запомнить курсор и выделение, чтобы восстановить их после перестройки окна. */
    private void saveCursor() {
        if (textBox == null) return;
        cursor = textBox.getCursorPosition();
        highlight = cursor;
        String sel = textBox.getHighlighted();
        if (!sel.isEmpty()) {
            String v = textBox.getValue();
            highlight = v.startsWith(sel, cursor) ? cursor + sel.length() : cursor - sel.length();
        }
    }

    private void rebuild() {
        saveCursor();
        this.clearWidgets();
        this.init();
    }

    /** Изменение с точкой отмены: запомнить состояние, изменить, перестроить окно. */
    private void change(Runnable r) {
        HexState.push();
        r.run();
        rebuild();
    }

    private void flash(String msg) {
        flash(msg, 0xFF55FF55);
    }

    private void flash(String msg, int color) {
        status = msg;
        statusColor = color;
        statusTicks = 70;
    }

    private void open(Screen screen) {
        saveCursor();
        this.minecraft.gui.setScreen(screen);
    }

    @Override
    protected void init() {
        normalizeStops();
        if (sponsor()) syncNick(s().lastEndSynced == null);
        left = (this.width - W) / 2;
        top = Math.max(8, (this.height - H) / 2);
        int y1 = top + Y_TEXT, y2 = top + Y_FORMAT, y3 = top + Y_COLORS, y4 = top + Y_COLOR_BUTTONS, y6 = top + Y_BOTTOM;
        swatchButtons.clear();
        java.util.Arrays.fill(formatButtons, null);

        // --- Заголовок: отмена/повтор и настройки ---
        this.addRenderableWidget(Button.builder(Component.literal("↶"), b -> undo())
                .bounds(left, top - 2, 16, 13).tooltip(HexUi.tip(HexUi.tr("undo"))).build());
        this.addRenderableWidget(Button.builder(Component.literal("↷"), b -> redo())
                .bounds(left + 18, top - 2, 16, 13).tooltip(HexUi.tip(HexUi.tr("redo"))).build());
        this.addRenderableWidget(Button.builder(Component.literal("⚙"), b -> open(new SettingsScreen(this)))
                .bounds(left + W - 16, top - 2, 16, 13).tooltip(HexUi.tip(HexUi.tr("settings"))).build());

        // --- Строка 1: текст (или ник) + команда ---
        if (!sponsor()) {
            textBox = new SelectableEditBox(this.font, left, y1, 214, 18, Component.literal(HexUi.tr("text")));
            textBox.setMaxLength(TEXT_MAX);
            textBox.setValue(s().text);
            textBox.setResponder(v -> {
                if (v.equals(s().text)) return;
                HexState.pushTyping();
                s().setText(v);
                refreshFormatButtons();
            });
            int len = s().text.length();
            textBox.setCursorPosition(Math.max(0, Math.min(cursor, len)));
            textBox.setHighlightPos(Math.max(0, Math.min(highlight, len)));
            textBox.setTooltip(HexUi.tip(HexUi.tr("text.hint")));
            this.addRenderableWidget(textBox);
            nickColorBox = null;
        } else {
            textBox = null;
            EditBox nick = new EditBox(this.font, left, y1, 150, 18, Component.literal(HexUi.tr("nick")));
            nick.setMaxLength(16);
            nick.setValue(s().nickName);
            nick.setResponder(v -> s().nickName = v);
            nick.setTooltip(HexUi.tip(HexUi.tr("nick.hint")));
            this.addRenderableWidget(nick);

            nickColorBox = new EditBox(this.font, left + 156, y1, 58, 18, Component.literal(HexUi.tr("nick_color")));
            nickColorBox.setMaxLength(7);
            nickColorBox.setValue(s().nickHex);
            nickColorBox.setResponder(v -> s().nickHex = HexCore.clean(v));
            nickColorBox.setTooltip(HexUi.tip(HexUi.tr("nick_color.hint")));
            this.addRenderableWidget(nickColorBox);
            addSwatchButton(left + 156, y1 + 20, 58, () -> s().nickHex, v -> s().nickHex = v);
        }
        this.addRenderableWidget(Button.builder(Component.literal(HexUi.commandLabel(s().command)), b -> change(() -> {
            s().command = (s().command + 1) % HexCore.COMMANDS.length;
            normalizeStops();
            if (sponsor()) syncNick(true);
        })).bounds(left + 220, y1 - 1, 120, 20).tooltip(HexUi.tip(HexUi.tr("command.hint"))).build());

        // --- Строка 2: шрифт, форматирование, символы, цвет части ---
        if (!sponsor()) {
            this.addRenderableWidget(Button.builder(Component.literal(s().smallCaps ? "ꜱᴍᴀʟʟ ᴄᴀᴘꜱ" : HexUi.tr("font.normal")),
                    b -> change(() -> s().smallCaps = !s().smallCaps)).bounds(left, y2, 96, 20).build());

            String[] tips = {HexUi.tr("format.bold"), HexUi.tr("format.italic"), HexUi.tr("format.underline"), HexUi.tr("format.strike")};
            for (int i = 0; i < 4; i++) {
                final int bit = 1 << i;
                formatButtons[i] = Button.builder(Component.empty(), b -> toggleFormat(bit))
                        .bounds(left + 100 + i * 22, y2, 20, 20)
                        .tooltip(HexUi.tip(tips[i] + "\n" + HexUi.tr("format.hint")))
                        .build();
                this.addRenderableWidget(formatButtons[i]);
            }
            refreshFormatButtons();

            this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("symbols")),
                    b -> open(new SymbolsScreen(this, this::insertSymbol)))
                    .bounds(left + 190, y2, 72, 20).tooltip(HexUi.tip(HexUi.tr("symbols.hint"))).build());

            this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("part_color")), b -> openPartColor())
                    .bounds(left + 266, y2, 74, 20)
                    .tooltip(HexUi.tip(HexUi.tr("part_color.hint"))).build());
        }

        // --- Строка 3: цвета (поле HEX + полоска-кнопка палитры под ним) ---
        List<String> stops = s().stops;
        int stride = colorStride(), boxW = stride - 4;
        for (int i = 0; i < stops.size(); i++) {
            final int idx = i;
            EditBox hex = new EditBox(this.font, left + i * stride, y3, boxW, 16, Component.literal(HexUi.tr("color_n", (i + 1))));
            hex.setMaxLength(7);
            hex.setValue(stops.get(i));
            hex.setResponder(v -> {
                String c = HexCore.clean(v);
                if (c.equals(s().stops.get(idx))) return;
                HexState.pushTyping();
                s().stops.set(idx, c);
                if (sponsor() && idx == s().stops.size() - 1) syncNick(false);
            });
            this.addRenderableWidget(hex);
            addSwatchButton(left + i * stride, y3 + 18, boxW, () -> s().stops.get(idx), v -> {
                HexState.push();
                s().stops.set(idx, v);
                if (sponsor() && idx == s().stops.size() - 1) syncNick(false);
            });
        }

        // --- Строка 4: кнопки цветов ---
        Button add = Button.builder(Component.literal(HexUi.tr("add_color")), b -> change(() -> {
            List<String> st = s().stops;
            if (st.size() >= maxStops()) return;
            if (st.size() == 1) st.add(st.get(0));
            else {
                String a = st.get(st.size() - 2), c = st.get(st.size() - 1);
                String mid = HexCore.valid(a) && HexCore.valid(c) ? HexCore.hex(HexCore.mix(HexCore.rgb(a), HexCore.rgb(c), 0.5)) : "FFFFFF";
                st.add(st.size() - 1, mid);
            }
        })).bounds(left, y4, 50, 20).build();
        add.active = stops.size() < maxStops();
        this.addRenderableWidget(add);

        Button remove = Button.builder(Component.literal(HexUi.tr("remove_color")), b -> change(() -> {
            List<String> st = s().stops;
            if (st.size() <= minStops()) return;
            st.remove(Math.max(0, st.size() - 2));
            if (sponsor()) syncNick(false);
        })).bounds(left + 53, y4, 50, 20).build();
        remove.active = stops.size() > minStops();
        this.addRenderableWidget(remove);

        this.addRenderableWidget(Button.builder(Component.literal("⇄"), b -> change(() -> {
            Collections.reverse(s().stops);
            if (sponsor()) syncNick(false);
        })).bounds(left + 106, y4, 20, 20).tooltip(HexUi.tip(HexUi.tr("reverse"))).build());

        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("random")), b -> change(() -> {
            s().stops.clear();
            s().stops.addAll(HexCore.randomGradient());
            normalizeStops();
            if (sponsor()) syncNick(false);
        })).bounds(left + 129, y4, 66, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("presets")),
                b -> open(new PresetsScreen(this, this::applyPreset)))
                .bounds(left + 198, y4, 70, 20).tooltip(HexUi.tip(HexUi.tr("presets.hint"))).build());

        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("save_preset")), b -> {
            if (!stopsValid()) { flash(HexUi.tr("fix_colors"), 0xFFFF5555); return; }
            String[] colors = s().stops.toArray(new String[0]);
            open(new NamePresetScreen(this, colors, name -> flash(HexUi.tr("saved_as", name))));
        }).bounds(left + 271, y4, 69, 20).tooltip(HexUi.tip(HexUi.tr("save_preset.hint"))).build());

        // --- Нижние кнопки ---
        int bw = 64, step = 69;
        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("copy")), b -> {
            String out = output();
            if (out.isEmpty()) { flash(HexUi.tr("fix_colors"), 0xFFFF5555); return; }
            this.minecraft.keyboardHandler.setClipboard(out);
            HexConfig.addHistory(out);
            int limit = HexCore.LIMITS[s().command], len = HexCore.measuredLength(out, s().command);
            if (len > limit) flash(HexUi.tr("copied_over", overLimitMessage(len, limit)), 0xFFFFD24D);
            else flash(HexUi.tr("copied"));
        }).bounds(left, y6, bw, 20).build());

        Button run = Button.builder(Component.literal(HexUi.tr("run")), b -> {
            String out = output();
            if (out.isEmpty() || !out.startsWith("/")) { flash(HexUi.tr("pick_command"), 0xFFFF5555); return; }
            int limit = HexCore.LIMITS[s().command], len = HexCore.measuredLength(out, s().command);
            if (len > limit) { flash(capitalize(overLimitMessage(len, limit)), 0xFFFF5555); return; }
            if (this.minecraft.player != null) {
                this.minecraft.player.connection.sendCommand(out.substring(1));
                HexConfig.addHistory(out);
                flash(HexUi.tr("sent"));
            }
        }).bounds(left + step, y6, bw, 20).tooltip(HexUi.tip(HexUi.tr("run.hint"))).build();
        run.active = !HexCore.COMMANDS[s().command].isEmpty();
        this.addRenderableWidget(run);

        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("import")), b -> {
            if (applyCommand(this.minecraft.keyboardHandler.getClipboard())) flash(HexUi.tr("imported"));
            else flash(HexUi.tr("import_failed"), 0xFFFF5555);
        }).bounds(left + step * 2, y6, bw, 20)
                .tooltip(HexUi.tip(HexUi.tr("import.hint"))).build());

        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("history")),
                b -> open(new HistoryScreen(this, this::applyCommand)))
                .bounds(left + step * 3, y6, bw, 20).tooltip(HexUi.tip(HexUi.tr("history.hint"))).build());

        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("back")), b -> this.onClose())
                .bounds(left + step * 4, y6, bw, 20).build());
    }

    // ---------------------------------------------------------------- действия

    private void undo() {
        if (HexState.undo()) { cursor = highlight = s().text.length(); textBox = null; rebuild(); }
        else flash(HexUi.tr("nothing_undo"), 0xFFA0A0A0);
    }

    private void redo() {
        if (HexState.redo()) { cursor = highlight = s().text.length(); textBox = null; rebuild(); }
        else flash(HexUi.tr("nothing_redo"), 0xFFA0A0A0);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.hasControlDown()) {
            int k = event.key();
            if (k == GLFW.GLFW_KEY_Z && !event.hasShiftDown()) { undo(); return true; }
            if (k == GLFW.GLFW_KEY_Y || (k == GLFW.GLFW_KEY_Z && event.hasShiftDown())) { redo(); return true; }
        }
        return super.keyPressed(event);
    }

    private void applyPreset(String[] colors) {
        HexState.push();
        s().stops.clear();
        s().stops.addAll(List.of(colors));
        normalizeStops();
        if (sponsor()) syncNick(false);
    }

    /** Загружает команду (импорт или история). False — если разобрать не удалось. */
    private boolean applyCommand(String raw) {
        HexCore.Parsed p = HexCore.parse(raw);
        if (p == null) return false;
        HexState.push();
        HexState st = s();
        st.command = p.command();
        st.stops = new ArrayList<>(p.stops());
        if (p.command() == 3) {
            st.nickHex = p.nickHex();
            st.lastEndSynced = st.stops.get(st.stops.size() - 1);
        } else {
            st.text = p.text();
            st.mask = p.mask();
            st.colors = p.colors();
            st.smallCaps = false;
            st.defaultBits = HexCore.uniformBits(p.mask(), 0) >= 0 ? HexCore.uniformBits(p.mask(), 0) : 0;
            cursor = highlight = st.text.length();
        }
        textBox = null;
        rebuild();
        return true;
    }

    private String overLimitMessage(int len, int limit) {
        boolean perChar = HexCore.uniformBits(s().mask, s().defaultBits) < 0 || HexCore.hasOverrides(s().colors);
        return HexUi.tr("over_limit", len, limit) + (perChar ? HexUi.tr("over_limit.per_char") : "");
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
        HexState.push();
        HexState st = s();
        int[] sel = selection();
        int a = 0, b = st.mask.length;
        if (sel != null) {
            a = Math.max(0, sel[0]);
            b = Math.min(st.mask.length, sel[1]);
        } else {
            st.defaultBits ^= bit;
        }
        if (b > a) {
            boolean all = true;
            for (int i = a; i < b; i++) if ((st.mask[i] & bit) == 0) all = false;
            for (int i = a; i < b; i++) st.mask[i] = all ? st.mask[i] & ~bit : st.mask[i] | bit;
            if (sel == null) st.defaultBits = all ? st.defaultBits & ~bit : st.defaultBits | bit;
        }
        refreshFormatButtons();
    }

    /** Зелёный — формат у всего текста, жёлтый — у части, серый — нет. */
    private void refreshFormatButtons() {
        String[] letters = {"L", "O", "N", "M"};
        int[] mask = s().mask;
        for (int i = 0; i < 4; i++) {
            if (formatButtons[i] == null) continue;
            int bit = 1 << i, count = 0;
            for (int m : mask) if ((m & bit) != 0) count++;
            boolean on = mask.length == 0 ? (s().defaultBits & bit) != 0 : count == mask.length;
            int color = on ? 0x55FF55 : count > 0 ? 0xFFD24D : 0xAAAAAA;
            Style st = Style.EMPTY.withColor(color)
                    .withBold(i == 0).withItalic(i == 1).withUnderlined(i == 2).withStrikethrough(i == 3);
            formatButtons[i].setMessage(Component.literal(letters[i]).withStyle(st));
        }
    }

    /** Окно «Цвет части» для выделенного фрагмента. */
    private void openPartColor() {
        int[] sel = selection();
        if (sel == null || sel[1] <= sel[0]) {
            flash(HexUi.tr("select_first"), 0xFFFFD24D);
            return;
        }
        int a = sel[0], b = Math.min(sel[1], s().colors.length);
        List<HexCore.StyledGlyph> gl = HexCore.styled(shownText(), s().stops, s().mask, s().colors, s().defaultBits);
        int from = gl.isEmpty() ? 0xFFFFFF : gl.get(Math.min(a, gl.size() - 1)).rgb();
        int to = gl.isEmpty() ? 0xFFFFFF : gl.get(Math.min(b - 1, gl.size() - 1)).rgb();
        open(new PartColorScreen(this, HexCore.hex(from), HexCore.hex(to), (start, end) -> {
            HexState.push();
            int[] colors = s().colors;
            int n = b - a;
            for (int i = a; i < b && i < colors.length; i++) {
                if (start == null) colors[i] = -1;
                else if (end == null || n <= 1) colors[i] = start;
                else colors[i] = HexCore.mix(start, end, (i - a) / (double) (n - 1));
            }
        }));
    }

    /** Вставка символа из окна «Символы» в позицию курсора. */
    private void insertSymbol(String sym) {
        String text = s().text;
        int c = Math.max(0, Math.min(cursor, text.length()));
        String v = text.substring(0, c) + sym + text.substring(c);
        if (v.length() > TEXT_MAX) return;
        HexState.push();
        s().setText(v);
        cursor = highlight = c + sym.length();
    }

    static String currentText() {
        return HexState.S.text;
    }

    // ---------------------------------------------------------------- вывод

    /** Шаг между полями цветов: все поля всегда помещаются в ширину окна. */
    private int colorStride() {
        return (W + 4) / maxStops();
    }

    /** Цветная полоска, по нажатию открывающая палитру. */
    private void addSwatchButton(int x, int y, int w, Supplier<String> get, Consumer<String> set) {
        Button b = Button.builder(Component.empty(), btn -> open(new ColorPickerScreen(this, get.get(), set)))
                .bounds(x, y, w, 8)
                .tooltip(HexUi.tip(HexUi.tr("open_palette")))
                .build();
        swatchButtons.add(b);
        this.addRenderableWidget(b);
    }

    private String shownText() {
        return s().smallCaps ? HexCore.toSmallCaps(s().text) : s().text;
    }

    /** Готовая команда или пустая строка, если цвета некорректны. */
    private String output() {
        if (!stopsValid()) return "";
        HexState st = s();
        if (sponsor()) {
            if (!HexCore.valid(st.nickHex)) return "";
            return HexCore.sponsorCommand(st.stops, st.nickHex);
        }
        return HexCore.gradientCommand(HexCore.COMMANDS[st.command], shownText(), st.stops, st.mask, st.colors, st.defaultBits);
    }

    /** Предпросмотр, отрисованный настоящим шрифтом Minecraft. */
    private MutableComponent preview() {
        if (!stopsValid()) return Component.literal(HexUi.tr("invalid_hex")).withStyle(Style.EMPTY.withColor(0xFF5555));
        HexState st = s();
        if (sponsor()) {
            MutableComponent root = Component.empty();
            String sc = HexCore.toSmallCaps(HexCore.SPONSOR_PREFIX);
            List<Integer> cols = HexCore.sponsorColors(st.stops);
            int[] cps = sc.codePoints().toArray();
            for (int i = 0; i < cps.length; i++) {
                root.append(Component.literal(new String(Character.toChars(cps[i]))).withStyle(Style.EMPTY.withColor(cols.get(i))));
            }
            root.append(Component.literal(" "));
            int nc = HexCore.valid(st.nickHex) ? HexCore.rgb(st.nickHex) : 0xFFFFFF;
            root.append(Component.literal(st.nickName.isEmpty() ? "nickname" : st.nickName).withStyle(Style.EMPTY.withColor(nc)));
            return root;
        }
        List<HexCore.StyledGlyph> glyphs = HexCore.styled(shownText(), st.stops, st.mask, st.colors, st.defaultBits);
        if (HexConfig.animatePreview) glyphs = HexCore.animate(glyphs, st.stops, st.colors, HexCore.animationPhase());
        return HexUi.styled(glyphs);
    }

    /** Предмет для предпросмотра: из руки, а если рука пустая — бирка. */
    private ItemStack previewItem() {
        if (this.minecraft.player != null) {
            ItemStack held = this.minecraft.player.getMainHandItem();
            if (!held.isEmpty()) return held;
        }
        return new ItemStack(Items.NAME_TAG);
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
        HexUi.drawPanel(g, left, top, W, H);
        super.extractRenderState(g, mouseX, mouseY, delta);

        MutableComponent title = HexUi.gradientTitle(HexUi.TITLE);
        g.text(this.font, title, (this.width - this.font.width(title)) / 2, top + 1, 0xFFFFFFFF, true);

        // Предпросмотр в стиле подсказки предмета; для /itemname и /itemlore — с иконкой предмета.
        int px = left, py = top + Y_PREVIEW, pw = W, ph = 22;
        HexUi.drawTooltipBox(g, px, py, pw, ph);
        boolean item = s().command <= 1;
        int textLeft = px;
        if (item) {
            g.item(previewItem(), px + 4, py + 3);
            textLeft = px + 22;
        }
        MutableComponent pv = preview();
        int area = px + pw - textLeft, tw = this.font.width(pv);
        g.text(this.font, pv, textLeft + Math.max(4, (area - tw) / 2), py + 7, 0xFFFFFFFF, true);

        String colorsLabel = (sponsor() ? HexUi.tr("colors_sponsor") : HexUi.tr("colors")) + HexUi.tr("colors.hint");
        g.text(this.font, Component.literal(fit(colorsLabel, W)), left, top + Y_COLORS_LABEL, 0xFFA0A0A0, false);

        // Полоски-кнопки палитры: цвет поля (или тёмно-красный, если HEX неверный).
        HexState st = s();
        int n = sponsor() ? 1 : 0;
        for (int i = 0; i < swatchButtons.size(); i++) {
            Button b = swatchButtons.get(i);
            String c = sponsor() ? (i == 0 ? st.nickHex : st.stops.get(i - n)) : st.stops.get(i);
            int col = HexCore.valid(c) ? HexCore.rgb(c) : 0x550000;
            HexUi.drawSwatch(g, b.getX(), b.getY(), b.getWidth(), b.getHeight(), col, b.isHovered(), false);
        }

        // Весь градиент одной полосой.
        HexUi.drawGradient(g, left, top + Y_GRADIENT, W, 4, st.stops);

        // Результат и счётчик длины.
        int ry = top + Y_RESULT;
        String out = output();
        g.text(this.font, Component.literal(HexUi.tr("result")), left, ry, 0xFFA0A0A0, false);
        if (!out.isEmpty()) {
            int limit = HexCore.LIMITS[st.command], len = HexCore.measuredLength(out, st.command);
            int cc = len > limit ? 0xFFFF5555 : len > limit * 0.85 ? 0xFFFFD24D : 0xFF55FF55;
            String counter = st.command <= 1 ? HexUi.tr("text_length", len, limit) : len + " / " + limit;
            g.text(this.font, Component.literal(counter), left + W - this.font.width(counter), ry, cc, false);
        }
        g.fill(left, ry + 10, left + W, ry + 26, 0xC0000000);
        String shown = out.isEmpty() ? HexUi.tr("hex_help") : out;
        g.text(this.font, Component.literal(fit(shown, W - 8)), left + 4, ry + 14, out.isEmpty() ? 0xFFFF5555 : 0xFFFFFFFF, false);

        if (!status.isEmpty()) {
            Component sc = Component.literal(status);
            g.text(this.font, sc, (this.width - this.font.width(sc)) / 2, top + Y_BOTTOM + 23, statusColor, true);
        }
    }

    @Override
    public void onClose() {
        saveCursor();
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
