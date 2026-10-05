package ru.fspirat.hexgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * Символы, как на сайте: вкладки по темам, «Избранные» и «Недавние».
 * Клик — вставить символ в текст, Shift + клик — добавить в избранное или убрать оттуда.
 */
public class SymbolsScreen extends Screen {
    private static final int COLS = 14;
    private static final int CELL = 20;
    private static final int ROWS = 6;
    private static final int W = COLS * CELL - 2;
    private static final int PER_PAGE = COLS * ROWS;
    /** Вкладка: -2 избранные, -1 недавние, 0… — темы из SiteData.SYMBOLS. Запоминается, пока игра запущена. */
    private static Integer tab = null;
    private static int page = 0;

    private final Screen parent;
    private final Consumer<String> insert;
    private final List<Button> cells = new ArrayList<>();
    private final List<String> cellSyms = new ArrayList<>();
    private int left;
    private int top;
    private int h;
    private String pageLabel = "";

    public SymbolsScreen(Screen parent, Consumer<String> insert) {
        super(Component.literal(HexUi.tr("symbols.title")));
        this.parent = parent;
        this.insert = insert;
    }

    private static List<String> split(String s) {
        List<String> out = new ArrayList<>();
        s.codePoints().forEach(c -> out.add(new String(Character.toChars(c))));
        return out;
    }

    private static List<String> symbolsOf(int t) {
        if (t == -2) return split(HexConfig.favSymbols);
        if (t == -1) return split(HexConfig.recentSymbols);
        return split(SiteData.SYMBOLS.get(t).chars());
    }

    private String tabName(int t) {
        return t == -2 ? "★ " + HexUi.tr("symbols.fav") : t == -1 ? HexUi.tr("symbols.recent") : HexUi.tr(SiteData.SYMBOLS.get(t).key());
    }

    @Override
    protected void init() {
        if (tab == null) tab = !HexConfig.favSymbols.isEmpty() ? -2 : !HexConfig.recentSymbols.isEmpty() ? -1 : 0;
        h = 62 + ROWS * CELL + 34;
        left = (this.width - W) / 2;
        top = Math.max(4, (this.height - h) / 2);
        cells.clear();
        cellSyms.clear();

        // Вкладки: Избранные · Недавние · ◀ тема ▶
        int y = top + 38;
        Button fav = Button.builder(Component.literal("★ " + HexUi.tr("symbols.fav")), b -> select(-2)).bounds(left, y, 70, 16).build();
        Button rec = Button.builder(Component.literal(HexUi.tr("symbols.recent")), b -> select(-1)).bounds(left + 72, y, 64, 16).build();
        if (tab == -2) HexUi.accent(fav);
        if (tab == -1) HexUi.accent(rec);
        this.addRenderableWidget(fav);
        this.addRenderableWidget(rec);
        int themes = SiteData.SYMBOLS.size(), cur = Math.max(0, tab);
        this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> select(Math.floorMod(Math.max(0, tab) - 1, themes)))
                .bounds(left + 140, y, 16, 16).build());
        Button theme = Button.builder(Component.literal(tabName(cur)), b -> select(Math.floorMod(Math.max(0, tab) + 1, themes)))
                .bounds(left + 158, y, W - 158 - 18, 16).tooltip(HexUi.tip(HexUi.tr("symbols.theme.hint"))).build();
        if (tab >= 0) HexUi.accent(theme);
        this.addRenderableWidget(theme);
        this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> select(Math.floorMod(Math.max(0, tab) + 1, themes)))
                .bounds(left + W - 16, y, 16, 16).build());

        List<String> syms = symbolsOf(tab);
        int pages = Math.max(1, (syms.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        String favs = HexConfig.favSymbols;
        for (int i = page * PER_PAGE, k = 0; i < Math.min(syms.size(), (page + 1) * PER_PAGE); i++, k++) {
            final String sym = syms.get(i);
            Button b = Button.builder(Component.literal(sym), btn -> click(sym))
                    .bounds(left + (k % COLS) * CELL, top + 58 + (k / COLS) * CELL, CELL - 2, CELL - 2)
                    .tooltip(HexUi.tip(HexUi.tr(favs.contains(sym) ? "symbols.insert_fav" : "symbols.insert_add", sym)))
                    .build();
            cells.add(b);
            cellSyms.add(sym);
            this.addRenderableWidget(b);
        }
        int by = top + 58 + ROWS * CELL + 4;
        if (pages > 1) {
            Button prev = Button.builder(Component.literal("◀"), b -> { page--; rebuild(); }).bounds(left, by, 16, 16).build();
            Button next = Button.builder(Component.literal("▶"), b -> { page++; rebuild(); }).bounds(left + 52, by, 16, 16).build();
            prev.active = page > 0;
            next.active = page < pages - 1;
            this.addRenderableWidget(prev);
            this.addRenderableWidget(next);
        }
        pageLabel = pages > 1 ? (page + 1) + "/" + pages : "";

        this.addRenderableWidget(HexUi.accent(Button.builder(Component.literal(HexUi.tr("done")), b -> this.onClose())
                .bounds(left + W - 100, by + 8, 100, 20).build()));
    }

    private void select(int t) {
        tab = t;
        page = 0;
        rebuild();
    }

    private void click(String sym) {
        if (HexUi.shiftDown()) {
            // Shift + клик — в избранное или из избранного
            HexConfig.favSymbols = HexConfig.favSymbols.contains(sym) ? HexConfig.favSymbols.replace(sym, "") : HexConfig.favSymbols + sym;
            HexConfig.save();
            rebuild();
            return;
        }
        insert.accept(sym);
        List<String> recent = split(HexConfig.recentSymbols);
        recent.remove(sym);
        recent.add(0, sym);
        while (recent.size() > HexConfig.MAX_RECENT_SYMBOLS) recent.remove(recent.size() - 1);
        HexConfig.recentSymbols = String.join("", recent);
        HexConfig.save();
        if (tab == -1) rebuild();
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        HexUi.drawPanel(g, left, top, W, h);
        super.extractRenderState(g, mouseX, mouseY, delta);
        HexUi.skin(this, g, mouseX, mouseY);

        Component t = HexUi.gradientTitle(HexUi.tr("symbols.title"));
        g.text(this.font, t, (this.width - this.font.width(t)) / 2, top + 4, 0xFFFFFFFF, true);

        // Текущий текст, чтобы было видно, что вставилось.
        g.fill(left, top + 18, left + W, top + 34, HexUi.fspirat() ? HexUi.C_SURFACE : 0xC0000000);
        String text = HexGenScreen.currentText();
        String shown = text;
        while (!shown.isEmpty() && this.font.width(shown) > W - 8) shown = shown.substring(1);
        g.text(this.font, Component.literal(shown.length() < text.length() ? "…" + shown.substring(1) : shown),
                left + 4, top + 22, 0xFFFFFFFF, false);

        // Метка избранного: жёлтая точка в углу символа.
        String favs = HexConfig.favSymbols;
        for (int i = 0; i < cells.size(); i++) {
            if (!favs.contains(cellSyms.get(i))) continue;
            Button b = cells.get(i);
            g.fill(b.getX() + b.getWidth() - 4, b.getY() + 1, b.getX() + b.getWidth() - 1, b.getY() + 4, 0xFFFFD24D);
        }

        int by = top + 58 + ROWS * CELL + 4;
        if (!pageLabel.isEmpty()) g.text(this.font, Component.literal(pageLabel), left + 34 - this.font.width(pageLabel) / 2, by + 4, 0xFFA0A0A0, false);
        List<String> syms = symbolsOf(tab);
        if (syms.isEmpty()) {
            Component empty = Component.literal(HexUi.tr(tab == -2 ? "symbols.fav.empty" : "symbols.recent.empty")).withStyle(Style.EMPTY.withColor(0x808080));
            g.text(this.font, empty, left + (W - this.font.width(empty)) / 2, top + 58 + 40, 0xFF808080, false);
        }
        g.text(this.font, Component.literal(HexUi.tr("symbols.hint")), left, by + 22, 0xFF707070, false);
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
