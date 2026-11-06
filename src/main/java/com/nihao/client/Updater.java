package com.nihao.client;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Self-update. Checks GitHub and GitLab releases (GitLab is the mirror for when GitHub is down) and takes the newest version.
 * The check runs only for a "clean" start, without launcher flags (-Dnihao.session). The user confirms in the main menu;
 * the jar is then downloaded and swapped in after the game exits (the running jar is locked on Windows). The release notes
 * are saved to update.nihao and shown once after the update. Plain string JSON on purpose (no Minecraft/Gson dependency).
 */
public final class Updater {
    public record Release(String tag, String notes, List<String> urls) {}

    private record Source(String api, String notesKey, String assetKey) {}

    private static final Source[] SOURCES = {
            new Source("https://api.github.com/repos/NiHaoLauncher/nihao-minecraft-client/releases?per_page=5", "body", "browser_download_url"),
            new Source("https://gitlab.com/api/v4/projects/nihaolauncher%2Fnihao-minecraft-client/releases?per_page=5", "description", "direct_asset_url")};
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    /** A newer release, once found (null until then). */
    public static volatile Release available;
    /** idle, downloading, ready (applied on exit) or failed. */
    public static volatile String state = "idle";

    private Updater() {}

    /** Starts the background check unless the launcher passed flags or this is a dev environment. */
    static void start() {
        if (System.getProperty(Session.PROP) != null || Platform.modJar() == null) return;
        Thread t = new Thread(Updater::check, "nihao-update");
        t.setDaemon(true);
        t.start();
    }

    private static String currentVersion() {
        return Platform.version("nihao");
    }

    private static void check() {
        String mc = Platform.version("minecraft");
        Release best = null;
        for (Source s : SOURCES) {
            try {
                Release r = latest(s, mc);
                if (r == null) continue;
                if (best == null || compare(r.tag(), best.tag()) > 0) best = r;
                else if (compare(r.tag(), best.tag()) == 0) best.urls().addAll(r.urls()); // same version on the mirror: fallback download
            } catch (Exception e) {
                System.err.println("[NiHaoClient] update check failed (" + s.api() + "): " + e);
            }
        }
        if (best != null) available = best;
    }

    /** Newest release of a source that is newer than this jar and has a jar for this Minecraft version. */
    private static Release latest(Source s, String mc) throws Exception {
        String all = HTTP.send(HttpRequest.newBuilder(URI.create(s.api())).timeout(Duration.ofSeconds(10)).build(),
                HttpResponse.BodyHandlers.ofString()).body();
        int second = all.indexOf("\"tag_name\"", all.indexOf("\"tag_name\"") + 1); // newest release is listed first
        String rel = second < 0 ? all : all.substring(0, second);
        String tag = field(rel, "tag_name");
        if (tag == null || compare(tag, currentVersion()) <= 0) return null;
        List<String> urls = new ArrayList<>();
        Matcher m = Pattern.compile("\"" + s.assetKey() + "\"\\s*:\\s*\"([^\"]+)\"").matcher(rel);
        while (m.find()) {
            String file = fileName(m.group(1));
            if (file.startsWith("nihao-" + Platform.loader() + "-") && (file.endsWith("+" + mc + ".jar") || file.endsWith("." + mc + ".jar"))) urls.add(m.group(1));
        }
        if (urls.isEmpty()) return null;
        String notes = field(rel, s.notesKey());
        return new Release(tag, notes == null ? "" : notes, urls);
    }

    /** Downloads the update (called when the user agrees); it is applied when the game exits. */
    public static void download() {
        Release r = available;
        Path jar = Platform.modJar();
        if (r == null || jar == null || state.equals("downloading") || state.equals("ready")) return;
        state = "downloading";
        Thread t = new Thread(() -> {
            for (String url : r.urls()) {
                try {
                    Path target = jar.resolveSibling(fileName(url)), part = jar.resolveSibling(fileName(url) + ".part");
                    Files.copy(HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(2)).build(),
                            HttpResponse.BodyHandlers.ofInputStream()).body(), part, StandardCopyOption.REPLACE_EXISTING);
                    if (Files.size(part) < 10_000) { // not a jar (error page): try the next source
                        Files.deleteIfExists(part);
                        continue;
                    }
                    Properties p = new Properties();
                    p.setProperty("tag", r.tag());
                    p.setProperty("notes", r.notes());
                    try (var out = Files.newOutputStream(marker())) {
                        p.store(out, "Release notes to show after the update");
                    }
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> swap(jar, part, target)));
                    state = "ready";
                    return;
                } catch (Exception e) {
                    System.err.println("[NiHaoClient] update download failed (" + url + "): " + e);
                }
            }
            state = "failed";
        }, "nihao-update-download");
        t.setDaemon(true);
        t.start();
    }

    private static Path marker() {
        return Platform.gameDir().resolve("update.nihao");
    }

    /** Release notes of the update that was just applied (returned only once); null if there is none. */
    public static Release takeApplied() {
        try {
            Path f = marker();
            if (!Files.exists(f)) return null;
            Properties p = new Properties();
            try (var in = Files.newInputStream(f)) {
                p.load(in);
            }
            if (compare(p.getProperty("tag", "0"), currentVersion()) > 0) return null; // swap has not happened yet
            Files.delete(f);
            return new Release(p.getProperty("tag"), p.getProperty("notes", ""), List.of());
        } catch (Exception e) {
            return null;
        }
    }

    private static String fileName(String url) {
        return URLDecoder.decode(url.substring(url.lastIndexOf('/') + 1), StandardCharsets.UTF_8);
    }

    private static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        return m.find() ? unescape(m.group(1)) : null;
    }

    private static String unescape(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\' || i + 1 >= s.length()) {
                b.append(c);
                continue;
            }
            char n = s.charAt(++i);
            switch (n) {
                case 'n' -> b.append('\n');
                case 'r' -> { }
                case 't' -> b.append('\t');
                case 'b' -> b.append('\b');
                case 'f' -> b.append('\f');
                case 'u' -> {
                    b.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                    i += 4;
                }
                default -> b.append(n); // \" \\ \/
            }
        }
        return b.toString();
    }

    /** Numeric dotted parts of a version ("v0.1.1-alpha" -> 0.1.1; "0.1.0+1.21.1" -> 0.1.0), compared left to right. */
    private static int compare(String a, String b) {
        Pattern num = Pattern.compile("\\d+");
        Matcher x = num.matcher(a.split("[+-]")[0]), y = num.matcher(b.split("[+-]")[0]);
        while (true) {
            boolean hx = x.find(), hy = y.find();
            if (!hx && !hy) return 0;
            int c = Integer.compare(hx ? Integer.parseInt(x.group()) : 0, hy ? Integer.parseInt(y.group()) : 0);
            if (c != 0) return c;
        }
    }

    /** Runs as the JVM exits: a detached script waits a few seconds, deletes the old jar and renames the download. */
    private static void swap(Path old, Path part, Path target) {
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                Path bat = Files.createTempFile("nihao-update", ".bat");
                Files.writeString(bat, "@echo off\r\nping -n 5 127.0.0.1 >nul\r\ndel /f /q \"" + old + "\"\r\nmove /y \"" + part + "\" \"" + target + "\"\r\ndel \"%~f0\"\r\n");
                new ProcessBuilder("cmd", "/c", "start", "\"\"", "/min", bat.toString()).start();
            } else {
                new ProcessBuilder("sh", "-c", "sleep 4; rm -f '" + old + "'; mv '" + part + "' '" + target + "'").start();
            }
        } catch (Exception ignored) {
            // update stays as a .part file; the user can click Update again next start
        }
    }
}
