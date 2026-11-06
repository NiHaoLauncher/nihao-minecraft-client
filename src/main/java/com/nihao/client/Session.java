package com.nihao.client;

/** Account type: offline, elyby or microsoft. Chosen by the launcher flag -Dnihao.session, a saved choice, or the start-up window. */
public final class Session {
    public static final String PROP = "nihao.session", TOKEN_ENV = "NIHAO_TOKEN";

    private static String type = "offline";
    private static String token;

    private Session() {}

    public static String type() {
        return type;
    }

    public static String token() {
        return token;
    }

    static void set(String type, String token) {
        Session.type = type;
        Session.token = token;
    }

    /** Applies -Dnihao.session=offline|elyby|microsoft (elyby token from env NIHAO_TOKEN); false if absent or unknown. */
    static boolean fromFlag() {
        String v = System.getProperty(PROP);
        if (v == null) return false;
        v = v.trim().toLowerCase();
        if (!v.equals("offline") && !v.equals("elyby") && !v.equals("microsoft")) return false;
        set(v, System.getenv(TOKEN_ENV));
        return true;
    }
}
