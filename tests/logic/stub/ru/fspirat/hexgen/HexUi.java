package ru.fspirat.hexgen;

/** Заглушка для тестов логики: HexCore берёт отсюда только перевод подписи шрифта. */
public final class HexUi {
    private HexUi() {}
    public static String tr(String key, Object... args) { return key; }
}
