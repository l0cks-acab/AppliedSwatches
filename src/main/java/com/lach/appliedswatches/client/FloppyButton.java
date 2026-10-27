package com.lach.appliedswatches.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Square button with a floppy disk icon; opens the file menu. */
public class FloppyButton extends Button {
    public FloppyButton(int x, int y, OnPress onPress) {
        super(x, y, 16, 16, Component.translatable("gui.appliedswatches.file"), onPress, DEFAULT_NARRATION);
        setTooltip(tooltip());
    }

    public static Tooltip tooltip() {
        return Tooltip.create(Component.translatable("gui.appliedswatches.file.tip"));
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
        Component msg = getMessage();
        setMessage(Component.empty());
        super.renderWidget(g, mx, my, partial);
        setMessage(msg);
        int x = getX() + 3, y = getY() + 3;
        int body = active ? 0xFF3A4FA8 : 0xFF555A66;
        // Body with a clipped top-right corner.
        g.fill(x, y, x + 9, y + 10, body);
        g.fill(x + 9, y + 1, x + 10, y + 10, body);
        // Metal shutter with its slot.
        g.fill(x + 2, y, x + 8, y + 3, 0xFFC9CED8);
        g.fill(x + 5, y + 1, x + 6, y + 3, 0xFF3A4FA8);
        // Label.
        g.fill(x + 1, y + 5, x + 9, y + 10, 0xFFF2F2F2);
        g.fill(x + 2, y + 6, x + 8, y + 7, 0xFFD83A3A);
        g.fill(x + 2, y + 8, x + 6, y + 9, 0xFF9AA3AF);
    }
}
