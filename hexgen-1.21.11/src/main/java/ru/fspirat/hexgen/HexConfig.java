package ru.fspirat.hexgen;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/** Настройки мода: положение кнопки над инвентарём (config/fshex-generator.properties). */
public final class HexConfig {
    private HexConfig() {}

    public static final int DEFAULT_OFFSET_X = 7;
    public static final int DEFAULT_OFFSET_Y = -46;

    /** Положение кнопки относительно левого верхнего угла инвентаря. */
    public static int buttonOffsetX = DEFAULT_OFFSET_X;
    public static int buttonOffsetY = DEFAULT_OFFSET_Y;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("fshex-generator.properties");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(f)) {
            p.load(r);
            buttonOffsetX = Integer.parseInt(p.getProperty("buttonOffsetX", String.valueOf(DEFAULT_OFFSET_X)).trim());
            buttonOffsetY = Integer.parseInt(p.getProperty("buttonOffsetY", String.valueOf(DEFAULT_OFFSET_Y)).trim());
        } catch (Exception e) {
            buttonOffsetX = DEFAULT_OFFSET_X;
            buttonOffsetY = DEFAULT_OFFSET_Y;
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("buttonOffsetX", String.valueOf(buttonOffsetX));
        p.setProperty("buttonOffsetY", String.valueOf(buttonOffsetY));
        try {
            Files.createDirectories(file().getParent());
            try (Writer w = Files.newBufferedWriter(file())) {
                p.store(w, "FSHEX GENERATOR");
            }
        } catch (Exception ignored) {
        }
    }
}
