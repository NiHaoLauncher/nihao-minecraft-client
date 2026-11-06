package com.nihao.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * The mod's own button: flat, dark, with an accent colour on hover and when selected (tabs, state chips). Not a vanilla Button, so it
 * does not use the vanilla button texture. Clicks are routed by the screen through {@link Ui#click}.
 */
public final class FlatButton extends AbstractWidget {
    private final Runnable action;
    /** Drawn in the accent colour (the current tab / state). */
    public boolean selected;

    public FlatButton(int x, int y, int w, int h, String label, Runnable action) {
        super(x, y, w, h, Component.literal(label));
        this.action = action;
    }

    public void run() {
        if (active) action.run();
    }

    public void setLabel(String label) {
        setMessage(Component.literal(label));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {}

    @Override
    public void renderWidget(GuiGraphics g, int mx, int my, float partial) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean hover = active && isHoveredOrFocused();
        Ui.button(g, x, y, w, h, hover, selected, active);
        var font = Minecraft.getInstance().font;
        g.drawCenteredString(font, getMessage(), x + w / 2, y + (h - 8) / 2, !active ? 0xFF777777 : selected ? 0xFFFFFFFF : 0xFFE0E0E8);
    }
}
