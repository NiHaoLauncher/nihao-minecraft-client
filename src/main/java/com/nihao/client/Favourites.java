package com.nihao.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/** The player's favourites (screenshots), kept in {@code <game dir>/nihao/favourites.txt} as "kind:id" lines. */
public final class Favourites {
    private static Set<String> set;

    private Favourites() {}

    private static Path file() {
        return Platform.gameDir().resolve("nihao").resolve("favourites.txt");
    }

    private static synchronized Set<String> load() {
        if (set == null) {
            set = new LinkedHashSet<>();
            try {
                if (Files.exists(file())) for (String line : Files.readAllLines(file())) if (!line.isBlank()) set.add(line.strip());
            } catch (Exception ignored) {
                // unreadable: start empty
            }
        }
        return set;
    }

    public static boolean has(String kind, String id) {
        return load().contains(kind + ":" + id);
    }

    /** @return true if it is a favourite now */
    public static synchronized boolean toggle(String kind, String id) {
        String key = kind + ":" + id;
        boolean added = load().add(key);
        if (!added) load().remove(key);
        try {
            Files.createDirectories(file().getParent());
            Files.write(file(), load());
        } catch (Exception e) {
            System.err.println("[NiHaoClient] could not save the favourites: " + e);
        }
        return added;
    }

    /** A small yellow star (7x7 pixels) at x, y: the favourite marker on cards and thumbnails. */
    public static void drawStar(net.minecraft.client.gui.GuiGraphics g, int x, int y) {
        int c = 0xFFFFD83A;
        g.fill(x + 3, y, x + 4, y + 7, c);
        g.fill(x, y + 3, x + 7, y + 4, c);
        g.fill(x + 2, y + 2, x + 5, y + 5, c);
        g.fill(x + 1, y + 1, x + 2, y + 2, c);
        g.fill(x + 5, y + 1, x + 6, y + 2, c);
        g.fill(x + 1, y + 5, x + 2, y + 6, c);
        g.fill(x + 5, y + 5, x + 6, y + 6, c);
    }
}
