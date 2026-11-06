package com.nihao.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Decides the session before Minecraft builds its user: launcher flag, else the saved choice, else a window. Called from
 * MainMixin with the launch arguments and returns them, possibly rewritten (Ely.by sign-in replaces username/uuid/accessToken).
 * Must not touch Minecraft classes.
 */
public final class SignIn {
    /** Launch arguments are "--key value" pairs; this edits a copy and returns it. */
    public static String[] run(String[] argv) {
        System.out.println("[NiHaoClient] sign-in started");
        Updater.start();
        List<String> list = new ArrayList<>(List.of(argv));
        Arguments args = new Arguments(list);
        try {
            if (Session.fromFlag()) return argv;
            Path file = Platform.gameDir().resolve("session.nihao");
            if (restore(file, args)) return list.toArray(new String[0]);
            String name = args.get("username");
            java.nio.file.Path cp = Platform.modJar();
            if (cp == null) cp = java.nio.file.Path.of(LoginWindow.class.getProtectionDomain().getCodeSource().getLocation().toURI()); // dev environment
            String licensed = licensedName(args);
            Path prefs = Platform.gameDir().resolve("nihao").resolve("login.properties"); // the state of the "Remember" checkbox
            Properties pref = new Properties();
            if (Files.exists(prefs)) try (var in = Files.newInputStream(prefs)) { pref.load(in); } catch (Exception ignored) { }
            LoginWindow.Result r = LoginWindow.show(name == null ? "Player" : name, licensed, cp, !"false".equals(pref.getProperty("remember")),
                    LoginWindow.language(Platform.gameDir()));
            if (r == LoginWindow.CLOSED) { // closed without a choice: the game does not start
                System.out.println("[NiHaoClient] sign-in window closed, exiting");
                System.exit(0);
            }
            if (r != null) {
                pref.setProperty("remember", String.valueOf(r.remember()));
                try {
                    Files.createDirectories(prefs.getParent());
                    try (var out = Files.newOutputStream(prefs)) { pref.store(out, "NiHaoClient sign-in window"); }
                } catch (Exception ignored) {
                    // the checkbox simply starts ticked next time
                }
            }
            if (r == null) { // the window could not be shown (no display): the game runs with the account it was started with
                if (licensed != null) Session.set("microsoft", null);
                return argv;
            }
            apply(r.type(), r.profile(), args);
            if (r.remember()) save(file, r.type(), r.profile());
        } catch (Throwable t) {
            System.err.println("[NiHaoClient] sign-in skipped: " + t);
        }
        return list.toArray(new String[0]);
    }

    /** "--key value" view over the launch arguments. */
    private static final class Arguments {
        private final List<String> list;

        Arguments(List<String> list) {
            this.list = list;
        }

        String get(String key) {
            int i = list.indexOf("--" + key);
            return i >= 0 && i + 1 < list.size() ? list.get(i + 1) : null;
        }

        void put(String key, String value) {
            int i = list.indexOf("--" + key);
            if (i >= 0 && i + 1 < list.size()) {
                list.set(i + 1, value);
            } else {
                list.add("--" + key);
                list.add(value);
            }
        }
    }

    private static boolean restore(Path file, Arguments args) throws Exception {
        if (!Files.exists(file)) return false;
        Properties p = new Properties();
        try (var in = Files.newInputStream(file)) {
            p.load(in);
        }
        String type = p.getProperty("type", "");
        switch (type) {
            case "offline":
                apply(type, null, args);
                return true;
            case "microsoft":
                if (licensedName(args) == null) return false; // no longer started from a licensed account
                apply(type, null, args);
                return true;
            case "elyby":
                ElyAuth.Profile pr = new ElyAuth.Profile(p.getProperty("name"), p.getProperty("uuid"), p.getProperty("accessToken"), p.getProperty("clientToken"));
                if (!ElyAuth.validate(pr.accessToken())) {
                    pr = ElyAuth.refresh(pr);
                    if (pr == null) return false;
                    save(file, type, pr);
                }
                apply(type, pr, args);
                return true;
            default:
                return false;
        }
    }

    private static void apply(String type, ElyAuth.Profile p, Arguments args) {
        Session.set(type, p == null ? null : p.accessToken());
        if (p == null) return;
        args.put("username", p.name());
        args.put("uuid", p.uuid());
        args.put("accessToken", p.accessToken());
        args.put("userType", "legacy");
    }

    private static void save(Path file, String type, ElyAuth.Profile p) throws Exception {
        Properties props = new Properties();
        props.setProperty("type", type);
        if (p != null) {
            props.setProperty("name", p.name());
            props.setProperty("uuid", p.uuid());
            props.setProperty("accessToken", p.accessToken());
            props.setProperty("clientToken", p.clientToken());
        }
        try (var out = Files.newOutputStream(file)) {
            props.store(out, "NiHaoClient sign-in. Contains an access token: do not share this file.");
        }
    }

    /** The profile name if the game was started from a licensed account (launch token accepted by Mojang), else null. */
    private static String licensedName(Arguments args) {
        String token = args.get("accessToken"), userType = args.get("userType");
        if (token == null || token.length() < 20 || userType == null || !userType.equalsIgnoreCase("msa")) return null;
        try {
            HttpResponse<String> r = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build().send(
                    HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                            .timeout(Duration.ofSeconds(8)).header("Authorization", "Bearer " + token).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) return null;
            var m = java.util.regex.Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"").matcher(r.body());
            return m.find() ? m.group(1) : args.get("username");
        } catch (Exception e) {
            return null;
        }
    }
}
