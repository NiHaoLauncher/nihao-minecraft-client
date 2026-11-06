package com.nihao.client;

import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LoadingModList;

import java.nio.file.Files;
import java.nio.file.Path;

/** Loader-specific facts (NeoForge). Same class name/signatures in every loaders/* folder. */
final class Platform {
    private Platform() {}

    static String loader() {
        return "neoforge";
    }

    static Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    static Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    static String version(String modId) {
        var f = LoadingModList.get().getModFileById(modId);
        return f == null ? "0" : f.getMods().get(0).getVersion().toString();
    }

    /** This mod's jar, or null when running from a directory (dev environment). */
    static Path modJar() {
        try {
            Path p = LoadingModList.get().getModFileById("nihao").getFile().getFilePath();
            return Files.isRegularFile(p) ? p : null;
        } catch (Exception e) {
            return null;
        }
    }
}
