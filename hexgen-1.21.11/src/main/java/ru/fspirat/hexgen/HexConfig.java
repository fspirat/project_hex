package ru.fspirat.hexgen;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/** Настройки мода (config/fstweak.properties): кнопка, подсказки, свои пресеты, история. */
public final class HexConfig {
    private HexConfig() {}

    public static final int DEFAULT_OFFSET_X = 7;
    public static final int DEFAULT_OFFSET_Y = -46;
    public static final int MAX_PRESETS = 12;
    public static final int MAX_HISTORY = 10;

    /** Положение кнопки относительно левого верхнего угла инвентаря. */
    public static int buttonOffsetX = DEFAULT_OFFSET_X;
    public static int buttonOffsetY = DEFAULT_OFFSET_Y;
    /** Показывать всплывающие подсказки на кнопках. */
    public static boolean showHints = true;
    /** Анимированный градиент в предпросмотре и заголовке. */
    public static boolean animatePreview = true;
    /** Тема окна: индекс в THEMES. */
    public static int theme = 0;
    /** Ключи перевода тем. */
    public static final String[] THEMES = {"theme.purple", "theme.dark", "theme.gradient"};
    public static final List<HexCore.Preset> USER_PRESETS = new ArrayList<>();
    /** Последние скопированные/выполненные команды, новые сверху. */
    public static final List<String> HISTORY = new ArrayList<>();

    private static Path dir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    private static Path file() {
        return dir().resolve("fstweak.properties");
    }

    public static void load() {
        Path f = file();
        // Файл прошлой версии мода.
        if (!Files.exists(f)) f = dir().resolve("fshex-generator.properties");
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (Exception e) {
            return;
        }
        buttonOffsetX = intProp(p, "buttonOffsetX", DEFAULT_OFFSET_X);
        buttonOffsetY = intProp(p, "buttonOffsetY", DEFAULT_OFFSET_Y);
        showHints = !"false".equals(p.getProperty("showHints"));
        animatePreview = !"false".equals(p.getProperty("animatePreview"));
        theme = Math.floorMod(intProp(p, "theme", 0), THEMES.length);
        USER_PRESETS.clear();
        for (int i = 0; i < MAX_PRESETS; i++) {
            String v = p.getProperty("preset." + i);
            if (v == null) continue;
            int bar = v.lastIndexOf('|');
            if (bar <= 0) continue;
            String[] colors = v.substring(bar + 1).split(",");
            boolean ok = colors.length >= 1;
            for (String c : colors) ok &= HexCore.valid(c);
            if (ok) USER_PRESETS.add(new HexCore.Preset(v.substring(0, bar), colors));
        }
        HISTORY.clear();
        for (int i = 0; i < MAX_HISTORY; i++) {
            String v = p.getProperty("history." + i);
            if (v != null && !v.isEmpty()) HISTORY.add(v);
        }
    }

    private static int intProp(Properties p, String key, int def) {
        try {
            return Integer.parseInt(p.getProperty(key, String.valueOf(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("buttonOffsetX", String.valueOf(buttonOffsetX));
        p.setProperty("buttonOffsetY", String.valueOf(buttonOffsetY));
        p.setProperty("showHints", String.valueOf(showHints));
        p.setProperty("animatePreview", String.valueOf(animatePreview));
        p.setProperty("theme", String.valueOf(theme));
        for (int i = 0; i < USER_PRESETS.size(); i++) {
            HexCore.Preset pr = USER_PRESETS.get(i);
            p.setProperty("preset." + i, pr.name() + "|" + String.join(",", pr.colors()));
        }
        for (int i = 0; i < HISTORY.size(); i++) p.setProperty("history." + i, HISTORY.get(i));
        try {
            Files.createDirectories(dir());
            try (Writer w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
                p.store(w, "FSTWEAK");
            }
        } catch (Exception ignored) {
        }
    }

    public static void addHistory(String command) {
        HISTORY.remove(command);
        HISTORY.add(0, command);
        while (HISTORY.size() > MAX_HISTORY) HISTORY.remove(HISTORY.size() - 1);
        save();
    }

    /** Свободное имя вида «Мой N» для нового пресета. */
    public static String nextPresetName() {
        int n = 1;
        while (true) {
            String name = HexUi.tr("preset.my", n);
            boolean used = false;
            for (HexCore.Preset p : USER_PRESETS) used |= p.name().equals(name);
            if (!used) return name;
            n++;
        }
    }

    /** Сохраняет пресет; пресет с тем же именем перезаписывается. */
    public static void addPreset(String name, String[] colors) {
        for (int i = 0; i < USER_PRESETS.size(); i++) {
            if (USER_PRESETS.get(i).name().equals(name)) {
                USER_PRESETS.set(i, new HexCore.Preset(name, colors));
                save();
                return;
            }
        }
        if (USER_PRESETS.size() >= MAX_PRESETS) USER_PRESETS.remove(0);
        USER_PRESETS.add(new HexCore.Preset(name, colors));
        save();
    }

    public static boolean hasPreset(String name) {
        for (HexCore.Preset p : USER_PRESETS) if (p.name().equals(name)) return true;
        return false;
    }

    /** Переименовывает свой пресет. False — если новое имя уже занято другим пресетом. */
    public static boolean renamePreset(String oldName, String newName) {
        if (!oldName.equals(newName) && hasPreset(newName)) return false;
        for (int i = 0; i < USER_PRESETS.size(); i++) {
            HexCore.Preset p = USER_PRESETS.get(i);
            if (p.name().equals(oldName)) {
                USER_PRESETS.set(i, new HexCore.Preset(newName, p.colors()));
                save();
                return true;
            }
        }
        return false;
    }

    /** Переставляет свой пресет с позиции from на позицию to. */
    public static void movePreset(int from, int to) {
        if (from < 0 || from >= USER_PRESETS.size() || to < 0 || to >= USER_PRESETS.size() || from == to) return;
        USER_PRESETS.add(to, USER_PRESETS.remove(from));
        save();
    }
}
