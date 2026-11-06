package com.nihao.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The mod's flat button for vanilla screens (title, pause, options): a real {@link Button}, so vanilla routes the clicks itself. */
public final class NiButton extends Button {
    private final ResourceLocation icon;

    /** A text button (icon == null) or a 16x16 icon button. */
    public NiButton(int x, int y, int w, int h, String label, ResourceLocation icon, String tooltip, Runnable action) {
        super(x, y, w, h, Component.literal(label), b -> action.run(), DEFAULT_NARRATION);
        this.icon = icon;
        if (tooltip != null) setTooltip(Tooltip.create(Component.literal(tooltip)));
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float tick) {
        Ui.button(g, getX(), getY(), width, height, active && isHoveredOrFocused(), false, active);
        if (icon != null) Compat.blit(g, icon, getX() + (width - 16) / 2, getY() + (height - 16) / 2, 16, 16, 16, 16);
        else g.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, active ? 0xFFE0E0E8 : 0xFF777777);
    }
}
