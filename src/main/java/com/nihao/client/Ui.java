package com.nihao.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

/** Look of the mod's own screens: colours, flat panels with rounded corners, a scroll bar, and click routing for {@link FlatButton}. */
public final class Ui {
    public static final int ACCENT = 0xFF4A90E2, ACCENT_SOFT = 0xFF6FA8E8, ACCENT_DARK = 0xFF2F5C94;
    public static final int PANEL = 0xC0101016, PANEL_BORDER = 0xFF3A3A48, CARD = 0xFF1C1C22;

    private Ui() {}

    /** A rectangle with its four corner pixels cut off. */
    public static void rounded(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0 + 1, y0, x1 - 1, y1, color);
        g.fill(x0, y0 + 1, x1, y1 - 1, color);
    }

    /** The flat button look shared by {@link FlatButton} and {@link NiButton}. */
    public static void button(GuiGraphics g, int x, int y, int w, int h, boolean hover, boolean selected, boolean active) {
        rounded(g, x, y, x + w, y + h, selected ? ACCENT : hover ? ACCENT_SOFT : 0xFF454555);
        rounded(g, x + 1, y + 1, x + w - 1, y + h - 1, !active ? 0xFF202026 : selected ? ACCENT_DARK : hover ? 0xFF343A4C : 0xFF26262E);
    }

    /** A dark translucent panel with a thin border. */
    public static void panel(GuiGraphics g, int x0, int y0, int x1, int y1) {
        rounded(g, x0 - 1, y0 - 1, x1 + 1, y1 + 1, PANEL_BORDER);
        rounded(g, x0, y0, x1, y1, PANEL);
    }

    /** Vertical scroll bar on the right edge of a panel (does nothing if everything fits). */
    public static void scrollbar(GuiGraphics g, int x, int top, int bottom, double scroll, double content, boolean active) {
        double view = bottom - top;
        if (content <= view) return;
        g.fill(x, top, x + 3, bottom, 0x40FFFFFF);
        int h = (int) Math.max(16, view * view / content);
        int y = top + (int) ((view - h) * scroll / (content - view));
        g.fill(x, y, x + 3, y + h, active ? Ui.ACCENT_SOFT : 0xFFB0B0C0);
    }

    /** Runs the FlatButton under the mouse, if any (screens call this first in their click handler). */
    public static boolean click(Screen screen, double x, double y, int button) {
        if (button != 0) return false;
        for (GuiEventListener c : screen.children()) {
            if (c instanceof FlatButton b && b.active && b.visible && b.isMouseOver(x, y)) {
                b.run();
                return true;
            }
        }
        return false;
    }
}
