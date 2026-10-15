package ru.fspirat.hexgen.fslog;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * Просмотр накопленного чата внутри игры (/log). Показывается снимок сообщений на момент открытия;
 * на сайт он уходит только по кнопке «Выгрузить на сайт». Сообщения выводятся как текст с цветами и
 * оформлением — без кликов и подсказок из оригинала, поэтому ничего из них не выполняется.
 * Поиск только отбирает, что показать: на сайт по-прежнему уходит весь снимок.
 */
public final class LogViewScreen extends Screen {
    // Цвета сайта FSPIRAT — как в окнах FSTWEAK.
    private static final int C_SURFACE = 0xFF080B08, C_CARD = 0xFF0D120D, C_CARD_HI = 0xFF151C14, C_LINE = 0xFF273322,
            C_LINE_HI = 0xFF34452D, C_TEXT = 0xFFE8EEE6, C_MUTED = 0xFF7C8578, C_DIM = 0xFF5E665A, C_GREEN = 0xFF7FBF3A,
            C_GREEN_HI = 0xFF9BE052, C_GREEN_LT = 0xFFD4FF9A, C_RED = 0xFFFF6B6B, C_YELLOW = 0xFFFFD24D, C_LINK = 0xFF55FFFF;
    /** Цвет найденного текста (RGB для стиля). */
    private static final int FOUND_RGB = 0xFFD24D;
    private static final int LINE_H = 10;
    /** Сколько времени за кадр можно тратить на перенос строк (5000 длинных сообщений размечаются за несколько кадров). */
    private static final long LAYOUT_BUDGET_NS = 4_000_000L;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final int count;
    private LogUpload<ChatBuffer.Entry> upload;
    private long addedAtSnapshot;
    private long snapshotTime;

    // Разметка: строки снизу вверх (0 — последняя строка самого нового сообщения), строятся порциями.
    private record Line(FormattedCharSequence text, String time) {}
    private final List<Line> rev = new ArrayList<>();
    /** Какие сообщения снимка показываются (индексы): все или подходящие под поиск. */
    private int[] shown = new int[0];
    /** Следующее сообщение для разметки — индекс в {@link #shown}, идём от новых к старым. */
    private int nextEntry;
    private int wrapWidth = -1;
    /** Прокрутка: на сколько строк окно поднято от самых новых сообщений. */
    private int scroll;

    private int left, top, w, h, areaY, areaH, timeW;
    private Button uploadButton, refreshButton;
    private EditBox searchBox;
    private String query = "";
    private LogUpload.State shownState;
    private String flash = "";
    private int flashTicks;

    public LogViewScreen(int count) {
        super(Component.translatable("fslog.view.title"));
        this.count = count;
        takeSnapshot();
    }

    /** Снимок: последние count сообщений на этот момент. Новые сообщения в него уже не попадут. */
    private void takeSnapshot() {
        if (upload != null) upload.close();
        upload = new LogUpload<>(ChatBuffer.last(count), Uploader::upload, Minecraft.getInstance(), System::currentTimeMillis, LogUpload.GATE);
        int n = upload.snapshot().size();
        upload.onResultAfterClose(r -> LogCommand.announce(r, n));
        addedAtSnapshot = ChatBuffer.added();
        snapshotTime = System.currentTimeMillis();
        applyFilter();
    }

    /** Отобрать сообщения под поиск (без учёта регистра) и начать разметку заново. */
    private void applyFilter() {
        List<ChatBuffer.Entry> snap = upload.snapshot();
        int[] idx = new int[snap.size()];
        int k = 0;
        for (int i = 0; i < snap.size(); i++) {
            if (query.isEmpty() || indexOf(plain(snap.get(i)), query, 0) >= 0) idx[k++] = i;
        }
        shown = Arrays.copyOf(idx, k);
        rev.clear();
        nextEntry = shown.length - 1;
        scroll = 0;
    }

    private static String plain(ChatBuffer.Entry e) {
        StringBuilder sb = new StringBuilder();
        for (ChatBuffer.Seg s : e.segs()) sb.append(s.text());
        return sb.toString();
    }

    /** Поиск подстроки без учёта регистра (посимвольно — длина строки не меняется, в отличие от toLowerCase). */
    private static int indexOf(String text, String q, int from) {
        for (int i = from, max = text.length() - q.length(); i <= max; i++) {
            if (text.regionMatches(true, i, q, 0, q.length())) return i;
        }
        return -1;
    }

    /** Выгрузка этого окна (для автотестов). */
    public LogUpload<ChatBuffer.Entry> upload() {
        return upload;
    }

    /** Все сообщения снимка уже разбиты на строки (для автотестов). */
    public boolean laidOut() {
        return nextEntry < 0;
    }

    /** Сколько сообщений сейчас показано с учётом поиска (для автотестов). */
    public int shownCount() {
        return shown.length;
    }

    @Override
    protected void init() {
        w = Math.min(this.width - 16, 520);
        left = (this.width - w) / 2;
        top = 8;
        h = this.height - 16;
        areaY = top + 26;
        areaH = Math.max(LINE_H * 3 + 6, h - 26 - 50);
        timeW = this.font.width("00:00:00 ");
        int ww = w - 12 - timeW - 6;
        if (ww != wrapWidth) {   // другой размер окна или GUI Scale — переразметить
            wrapWidth = ww;
            rev.clear();
            nextEntry = shown.length - 1;
        }

        // Поиск — справа в шапке; Ctrl+F переводит сюда.
        int sw = Math.min(170, w / 2);
        searchBox = new EditBox(this.font, left + w - sw, top + 10, sw, 14, Component.translatable("fslog.view.search"));
        searchBox.setMaxLength(100);
        searchBox.setValue(query);
        searchBox.setHint(Component.translatable("fslog.view.search.hint").withStyle(Style.EMPTY.withColor(C_DIM)));
        searchBox.setTooltip(Tooltip.create(Component.translatable("fslog.view.search.tip")));
        searchBox.setResponder(v -> {
            if (v.equals(query)) return;
            query = v;
            applyFilter();
        });
        this.addRenderableWidget(searchBox);

        int by = top + h - 20, gap = 4, closeW = 60, rest = w - closeW - gap;
        boolean failed = upload.state() == LogUpload.State.FAILED, done = upload.state() == LogUpload.State.DONE;
        int x = left, refreshW;
        if (!done) {
            int upW = Math.min(140, rest * 55 / 100);
            uploadButton = Button.builder(Component.translatable(failed ? "fslog.view.retry" : "fslog.view.upload"), b -> pressUpload())
                    .bounds(x, by, upW, 20).tooltip(Tooltip.create(Component.translatable("fslog.view.upload.hint"))).build();
            uploadButton.active = upload.canStart();
            this.addRenderableWidget(uploadButton);
            x += upW + gap;
            refreshW = Math.min(110, rest - upW - gap);
        } else {
            uploadButton = null;
            String url = upload.result().url();
            int linkW = Math.min(110, (rest - 2 * gap) * 35 / 100);
            this.addRenderableWidget(Button.builder(Component.translatable("fslog.view.open_link"), b -> openLink(url))
                    .bounds(x, by, linkW, 20).build());
            x += linkW + gap;
            this.addRenderableWidget(Button.builder(Component.translatable("fslog.view.copy_link"), b -> {
                this.minecraft.keyboardHandler.setClipboard(url);
                setFlash(I18n.get("fslog.view.copied"));
            }).bounds(x, by, linkW, 20).build());
            x += linkW + gap;
            refreshW = Math.min(110, rest - 2 * linkW - 2 * gap);
        }
        refreshButton = Button.builder(Component.literal(refreshLabel()), b -> {
            takeSnapshot();
            rebuild();
        }).bounds(x, by, refreshW, 20).tooltip(Tooltip.create(Component.translatable("fslog.view.refresh.hint"))).build();
        refreshButton.active = !upload.sending();
        this.addRenderableWidget(refreshButton);
        this.addRenderableWidget(Button.builder(Component.translatable("fslog.view.close"), b -> this.onClose())
                .bounds(left + w - closeW, by, closeW, 20).build());
        shownState = upload.state();
    }

    private String refreshLabel() {
        long fresh = ChatBuffer.added() - addedAtSnapshot;
        return fresh > 0 ? I18n.get("fslog.view.refresh.new", fresh) : I18n.get("fslog.view.refresh");
    }

    /** Пересоздать кнопки (сменилось состояние выгрузки); набор в поле поиска не прерывается. */
    private void rebuild() {
        boolean typing = searchBox != null && searchBox.isFocused();
        int cursor = searchBox != null ? searchBox.getCursorPosition() : 0;
        this.clearWidgets();
        this.init();
        if (typing) {
            this.setFocused(searchBox);
            searchBox.setCursorPosition(cursor);
            searchBox.setHighlightPos(cursor);
        }
    }

    /** Кнопка «Выгрузить на сайт» / «Повторить» — единственный путь отправки лога. */
    public void pressUpload() {
        switch (upload.start()) {
            case BUSY -> setFlash(I18n.get("fslog.view.busy"));
            case COOLDOWN -> setFlash(I18n.get("fslog.wait", upload.refusedWait()));
            default -> {}
        }
        rebuild();
    }

    private void setFlash(String s) {
        flash = s;
        flashTicks = 80;
    }

    /** Ссылка открывается через обычное окно игры «Открыть ссылку?», после него — обратно сюда. */
    private void openLink(String url) {
        ConfirmLinkScreen.confirmLinkNow(this, java.net.URI.create(url));
    }

    @Override
    public void tick() {
        super.tick();
        if (flashTicks > 0 && --flashTicks == 0) flash = "";
        if (upload.state() != shownState) rebuild();   // ответ сети пришёл — показать ссылку или «Повторить»
        if (uploadButton != null) uploadButton.active = upload.canStart();
        if (refreshButton != null) {
            refreshButton.active = !upload.sending();
            refreshButton.setMessage(Component.literal(refreshLabel()));
        }
    }

    @Override
    public void added() {
        super.added();
        upload.reopen();
    }

    @Override
    public void removed() {
        // Закрыли во время отправки — ответ придёт в чат; само окно не откроется.
        upload.close();
        super.removed();
    }

    // ---------------------------------------------------------------- разметка

    /**
     * Сообщение из буфера → текст с цветом и оформлением (без кликов, подсказок и т. п.).
     * Найденное по поиску выделяется жёлтым с подчёркиванием, даже если совпадение идёт через куски разного цвета.
     */
    private static Component componentOf(ChatBuffer.Entry e, String q) {
        String all = plain(e);
        boolean[] hit = new boolean[all.length()];
        if (!q.isEmpty()) {
            for (int i = indexOf(all, q, 0); i >= 0; i = indexOf(all, q, i + q.length())) Arrays.fill(hit, i, i + q.length(), true);
        }
        MutableComponent c = Component.empty();
        int pos = 0;
        for (ChatBuffer.Seg s : e.segs()) {
            int f = s.flags();
            Style st = Style.EMPTY.withBold((f & 1) != 0).withItalic((f & 2) != 0).withUnderlined((f & 4) != 0)
                    .withStrikethrough((f & 8) != 0).withObfuscated((f & 16) != 0);
            if (s.color() >= 0) st = st.withColor(s.color());
            String t = s.text();
            for (int a = 0; a < t.length(); ) {
                boolean h = hit[pos + a];
                int b = a;
                while (b < t.length() && hit[pos + b] == h) b++;
                c.append(Component.literal(t.substring(a, b)).withStyle(h ? st.withColor(FOUND_RGB).withUnderlined(true) : st));
                a = b;
            }
            pos += t.length();
        }
        return c;
    }

    /** Переносит строки порциями, начиная с самых новых, — окно не замирает даже на 5000 сообщений. */
    private void layoutStep() {
        if (nextEntry < 0 || wrapWidth <= 0) return;
        long start = System.nanoTime();
        List<ChatBuffer.Entry> snap = upload.snapshot();
        while (nextEntry >= 0 && System.nanoTime() - start < LAYOUT_BUDGET_NS) {
            ChatBuffer.Entry e = snap.get(shown[nextEntry--]);
            List<FormattedCharSequence> parts = this.font.split(componentOf(e, query), wrapWidth);
            String time = TIME.format(Instant.ofEpochMilli(e.time()));
            if (parts.isEmpty()) {
                rev.add(new Line(FormattedCharSequence.EMPTY, time));
                continue;
            }
            for (int j = parts.size() - 1; j >= 0; j--) rev.add(new Line(parts.get(j), j == 0 ? time : null));
        }
    }

    private int rows() {
        return Math.max(1, (areaH - 6) / LINE_H);
    }

    private int maxScroll() {
        return Math.max(0, rev.size() - rows());
    }

    private void scrollBy(int lines) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll + lines));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollBy((int) Math.round(scrollY * 3));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.hasControlDown() && event.key() == InputConstants.KEY_F) {
            this.setFocused(searchBox);
            return true;
        }
        // Home/End в непустом поле поиска двигают курсор, а не список.
        boolean editing = searchBox != null && searchBox.isFocused() && !searchBox.getValue().isEmpty();
        switch (event.key()) {
            case InputConstants.KEY_UP -> { scrollBy(1); return true; }
            case InputConstants.KEY_DOWN -> { scrollBy(-1); return true; }
            case InputConstants.KEY_PAGEUP -> { scrollBy(rows() - 1); return true; }
            case InputConstants.KEY_PAGEDOWN -> { scrollBy(-(rows() - 1)); return true; }
            case InputConstants.KEY_HOME -> { if (editing) break; scroll = maxScroll(); return true; }
            case InputConstants.KEY_END -> { if (editing) break; scroll = 0; return true; }
            default -> {}
        }
        return super.keyPressed(event);
    }

    // ---------------------------------------------------------------- отрисовка

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        layoutStep();
        drawPanel(g);
        super.extractRenderState(g, mouseX, mouseY, delta);
        skin(g);

        int n = upload.snapshot().size();
        Component title = Component.translatable("fslog.view.title").withStyle(Style.EMPTY.withColor(C_GREEN_HI).withBold(true));
        g.text(this.font, title, left + 4, top + 1, 0xFFFFFFFF, true);
        String keys = I18n.get("fslog.view.keys");
        if (this.font.width(title) + this.font.width(keys) + 16 < w) {
            g.text(this.font, Component.literal(keys), left + w - 4 - this.font.width(keys), top + 1, C_DIM, false);
        }
        String info = query.isEmpty() ? I18n.get("fslog.view.count", n, TIME.format(Instant.ofEpochMilli(snapshotTime)))
                : I18n.get("fslog.view.found", shown.length, n);
        info = fit(info, w - Math.min(170, w / 2) - 12);
        g.text(this.font, Component.literal(info), left + 4, top + 13, query.isEmpty() ? C_MUTED : C_YELLOW, false);

        // Область сообщений: рисуются только видимые строки.
        int ax = left, ay = areaY, aw = w;
        g.fill(ax, ay, ax + aw, ay + areaH, C_LINE);
        g.fill(ax + 1, ay + 1, ax + aw - 1, ay + areaH - 1, C_SURFACE);
        if (n == 0 || shown.length == 0) {
            String empty = fit(I18n.get(n == 0 ? "fslog.view.empty" : "fslog.view.notfound"), aw - 12);
            g.text(this.font, Component.literal(empty), ax + (aw - this.font.width(empty)) / 2, ay + areaH / 2 - 4, C_MUTED, false);
        } else {
            scroll = Math.min(scroll, maxScroll());
            int rows = rows();
            for (int r = 0; r < rows; r++) {
                int k = scroll + r;
                if (k >= rev.size()) break;
                Line line = rev.get(k);
                int y = ay + areaH - 3 - (r + 1) * LINE_H;
                if (line.time() != null) g.text(this.font, Component.literal(line.time()), ax + 6, y, C_MUTED, false);
                g.text(this.font, line.text(), ax + 6 + timeW, y, 0xFFFFFFFF, false);
            }
            if (nextEntry >= 0) {
                String wait = I18n.get("fslog.view.layout", shown.length - nextEntry - 1, shown.length);
                g.text(this.font, Component.literal(wait), ax + aw - 8 - this.font.width(wait), ay + 4, C_MUTED, false);
            }
            // Полоса прокрутки.
            int total = rev.size();
            if (total > rows) {
                int trackH = areaH - 4, thumbH = Math.max(12, trackH * rows / total);
                int thumbY = ay + 2 + (int) ((long) (trackH - thumbH) * (maxScroll() - scroll) / Math.max(1, maxScroll()));
                g.fill(ax + aw - 4, ay + 2, ax + aw - 2, ay + areaH - 2, 0xFF141A13);
                g.fill(ax + aw - 4, thumbY, ax + aw - 2, thumbY + thumbH, C_GREEN);
            }
        }

        // Строка состояния (одна строка; длинное — переносится, видна первая часть).
        List<FormattedCharSequence> status = this.font.split(statusLine(), w - 8);
        if (!status.isEmpty()) g.text(this.font, status.get(0), left + 4, top + h - 36, 0xFFFFFFFF, false);
    }

    private Component statusLine() {
        if (!flash.isEmpty()) return Component.literal(flash).withStyle(Style.EMPTY.withColor(C_YELLOW));
        int n = upload.snapshot().size();
        LogUpload.Result r = upload.result();
        return switch (upload.state()) {
            case SENDING -> Component.translatable("fslog.uploading", n).withStyle(Style.EMPTY.withColor(C_MUTED));
            case DONE -> Component.empty()
                    .append(Component.translatable("fslog.done", n).withStyle(Style.EMPTY.withColor(C_GREEN_HI)))
                    .append(" ")
                    .append(Component.literal(r.url()).withStyle(Style.EMPTY.withColor(C_LINK).withUnderlined(true)));
            case FAILED -> Component.translatable("fslog.failed", LogCommand.reason(r)).withStyle(Style.EMPTY.withColor(C_RED));
            default -> {
                if (n == 0) yield Component.translatable("fslog.empty").withStyle(Style.EMPTY.withColor(C_MUTED));
                if (LogUpload.GATE.busy()) yield Component.translatable("fslog.view.busy").withStyle(Style.EMPTY.withColor(C_YELLOW));
                int wait = LogUpload.GATE.waitSec(System.currentTimeMillis());
                if (wait > 0) yield Component.translatable("fslog.wait", wait).withStyle(Style.EMPTY.withColor(C_YELLOW));
                if (!query.isEmpty()) yield Component.translatable("fslog.view.search.note", n).withStyle(Style.EMPTY.withColor(C_MUTED));
                yield Component.translatable("fslog.view.hint").withStyle(Style.EMPTY.withColor(C_MUTED));
            }
        };
    }

    /** Обрезает строку с «…», чтобы влезла в maxW пикселей. */
    private String fit(String s, int maxW) {
        if (this.font.width(s) <= maxW) return s;
        int end = s.length();
        while (end > 0 && this.font.width(s.substring(0, end) + "…") > maxW) end--;
        return s.substring(0, end) + "…";
    }

    /** Подложка окна в стиле FSPIRAT (как у окон FSTWEAK). */
    private void drawPanel(GuiGraphicsExtractor g) {
        int x0 = left - 6, y0 = top - 4, x1 = left + w + 6, y1 = top + h + 4;
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0x247FBF3A);
        g.fill(x0, y0, x1, y1, 0xF20B100B);
        border(g, x0, y0, x1, y1, C_LINE_HI);
        g.fill(x0, y0, x0 + 28, y0 + 2, C_GREEN);
    }

    private static void border(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int c) {
        g.fill(x0, y0, x1, y0 + 1, c);
        g.fill(x0, y1 - 1, x1, y1, c);
        g.fill(x0, y0, x0 + 1, y1, c);
        g.fill(x1 - 1, y0, x1, y1, c);
    }

    /** Кнопки в стиле FSPIRAT поверх стандартных (как HexUi.skin в FSTWEAK); «Выгрузить» — зелёная. */
    private void skin(GuiGraphicsExtractor g) {
        for (GuiEventListener e : this.children()) {
            if (e instanceof EditBox box && box.isVisible()) {
                border(g, box.getX(), box.getY(), box.getX() + box.getWidth(), box.getY() + box.getHeight(),
                        box.isFocused() ? C_GREEN : box.isHovered() ? C_LINE_HI : C_LINE);
                continue;
            }
            if (!(e instanceof Button b) || !b.visible) continue;
            String msg = b.getMessage().getString();
            if (msg.isEmpty()) continue;
            // Стандартную кнопку делаем почти прозрачной, а сверху рисуем свою.
            b.setAlpha(0.03f);
            boolean accent = b == uploadButton, hover = b.active && b.isHovered();
            int x = b.getX(), y = b.getY(), bw = b.getWidth(), bh = b.getHeight();
            int bg = !b.active ? 0xFF0A0D0A : accent ? (hover ? C_GREEN_HI : C_GREEN) : hover ? C_CARD_HI : C_CARD;
            int edge = !b.active ? 0xFF1A2117 : accent ? bg : hover ? C_GREEN : C_LINE;
            g.fill(x, y, x + bw, y + bh, edge);
            g.fill(x + 1, y + 1, x + bw - 1, y + bh - 1, bg);
            if (!accent) g.fill(x + 1, y + bh - 2, x + bw - 1, y + bh - 1, 0x50000000);
            int color = !b.active ? C_DIM : accent ? 0xFF071006 : hover ? C_GREEN_LT : C_TEXT;
            if (this.font.width(msg) > bw - 6) {
                while (msg.length() > 1 && this.font.width(msg + "…") > bw - 6) msg = msg.substring(0, msg.length() - 1);
                msg += "…";
            }
            g.text(this.font, Component.literal(msg), x + (bw - this.font.width(msg)) / 2, y + (bh - 8) / 2, color, !accent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
