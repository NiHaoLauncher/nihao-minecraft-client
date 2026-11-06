package com.nihao.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** General NiHaoClient settings, kept in {@code <game dir>/nihao/config.properties}. */
public final class NiHaoConfig {
    /** Whose Ely.by skins and capes are shown; the others keep the vanilla ones. {@code key} is the translation key (see {@link Lang}). */
    public enum SkinMode {
        ALL("skins.all"), OTHERS("skins.others"), ME("skins.me"), OFF("skins.off");

        public final String key;

        SkinMode(String key) {
            this.key = key;
        }

        public SkinMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public static SkinMode skins = SkinMode.ALL;

    /** The zoom key works. */
    public static boolean zoom = true;

    private NiHaoConfig() {}

    private static Path file() {
        return Platform.gameDir().resolve("nihao").resolve("config.properties");
    }

    static {
        Properties p = new Properties();
        try (var in = Files.newInputStream(file())) {
            p.load(in);
        } catch (Exception ignored) {
            // first start: defaults
        }
        zoom = !"false".equals(p.getProperty("zoom"));
        try {
            skins = SkinMode.valueOf(p.getProperty("skins", "ALL"));
        } catch (IllegalArgumentException ignored) {
            // keeps the default
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("zoom", String.valueOf(zoom));
        p.setProperty("skins", skins.name());
        try {
            Files.createDirectories(file().getParent());
            try (var out = Files.newOutputStream(file())) {
                p.store(out, "NiHaoClient settings");
            }
        } catch (Exception ignored) {
            // the settings are simply not remembered
        }
    }
}
