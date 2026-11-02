package com.lach.appliedswatches.client;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import com.lach.appliedswatches.ApplicatorData;
import com.lach.appliedswatches.ApplicatorItem;
import com.lach.appliedswatches.PlaceMode;
import com.lach.appliedswatches.net.ApplicatorConfigPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Shift + right-click screen: pick the block (or Random), the placement mode and its size. */
public class ApplicatorScreen extends Screen {
    private static final int W = 196, COLS = 9, CELL = 18;
    private static final int PURPLE = 0xFF9A6AF0;

    private final InteractionHand hand;
    private List<Item> blocks = List.of();
    private String paletteName = "";
    private int selected;
    private PlaceMode mode;
    private int size;
    private int left, top, h, gridY, modeY;
    private final List<Button> modeButtons = new ArrayList<>();
    private Button minus, plus;

    public ApplicatorScreen(InteractionHand hand) {
        super(Component.translatable("item.appliedswatches.swatch_applicator"));
        this.hand = hand;
    }

    private ItemStack stack() {
        return minecraft == null || minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getItemInHand(hand);
    }

    @Override
    protected void init() {
        ApplicatorData d = ApplicatorItem.data(stack());
        blocks = d.blocks();
        paletteName = d.name();
        selected = d.selected();
        mode = d.mode();
        size = d.size();
        int rows = Math.max(1, (blocks.size() + 1 + COLS - 1) / COLS);
        gridY = 30;
        modeY = gridY + rows * CELL + 12;
        h = modeY + 82;
        left = (width - W) / 2;
        top = (height - h) / 2;

        modeButtons.clear();
        PlaceMode[] modes = PlaceMode.values();
        int bw = (W - 16 - (modes.length - 1) * 2) / modes.length;
        for (int i = 0; i < modes.length; i++) {
            PlaceMode m = modes[i];
            modeButtons.add(addRenderableWidget(Button.builder(Component.translatable(m.key()), b -> {
                mode = m;
                size = m.clampSize(size);
                sync();
            }).bounds(left + 8 + i * (bw + 2), top + modeY, bw, 16).build()));
        }
        minus = addRenderableWidget(Button.builder(Component.literal("−"), b -> {
            size = mode.clampSize(size - mode.step);
            sync();
        }).bounds(left + 8, top + modeY + 22, 20, 16).build());
        plus = addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            size = mode.clampSize(size + mode.step);
            sync();
        }).bounds(left + W - 28, top + modeY + 22, 20, 16).build());
        refreshButtons();
    }

    private void refreshButtons() {
        PlaceMode[] modes = PlaceMode.values();
        for (int i = 0; i < modeButtons.size(); i++) modeButtons.get(i).active = modes[i] != mode;
        boolean sized = mode != PlaceMode.SINGLE;
        minus.visible = plus.visible = sized;
        minus.active = size > mode.minSize;
        plus.active = size < mode.maxSize;
    }

    private void sync() {
        refreshButtons();
        ClientPacketDistributor.sendToServer(new ApplicatorConfigPayload(hand == InteractionHand.OFF_HAND, selected, mode, size));
    }

    private Component sizeLabel() {
        return switch (mode) {
            case SINGLE -> Component.translatable("gui.appliedswatches.applicator.one");
            case LINE -> Component.translatable("gui.appliedswatches.applicator.length", size);
            case WAND -> Component.translatable("gui.appliedswatches.applicator.reach", size, size);
            case SQUARE -> Component.translatable("gui.appliedswatches.applicator.square", size, size);
        };
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractBackground(g, mx, my, partial);
        g.fill(left, top, left + W, top + h, 0xFF2B2F36);
        g.fill(left + 1, top + 1, left + W - 1, top + h - 1, 0xFF3C424B);
        g.fill(left + 1, top + 1, left + W - 1, top + 2, 0xFF59616D);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractRenderState(g, mx, my, partial);
        g.text(font, title, left + 8, top + 7, 0xFFE4E8EE, false);
        if (!paletteName.isEmpty())
            g.text(font, font.plainSubstrByWidth(paletteName, W - 16), left + 8, top + 18, 0xFF9AA3AF, false);

        int gx = left + 8, gy = top + gridY;
        int hover = cellAt(mx, my);
        for (int i = 0; i <= blocks.size(); i++) {
            int cx = gx + (i % COLS) * CELL, cy = gy + (i / COLS) * CELL;
            boolean random = i == blocks.size();
            boolean sel = random ? selected == ApplicatorData.RANDOM : selected == i;
            g.fill(cx, cy, cx + 18, cy + 18, sel ? PURPLE : 0xFF1E2126);
            g.fill(cx + 1, cy + 1, cx + 17, cy + 17, i == hover ? 0xFF4A5A86 : 0xFF262A30);
            if (random) g.centeredText(font, "?", cx + 9, cy + 5, sel ? 0xFFE0D0FF : 0xFFB8C0CC);
            else g.item(new ItemStack(blocks.get(i)), cx + 1, cy + 1);
        }
        if (blocks.isEmpty())
            g.text(font, Component.translatable("item.appliedswatches.swatch_applicator.tip"), gx + 22, gy + 5, 0xFF9AA3AF, false);

        g.centeredText(font, sizeLabel(), left + W / 2, top + modeY + 26, 0xFFE4E8EE);
        Component hint = Component.translatable(mode.key() + ".tip");
        int y = top + modeY + 46;
        for (var line : font.split(hint, W - 16)) {
            g.text(font, line, left + 8, y, 0xFF9AA3AF, false);
            y += 10;
        }

        if (hover >= 0) {
            Component tip = hover == blocks.size() ? Component.translatable("item.appliedswatches.swatch_applicator.random")
                    : new ItemStack(blocks.get(hover)).getHoverName();
            g.setTooltipForNextFrame(font, tip.copy().withStyle(ChatFormatting.WHITE), mx, my);
        }
    }

    private int cellAt(double mx, double my) {
        int gx = left + 8, gy = top + gridY;
        if (mx < gx || my < gy || mx >= gx + COLS * CELL) return -1;
        int i = ((int) (my - gy) / CELL) * COLS + (int) (mx - gx) / CELL;
        return i <= blocks.size() && !blocks.isEmpty() ? i : -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        int i = cellAt(mx, my);
        if (button == 0 && i >= 0) {
            selected = i == blocks.size() ? ApplicatorData.RANDOM : i;
            sync();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (minecraft != null && minecraft.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (!(stack().getItem() instanceof ApplicatorItem)) onClose();
    }
}
