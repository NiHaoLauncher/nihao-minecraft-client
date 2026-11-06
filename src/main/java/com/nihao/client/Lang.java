package com.nihao.client;

import net.minecraft.client.resources.language.I18n;

/** Translated text of the mod: the keys of assets/nihao/lang/*.json (en_us, uk_ua, ru_ru) without the "nihao." prefix. */
final class Lang {
    private Lang() {}

    static String t(String key, Object... args) {
        return I18n.get("nihao." + key, args);
    }
}
