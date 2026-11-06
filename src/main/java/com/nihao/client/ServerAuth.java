package com.nihao.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects a server's authentication by starting a login: an Encryption Request means online-mode, an immediate Login Success
 * means online-mode=false. The protocol cannot tell Mojang from Ely.by, so hosts listed in config/nihao-elyby-servers.txt
 * (one per line) are shown as Ely.by and other online servers as "Mojang/Ely.by".
 */
public final class ServerAuth {
    private record Auth(String text, int color, String... icons) {}

    private static final Auth NONE = new Auth(null, 0);
    private static final Map<String, Auth> CACHE = new ConcurrentHashMap<>();

    private ServerAuth() {}

    private static final Map<String, ResourceLocation> ICONS = new ConcurrentHashMap<>();

    /** Draws a 32x32 auth icon (mojang, elyby, microsoft, offline) scaled to size. */
    static void icon(GuiGraphics g, String name, int x, int y, int size) {
        ResourceLocation tex = ICONS.computeIfAbsent(name, n -> Compat.id("nihao", "textures/gui/auth/" + n + ".png"));
        Compat.blitUv(g, tex, x, y, size, size, 0, 0, 32, 32, 32, 32);
    }

    /** Draws the auth icons as a vertical stack at x (right of the server row), y = row top; the probe runs once per address in the background. */
    public static void draw(GuiGraphics g, ServerData data, int x, int y, int mx, int my) {
        Auth a = CACHE.get(data.ip);
        if (a == null) {
            CACHE.put(data.ip, NONE);
            String ip = data.ip;
            CompletableFuture.supplyAsync(() -> probe(ip)).thenAccept(r -> CACHE.put(ip, r));
            return;
        }
        if (a.text() == null) return;
        y += (32 - (a.icons().length * 14 - 2)) / 2; // centre the stack in the 32px row
        for (String i : a.icons()) {
            icon(g, i, x, y, 12);
            if (mx >= x && mx < x + 12 && my >= y && my < y + 12) Compat.tooltip(g, Component.literal(describe(i)), mx, my);
            y += 14;
        }
    }

    /** Re-detects every known server in the background (called when the server list screen is (re)opened, e.g. by Refresh or after adding a server). */
    public static void refresh() {
        for (String ip : CACHE.keySet()) CompletableFuture.supplyAsync(() -> probe(ip)).thenAccept(r -> CACHE.put(ip, r));
    }

    private static String describe(String icon) {
        return switch (icon) {
            case "mojang" -> Lang.t("server.mojang");
            case "elyby" -> Lang.t("server.elyby");
            default -> Lang.t("server.offline");
        };
    }

    private static Auth probe(String ip) {
        try {
            ServerAddress addr = ServerAddress.parseString(ip);
            InetSocketAddress sa = ServerNameResolver.DEFAULT.resolveAddress(addr)
                    .map(ResolvedServerAddress::asInetSocketAddress).orElse(null);
            if (sa == null) return NONE;
            int proto = serverProtocol(sa, addr); // a server of another version would refuse our own protocol number
            try (Socket s = new Socket()) {
                s.connect(sa, 5000);
                s.setSoTimeout(5000);
                DataOutputStream out = new DataOutputStream(s.getOutputStream());
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                DataOutputStream p = new DataOutputStream(b);
                handshake(p, proto, addr, 2); // next state: login
                send(out, b);
                b.reset();
                writeVarInt(p, 0); // login start; the layout depends on the version
                writeString(p, "NiHaoProbe");
                byte[] uuid = uuidBytes(UUID.nameUUIDFromBytes("OfflinePlayer:NiHaoProbe".getBytes(StandardCharsets.UTF_8)));
                if (proto >= 764) p.write(uuid); // 1.20.2+
                else if (proto >= 761) { p.writeBoolean(true); p.write(uuid); } // 1.19.3 - 1.20.1
                else if (proto >= 759) { p.writeBoolean(false); p.writeBoolean(true); p.write(uuid); } // 1.19 - 1.19.2
                send(out, b);
                DataInputStream in = new DataInputStream(s.getInputStream());
                readVarInt(in); // packet length
                int id = readVarInt(in);
                System.out.println("[NiHaoClient] auth probe " + ip + " protocol " + proto + " -> packet " + id);
                if (id == 1) return elyHost(addr.getHost())
                        ? new Auth("Ely.by", 0xFF55FFFF, "elyby") : new Auth("Mojang", 0xFF55FF55, "mojang");
                if (id == 2 || id == 3) return new Auth("online-mode=false", 0xFFFF5555, "offline"); // 3 = Set Compression, precedes Login Success
                return NONE; // disconnect (whitelist, version mismatch...): unknown
            }
        } catch (Exception e) {
            System.out.println("[NiHaoClient] auth probe " + ip + " failed: " + e);
            return NONE;
        }
    }

    private static void handshake(DataOutputStream p, int proto, ServerAddress addr, int state) throws java.io.IOException {
        writeVarInt(p, 0);
        writeVarInt(p, proto);
        writeString(p, addr.getHost());
        p.writeShort(addr.getPort());
        writeVarInt(p, state);
    }

    /** Status ping: the protocol number the server reports, or ours if it does not answer. */
    private static int serverProtocol(InetSocketAddress sa, ServerAddress addr) {
        try (Socket s = new Socket()) {
            s.connect(sa, 5000);
            s.setSoTimeout(5000);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream p = new DataOutputStream(b);
            handshake(p, Compat.protocol(), addr, 1);
            send(out, b);
            b.reset();
            writeVarInt(p, 0); // status request
            send(out, b);
            DataInputStream in = new DataInputStream(s.getInputStream());
            readVarInt(in);
            readVarInt(in);
            byte[] json = new byte[readVarInt(in)];
            in.readFully(json);
            return com.google.gson.JsonParser.parseString(new String(json, StandardCharsets.UTF_8)).getAsJsonObject()
                    .getAsJsonObject("version").get("protocol").getAsInt();
        } catch (Exception e) {
            return Compat.protocol();
        }
    }

    private static boolean elyHost(String host) {
        try {
            Path f = Platform.configDir().resolve("nihao-elyby-servers.txt");
            return Files.exists(f) && Files.readAllLines(f).stream().anyMatch(l -> l.trim().equalsIgnoreCase(host));
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] uuidBytes(UUID u) {
        return java.nio.ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();
    }

    private static void send(DataOutputStream out, ByteArrayOutputStream body) throws java.io.IOException {
        writeVarInt(out, body.size());
        body.writeTo(out);
        out.flush();
    }

    private static void writeString(DataOutputStream o, String s) throws java.io.IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(o, b.length);
        o.write(b);
    }

    private static void writeVarInt(DataOutputStream o, int v) throws java.io.IOException {
        while ((v & ~0x7F) != 0) {
            o.writeByte((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        o.writeByte(v);
    }

    private static int readVarInt(DataInputStream in) throws java.io.IOException {
        int v = 0, shift = 0, b;
        do {
            b = in.readUnsignedByte();
            v |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0 && shift < 35);
        return v;
    }
}
