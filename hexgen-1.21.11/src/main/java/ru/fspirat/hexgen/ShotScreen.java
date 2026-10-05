package ru.fspirat.hexgen;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Градиент со скриншота: берёт скриншоты из папки screenshots (F2), находит на них строки цветного текста
 * и подбирает цвета градиента — как кнопка «Узнать градиент по скриншоту» на сайте.
 */
public class ShotScreen extends Screen {
    private static final int W = 320;
    private static final int H = 220;
    private static final int ROWS = 6;
    private static int fileIndex = 0;

    private final Screen parent;
    private final Consumer<String[]> apply;
    private List<File> files = new ArrayList<>();
    private ShotGradient.Result result;
    private String error;
    private boolean loading;
    private int page = 0;
    private int left;
    private int top;
    private final List<ShotGradient.Line> shown = new ArrayList<>();
    private final List<Integer> rowY = new ArrayList<>();

    public ShotScreen(Screen parent, Consumer<String[]> apply) {
        super(Component.literal(HexUi.tr("shot.title")));
        this.parent = parent;
        this.apply = apply;
    }

    private void scan() {
        File dir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("screenshots").toFile();
        File[] list = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".png"));
        files = list == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(list));
        files.sort(Comparator.comparingLong(File::lastModified).reversed());
        fileIndex = Math.max(0, Math.min(fileIndex, files.size() - 1));
    }

    private void load() {
        result = null;
        error = null;
        page = 0;
        if (files.isEmpty()) { error = HexUi.tr("shot.none"); return; }
        File f = files.get(fileIndex);
        loading = true;
        CompletableFuture.supplyAsync(() -> {
            try {
                return ShotGradient.analyze(ImageIO.read(f));
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(r -> this.minecraft.execute(() -> {
            loading = false;
            if (r == null) error = HexUi.tr("shot.read_error");
            else {
                result = r;
                if (r.lines().isEmpty()) error = HexUi.tr("shot.no_text");
            }
            rebuild();
        }));
    }

    @Override
    protected void init() {
        left = (this.width - W) / 2;
        top = Math.max(8, (this.height - H) / 2);
        if (files.isEmpty() && result == null && !loading && error == null) { scan(); load(); }

        Button older = Button.builder(Component.literal("◀"), b -> { fileIndex++; load(); rebuild(); }).bounds(left, top + 14, 16, 14)
                .tooltip(HexUi.tip(HexUi.tr("shot.older"))).build();
        Button newer = Button.builder(Component.literal("▶"), b -> { fileIndex--; load(); rebuild(); }).bounds(left + W - 16, top + 14, 16, 14)
                .tooltip(HexUi.tip(HexUi.tr("shot.newer"))).build();
        older.active = !loading && fileIndex < files.size() - 1;
        newer.active = !loading && fileIndex > 0;
        this.addRenderableWidget(older);
        this.addRenderableWidget(newer);

        shown.clear();
        rowY.clear();
        if (result != null) {
            List<ShotGradient.Line> lines = new ArrayList<>(result.lines());
            // сначала строки с градиентом, потом одноцветные; внутри — сверху вниз
            lines.sort(Comparator.<ShotGradient.Line>comparingInt(l -> l.stops().size() > 1 ? 0 : 1).thenComparingInt(ShotGradient.Line::y));
            int pages = Math.max(1, (lines.size() + ROWS - 1) / ROWS);
            page = Math.max(0, Math.min(page, pages - 1));
            for (int i = page * ROWS, k = 0; i < Math.min(lines.size(), (page + 1) * ROWS); i++, k++) {
                ShotGradient.Line l = lines.get(i);
                int y = top + 34 + k * 26;
                shown.add(l);
                rowY.add(y);
                this.addRenderableWidget(HexUi.accent(Button.builder(Component.literal(HexUi.tr("shot.apply")), b -> pick(l.stops()))
                        .bounds(left + W - 70, y + 2, 70, 18).build()));
                if (l.alt() != null) {
                    this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("shot.simple")), b -> pick(l.alt()))
                            .bounds(left + W - 142, y + 2, 70, 18)
                            .tooltip(HexUi.tip(HexUi.tr("shot.simple.hint", String.join(" → ", l.alt())))).build());
                }
            }
            if (pages > 1) {
                Button prev = Button.builder(Component.literal("◀"), b -> { page--; rebuild(); }).bounds(left, top + H - 20, 16, 20).build();
                Button next = Button.builder(Component.literal("▶"), b -> { page++; rebuild(); }).bounds(left + 50, top + H - 20, 16, 20).build();
                prev.active = page > 0;
                next.active = page < pages - 1;
                this.addRenderableWidget(prev);
                this.addRenderableWidget(next);
            }
        }
        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("shot.refresh")), b -> { scan(); fileIndex = 0; load(); rebuild(); })
                .bounds(left + W - 186, top + H - 20, 90, 20).tooltip(HexUi.tip(HexUi.tr("shot.refresh.hint"))).build());
        this.addRenderableWidget(Button.builder(Component.literal(HexUi.tr("back")), b -> this.onClose())
                .bounds(left + W - 90, top + H - 20, 90, 20).build());
    }

    private void pick(List<String> stops) {
        apply.accept(stops.toArray(new String[0]));
        this.onClose();
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        HexUi.drawPanel(g, left, top, W, H);
        super.render(g, mouseX, mouseY, delta);
        HexUi.skin(this, g, mouseX, mouseY);
        Component t = HexUi.gradientTitle(HexUi.tr("shot.title"));
        g.drawString(this.font, t, (this.width - this.font.width(t)) / 2, top + 1, 0xFFFFFFFF, true);

        String name = files.isEmpty() ? "—" : files.get(fileIndex).getName() + "  ·  "
                + new SimpleDateFormat("dd.MM HH:mm").format(new Date(files.get(fileIndex).lastModified()))
                + "  (" + (fileIndex + 1) + "/" + files.size() + ")";
        g.drawString(this.font, Component.literal(name), left + (W - this.font.width(name)) / 2, top + 17, 0xFFA0A0A0, false);

        if (loading) {
            String s = HexUi.tr("shot.loading");
            g.drawString(this.font, Component.literal(s), left + (W - this.font.width(s)) / 2, top + 90, 0xFFA0A0A0, false);
        } else if (error != null) {
            String s = error;
            g.drawString(this.font, Component.literal(s), left + (W - this.font.width(s)) / 2, top + 80, 0xFFFF7777, false);
            String hint = HexUi.tr("shot.hint");
            g.drawString(this.font, Component.literal(hint), left + (W - this.font.width(hint)) / 2, top + 96, 0xFF808080, false);
        }

        for (int i = 0; i < shown.size(); i++) {
            ShotGradient.Line l = shown.get(i);
            int y = rowY.get(i);
            g.fill(left, y, left + W, y + 22, HexUi.fspirat() ? HexUi.C_SURFACE : 0x80000000);
            // цвета найденных символов
            int x = left + 4;
            int maxX = left + W - (l.alt() != null ? 150 : 78);
            for (int c : l.chars()) {
                if (x + 5 > maxX) break;
                if (c >= 0) g.fill(x, y + 3, x + 4, y + 9, 0xFF000000 | c);
                x += c >= 0 ? 5 : 3;
            }
            HexUi.drawGradient(g, left + 4, y + 12, Math.min(110, maxX - left - 8), 3, l.stops());
            String info = String.join(" ", l.stops().stream().map(s -> "#" + s).toList());
            int maxInfo = maxX - (left + 120);
            if (this.font.width(info) > maxInfo) info = HexUi.tr("shot.colors", l.stops().size());
            g.drawString(this.font, Component.literal(info), left + 120, y + 10, 0xFFE8EEE6, false);
        }
        if (result != null && !result.exact() && !result.lines().isEmpty()) {
            String s = HexUi.tr("shot.inexact");
            g.drawString(this.font, Component.literal(s), left + 72, top + H - 14, 0xFFFFD24D, false);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
