package com.nihao.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shows release notes written in Markdown: headings, bullets, tables (as text rows), **bold**, *italic*, `code`, links (text only)
 * and images (PNG, downloaded in the background). Anything else is shown as plain text.
 */
public class ChangelogScreen extends MouseScreen {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final Pattern INLINE = Pattern.compile("\\*\\*(.+?)\\*\\*|`([^`]+)`|\\[([^\\]]+)\\]\\(([^)]+)\\)|\\*([^*]+)\\*");
    private static final Pattern IMAGE = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)\\s]+)[^)]*\\)|<img[^>]*src=\"([^\"]+)\"[^>]*>");
    private static final int GOLD = 0xFFAA00, MAX_IMG_H = 160;

    private static final class Img {
        String url, alt;
        ResourceLocation id;
        int w, h;
        boolean loaded;
    }

    private record Block(Component text, int indent, int before, Img img) {}

    private record Row(FormattedCharSequence text, Img img, int x, int y, int w, int h) {}

    private final Screen parent;
    private final String heading;
    private final List<Block> blocks = new ArrayList<>();
    private final List<Img> images = new ArrayList<>();
    private List<Row> rows = new ArrayList<>();
    private int total;
    private boolean dirty = true;
    private double scroll;

    public ChangelogScreen(Screen parent, String tag, String markdown) {
        super(Component.translatable("nihao.changelog.title"));
        this.parent = parent;
        this.heading = Lang.t("changelog.heading", tag);
        parse(markdown);
    }

    private void parse(String md) {
        boolean code = false;
        int gap = 0;
        for (String raw : md.replace("✅", "+").replace("❌", "-").replace("\t", "  ").split("\n")) {
            String line = raw.stripTrailing();
            if (line.stripLeading().startsWith("```")) {
                code = !code;
                continue;
            }
            if (code) {
                blocks.add(new Block(Component.literal(line).withStyle(Style.EMPTY.withColor(0xAAAAAA)), 8, 0, null));
                continue;
            }
            List<Img> found = new ArrayList<>();
            Matcher im = IMAGE.matcher(line);
            while (im.find()) {
                Img i = new Img();
                i.alt = im.group(1) != null ? im.group(1) : "image";
                i.url = im.group(2) != null ? im.group(2) : im.group(3);
                found.add(i);
            }
            line = IMAGE.matcher(line).replaceAll("").replaceAll("<[^>]+>", "").stripTrailing();
            String t = line.strip();
            if (t.isEmpty()) {
                if (found.isEmpty()) gap = 4;
            } else if (t.matches("[-*_]{3,}")) {
                gap = 6;
            } else if (t.startsWith("|")) {
                if (!t.matches("[|\\s:-]+")) { // skip the |---|---| separator
                    blocks.add(new Block(inline(String.join("  |  ", java.util.Arrays.stream(t.replaceAll("^\\||\\|$", "").split("\\|")).map(String::strip).toList()), Style.EMPTY), 4, gap, null));
                    gap = 0;
                }
            } else if (t.startsWith("#")) {
                blocks.add(new Block(inline(t.replaceFirst("^#+\\s*", ""), Style.EMPTY.withBold(true).withColor(GOLD)), 0, 6, null));
                gap = 2;
            } else if (t.matches("[-*+]\\s+.*")) {
                int indent = 6 + (line.length() - line.stripLeading().length()) * 3;
                blocks.add(new Block(Component.literal("• ").append(inline(t.substring(2).strip(), Style.EMPTY)), indent, gap, null));
                gap = 0;
            } else if (t.startsWith(">")) {
                blocks.add(new Block(inline(t.replaceFirst("^>\\s*", ""), Style.EMPTY.withItalic(true).withColor(0xAAAAAA)), 6, gap, null));
                gap = 0;
            } else {
                blocks.add(new Block(inline(t, Style.EMPTY), 0, gap, null));
                gap = 0;
            }
            for (Img i : found) {
                i.id = Compat.id("nihao", "changelog/img" + images.size());
                images.add(i);
                blocks.add(new Block(null, 0, 4, i));
                load(i);
            }
        }
    }

    private static MutableComponent inline(String s, Style base) {
        MutableComponent out = Component.empty();
        Matcher m = INLINE.matcher(s);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) out.append(Component.literal(s.substring(last, m.start())).withStyle(base));
            if (m.group(1) != null) out.append(Component.literal(m.group(1)).withStyle(base.withBold(true)));
            else if (m.group(2) != null) out.append(Component.literal(m.group(2)).withStyle(base.withColor(0xAAAAAA)));
            else if (m.group(3) != null) out.append(Component.literal(m.group(3)).withStyle(base.withColor(0x5555FF).withUnderlined(true)));
            else out.append(Component.literal(m.group(5)).withStyle(base.withItalic(true)));
            last = m.end();
        }
        if (last < s.length()) out.append(Component.literal(s.substring(last)).withStyle(base));
        return out;
    }

    private void load(Img i) {
        if (!i.url.startsWith("http")) return; // relative links cannot be resolved
        CompletableFuture.supplyAsync(() -> {
            try {
                return HTTP.send(HttpRequest.newBuilder(URI.create(i.url)).timeout(Duration.ofSeconds(15)).build(),
                        HttpResponse.BodyHandlers.ofByteArray()).body();
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(bytes -> Minecraft.getInstance().execute(() -> {
            if (bytes == null) return;
            try {
                NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes)); // PNG only; others keep the alt text
                i.w = img.getWidth();
                i.h = img.getHeight();
                Minecraft.getInstance().getTextureManager().register(i.id, Compat.dynamicTexture(img));
                i.loaded = true;
                dirty = true;
            } catch (Exception ignored) {
                // not a PNG
            }
        }));
    }

    @Override
    protected void init() {
        dirty = true;
        addRenderableWidget(new FlatButton(width / 2 - 50, height - 28, 100, 20, Lang.t("close"), this::onClose));
    }

    private int top() { return 28; }

    private int bottom() { return height - 34; }

    private void layout() {
        int cw = Math.min(width - 40, 420), x0 = (width - cw) / 2, y = 0;
        List<Row> out = new ArrayList<>();
        for (Block b : blocks) {
            y += b.before();
            if (b.img() != null && b.img().loaded) {
                Img i = b.img();
                double s = Math.min(1.0, Math.min((double) cw / i.w, (double) MAX_IMG_H / i.h));
                int w = (int) (i.w * s), h = (int) (i.h * s);
                out.add(new Row(null, i, x0, y, w, h));
                y += h;
            } else {
                Component c = b.text() != null ? b.text() : Component.literal("[" + b.img().alt + "]").withStyle(Style.EMPTY.withColor(0xAAAAAA));
                for (FormattedCharSequence seq : font.split(c, cw - b.indent())) {
                    out.add(new Row(seq, null, x0 + b.indent(), y, 0, 0));
                    y += font.lineHeight + 1;
                }
            }
        }
        rows = out;
        total = y;
        scroll = Math.max(0, Math.min(scroll, total - (bottom() - top() - 8)));
    }

    @Override
    boolean onScroll(double x, double y, double amount) {
        scroll = Math.max(0, Math.min(scroll - amount * 20, Math.max(0, total - (bottom() - top() - 8))));
        return true;
    }

    @Override
    boolean onClick(double x, double y, int button) {
        return Ui.click(this, x, y, button);
    }

    @Override
    boolean onDrag(double x, double y, int button, double dx, double dy) {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        if (dirty) {
            layout();
            dirty = false;
        }
        g.drawCenteredString(font, heading, width / 2, 10, 0xFFFFFFFF);
        g.enableScissor(0, top(), width, bottom());
        for (Row r : rows) {
            int y = top() + 4 + r.y() - (int) scroll;
            if (y > bottom() || y + Math.max(r.h(), font.lineHeight) < top()) continue;
            if (r.img() != null) Compat.blitUv(g, r.img().id, r.x(), y, r.w(), r.h(), 0, 0, r.img().w, r.img().h, r.img().w, r.img().h);
            else g.drawString(font, r.text(), r.x(), y, 0xFFFFFFFF);
        }
        g.disableScissor();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        for (Img i : images) if (i.loaded) minecraft.getTextureManager().release(i.id);
    }
}
