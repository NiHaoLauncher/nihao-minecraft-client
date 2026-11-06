package com.nihao.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** General NiHaoClient settings (opened from the game's Options). */
public class NiHaoSettingsScreen extends MouseScreen {
    private static final int ROW = 34, ROW_W = 280;

    /**
     * One setting: the button shows label() and highlighted() paints it in the accent colour; a click calls click().
     * gated: the option only exists on versions with the zoom feature (see {@code supported} below).
     */
    private record Row(String titleKey, String hintKey, Supplier<String> label, BooleanSupplier highlighted, Runnable click, boolean gated, int buttonW) {}

    private static Row toggle(String key, BooleanSupplier get, Consumer<Boolean> set) {
        return new Row(key, key + ".hint", () -> Lang.t(get.getAsBoolean() ? "on" : "off"), get, () -> set.accept(!get.getAsBoolean()), true, 48);
    }

    private static final Row[] ROWS = {
            new Row("settings.skins", "settings.skins.hint", () -> Lang.t(NiHaoConfig.skins.key), () -> NiHaoConfig.skins != NiHaoConfig.SkinMode.OFF,
                    () -> NiHaoConfig.skins = NiHaoConfig.skins.next(), false, 96),
            toggle("settings.zoom", () -> NiHaoConfig.zoom, v -> NiHaoConfig.zoom = v),
    };

    private final Screen parent;
//#if ZOOM
    private final boolean supported = true; // zoom
//#else
    private final boolean supported = false; // zoom only exists on versions with the `zoom` feature flag
//#endif

    public NiHaoSettingsScreen(Screen parent) {
        super(Component.literal("NiHaoClient"));
        this.parent = parent;
    }

    private int left() { return (width - ROW_W) / 2; }

    private int top() { return 40; }

    @Override
    protected void init() {
        for (int i = 0; i < ROWS.length; i++) {
            Row r = ROWS[i];
            FlatButton button = new FlatButton(left() + ROW_W - 6 - r.buttonW(), top() + i * ROW + 6, r.buttonW(), 20, "", () -> {
                r.click().run();
                NiHaoConfig.save();
            });
            button.active = !r.gated() || supported;
            addRenderableWidget(button);
        }
        addRenderableWidget(new FlatButton(width / 2 - 50, height - 28, 100, 20, Lang.t("done"), this::onClose));
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
    boolean onScroll(double x, double y, double amount) {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // the buttons first: their state is drawn by the widgets in super.render
        int n = 0;
        for (var c : children()) {
            if (c instanceof FlatButton b && n < ROWS.length) {
                Row r = ROWS[n++];
                b.setLabel(r.label().get());
                b.selected = r.highlighted().getAsBoolean() && (!r.gated() || supported);
            }
        }
        super.render(g, mouseX, mouseY, partial);
        g.drawCenteredString(font, "NiHaoClient", width / 2, 14, 0xFFFFFFFF);
        Ui.panel(g, left() - 8, top() - 6, left() + ROW_W + 8, top() + ROWS.length * ROW + 2);
        for (int i = 0; i < ROWS.length; i++) {
            int y = top() + i * ROW;
            g.drawString(font, Lang.t(ROWS[i].titleKey()), left(), y + 3, 0xFFFFFFFF);
            // the hint wraps to two lines: translations are longer than the English text
            var lines = font.split(Component.literal(Lang.t(ROWS[i].hintKey())), ROW_W - ROWS[i].buttonW() - 14);
            for (int l = 0; l < Math.min(2, lines.size()); l++) g.drawString(font, lines.get(l), left(), y + 14 + l * 10, 0xFF9090A0);
        }
        if (!supported) g.drawCenteredString(font, Lang.t("settings.unsupported"), width / 2, top() + ROWS.length * ROW + 14, 0xFFE0A040);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
