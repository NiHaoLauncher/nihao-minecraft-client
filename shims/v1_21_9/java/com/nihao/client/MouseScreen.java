package com.nihao.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Mouse input hooks with one signature for every MC version (the Screen overrides changed in 1.21.9). */
abstract class MouseScreen extends Screen {
    protected MouseScreen(Component title) {
        super(title);
    }

    abstract boolean onClick(double x, double y, int button);

    abstract boolean onDrag(double x, double y, int button, double dx, double dy);

    abstract boolean onScroll(double x, double y, double amount);

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        return onClick(e.x(), e.y(), e.button()) || super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        return onDrag(e.x(), e.y(), e.button(), dx, dy) || super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        return onScroll(x, y, scrollY) || super.mouseScrolled(x, y, scrollX, scrollY);
    }
}
