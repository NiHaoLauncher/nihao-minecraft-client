package com.nihao.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Ely.by Yggdrasil-style auth (authserver.ely.by). Plain string JSON on purpose: runs in preLaunch, before Minecraft's libraries are safe to use. */
final class ElyAuth {
    record Profile(String name, String uuid, String accessToken, String clientToken) {}

    private static final String API = "https://authserver.ely.by/auth/";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    private ElyAuth() {}

    /** Password for 2FA accounts is "password:code". Throws with Ely.by's message on failure. */
    static Profile login(String user, String password, String clientToken) throws Exception {
        return parse(post("authenticate", "{\"username\":" + q(user) + ",\"password\":" + q(password)
                + ",\"clientToken\":" + q(clientToken) + ",\"requestUser\":false}"), clientToken);
    }

    static boolean validate(String accessToken) {
        try {
            return send("validate", "{\"accessToken\":" + q(accessToken) + "}").statusCode() == 204;
        } catch (Exception e) {
            return false;
        }
    }

    /** New profile with a fresh access token, or null if the old one can no longer be refreshed. */
    static Profile refresh(Profile p) {
        try {
            return parse(post("refresh", "{\"accessToken\":" + q(p.accessToken()) + ",\"clientToken\":" + q(p.clientToken()) + "}"), p.clientToken());
        } catch (Exception e) {
            return null;
        }
    }

    private static Profile parse(String json, String clientToken) {
        String sel = json.substring(Math.max(0, json.indexOf("selectedProfile")));
        return new Profile(field(sel, "name"), field(sel, "id"), field(json, "accessToken"), clientToken);
    }

    private static String post(String method, String body) throws Exception {
        HttpResponse<String> r = send(method, body);
        if (r.statusCode() != 200) {
            String msg = field(r.body(), "errorMessage");
            throw new Exception(msg != null ? msg : "HTTP " + r.statusCode());
        }
        return r.body();
    }

    private static HttpResponse<String> send(String method, String body) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create(API + method)).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String q(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
