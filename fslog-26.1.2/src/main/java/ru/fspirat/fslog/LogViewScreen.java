package ru.fspirat.fslog;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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
import org.lwjgl.glfw.GLFW;

/**
 * Просмотр накопленного чата внутри игры (/log). Показывается снимок сообщений на момент открытия;
 * на сайт он уходит только по кнопке «Выгрузить на сайт». Сообщения выводятся как текст с цветами и
 * оформлением — без кликов и подсказок из оригинала, поэтому ничего из них не выполняется.
 */
public final class LogViewScreen extends Screen {
    // Цвета сайта FSPIRAT — как в окнах FSTWEAK.
    private static final int C_SURFACE = 0xFF080B08, C_CARD = 0xFF0D120D, C_CARD_HI = 0xFF151C14, C_LINE = 0xFF273322,
            C_LINE_HI = 0xFF34452D, C_TEXT = 0xFFE8EEE6, C_MUTED = 0xFF7C8578, C_DIM = 0xFF5E665A, C_GREEN = 0xFF7FBF3A,
            C_GREEN_HI = 0xFF9BE052, C_GREEN_LT = 0xFFD4FF9A, C_RED = 0xFFFF6B6B, C_YELLOW = 0xFFFFD24D, C_LINK = 0xFF55FFFF;
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
    private int nextEntry;
    private int wrapWidth = -1;
    /** Прокрутка: на сколько строк окно поднято от самых новых сообщений. */
    private int scroll;

    private int left, top, w, h, areaY, areaH, timeW;
    private Button uploadButton, refreshButton;
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
        rev.clear();
        nextEntry = n - 1;
        scroll = 0;
    }

    /** Выгрузка этого окна (для автотестов). */
    public LogUpload<ChatBuffer.Entry> upload() {
        return upload;
    }

    /** Все сообщения снимка уже разбиты на строки (для автотестов). */
    public boolean laidOut() {
        return nextEntry < 0;
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
            nextEntry = upload.snapshot().size() - 1;
        }

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

    private void rebuild() {
        this.clearWidgets();
        this.init();
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
        ConfirmLinkScreen.confirmLinkNow(this, url);
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

    /** Сообщение из буфера → текст с цветом и оформлением (без кликов, подсказок и т. п.). */
    private static Component componentOf(ChatBuffer.Entry e) {
        MutableComponent c = Component.empty();
        for (ChatBuffer.Seg s : e.segs()) {
            int f = s.flags();
            Style st = Style.EMPTY.withBold((f & 1) != 0).withItalic((f & 2) != 0).withUnderlined((f & 4) != 0)
                    .withStrikethrough((f & 8) != 0).withObfuscated((f & 16) != 0);
            if (s.color() >= 0) st = st.withColor(s.color());
            c.append(Component.literal(s.text()).withStyle(st));
        }
        return c;
    }

    /** Переносит строки порциями, начиная с самых новых, — окно не замирает даже на 5000 сообщений. */
    private void layoutStep() {
        if (nextEntry < 0 || wrapWidth <= 0) return;
        long start = System.nanoTime();
        List<ChatBuffer.Entry> snap = upload.snapshot();
        while (nextEntry >= 0 && System.nanoTime() - start < LAYOUT_BUDGET_NS) {
            ChatBuffer.Entry e = snap.get(nextEntry--);
            List<FormattedCharSequence> parts = this.font.split(componentOf(e), wrapWidth);
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
        switch (event.key()) {
            case GLFW.GLFW_KEY_UP -> { scrollBy(1); return true; }
            case GLFW.GLFW_KEY_DOWN -> { scrollBy(-1); return true; }
            case GLFW.GLFW_KEY_PAGE_UP -> { scrollBy(rows() - 1); return true; }
            case GLFW.GLFW_KEY_PAGE_DOWN -> { scrollBy(-(rows() - 1)); return true; }
            case GLFW.GLFW_KEY_HOME -> { scroll = maxScroll(); return true; }
            case GLFW.GLFW_KEY_END -> { scroll = 0; return true; }
            default -> { return super.keyPressed(event); }
        }
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
        String info = I18n.get("fslog.view.count", n, TIME.format(Instant.ofEpochMilli(snapshotTime)));
        g.text(this.font, Component.literal(info), left + 4, top + 13, C_MUTED, false);

        // Область сообщений: рисуются только видимые строки.
        int ax = left, ay = areaY, aw = w;
        g.fill(ax, ay, ax + aw, ay + areaH, C_LINE);
        g.fill(ax + 1, ay + 1, ax + aw - 1, ay + areaH - 1, C_SURFACE);
        if (n == 0) {
            String empty = I18n.get("fslog.view.empty");
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
                String wait = I18n.get("fslog.view.layout", n - nextEntry - 1, n);
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
                yield Component.translatable("fslog.view.hint").withStyle(Style.EMPTY.withColor(C_MUTED));
            }
        };
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
