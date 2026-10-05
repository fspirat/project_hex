package ru.fspirat.fslog;

import net.fabricmc.api.ClientModInitializer;

/** Точка входа отдельного мода FSLOG. */
public final class FsLogClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FsLog.init();
    }
}
