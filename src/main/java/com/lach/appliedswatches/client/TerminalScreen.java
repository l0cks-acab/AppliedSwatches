package com.lach.appliedswatches.client;

import com.lach.appliedswatches.AppliedSwatches;
import com.lach.appliedswatches.SwatchMenu;
import com.lach.appliedswatches.SwatchTerminalBlockEntity;
import com.lach.appliedswatches.net.CandidatesPayload;
import com.lach.appliedswatches.net.ExportPayload;
import com.lach.appliedswatches.net.LoadApplicatorPayload;
import com.lach.appliedswatches.net.RefreshPayload;
import com.lach.appliedswatches.net.ReturnPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Shared terminal screen: the match grid, Export/Return/Refresh and the output slots. Subclasses supply the palette. */
public abstract class TerminalScreen extends AbstractContainerScreen<SwatchMenu> {
    protected static final int GRID_X = 188, GRID_Y = 18, COLS = 9, ROWS = 5, CELL = 18;
    protected static final int[] PER_ITEM = {1, 4, 8, 16, 32, 64};
    protected static final int MAX_TOLERANCE = 60;

    /** Settings are kept between screens and shared by both terminals. */
    protected static int tolerance = 20, perItemIdx = 5;
    protected static boolean fullOnly = true;

    protected record Match(Item item, long amount, int rgb, int swatch, float dist) {
    }

    /** One row of the floppy button's pop-up menu. */
    protected record MenuEntry(Component label, Runnable action) {
    }

    private static final int MENU_ROW = 14;

    private List<Item> stored = List.of();
    private long[] amounts = new long[0];
    private boolean networkOnline, loaded;
    protected int[] palette = new int[0];
    private final List<Match> matches = new ArrayList<>();
    private final List<Item> exports = new ArrayList<>();
    private int scroll;
    private boolean wasOnline;
    protected Button perItemBtn, fullBtn;
    private Button exportBtn, applicatorBtn;
    private AbstractSliderButton toleranceSlider;
    private List<MenuEntry> fileMenu = List.of();
    private boolean fileMenuOpen;
    private FloppyButton floppy;
    private int menuX, menuY, menuW;

    protected TerminalScreen(SwatchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 358;
        imageHeight = SwatchMenu.INV_Y + 83;
        inventoryLabelX = SwatchMenu.INV_X;
        inventoryLabelY = SwatchMenu.INV_Y - 11;
    }

    /** Palette for the current settings, as 0xRRGGBB, in export order. */
    protected abstract int[] buildPalette();

    /** Items skipped by the player. */
    protected abstract Set<Item> excluded();

    /** Name given to the palette when it is loaded into an applicator. */
    protected abstract String paletteName();

    /** Blocks that always count as a match when stored, whatever their colour. */
    protected Set<Item> preferred() {
        return Set.of();
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos, y = topPos;
        exportBtn = addRenderableWidget(Button.builder(Component.translatable("gui.appliedswatches.export"), b -> export())
                .bounds(x + GRID_X, y + 113, 60, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.appliedswatches.return"), b -> PacketDistributor.sendToServer(ReturnPayload.INSTANCE))
                .tooltip(Tooltip.create(Component.translatable("gui.appliedswatches.return.tip")))
                .bounds(x + GRID_X + 62, y + 113, 49, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.appliedswatches.refresh"), b -> refresh())
                .bounds(x + GRID_X + 113, y + 113, 49, 16).build());
        initPanel(x, y);
        update();
        refresh();
    }

    /** Adds the subclass's controls. */
    protected abstract void initPanel(int x, int y);

    /** Adds the shared tolerance / amount / shape controls as a column. */
    protected void addSettings(int x, int y, int w) {
        addToleranceSlider(x, y, w);
        perItemBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            perItemIdx = (perItemIdx + (hasShiftDown() ? PER_ITEM.length - 1 : 1)) % PER_ITEM.length;
            update();
        }).bounds(x, y + 17, w, 14).build());
        fullBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            fullOnly = !fullOnly;
            update();
        }).bounds(x, y + 34, w, 14).build());
    }

    /** Adds the floppy button; clicking it opens a small menu of file actions below it. */
    protected void addFileMenu(int x, int y, List<MenuEntry> entries) {
        fileMenu = entries;
        menuX = x;
        menuY = y + 17;
        menuW = 60;
        for (MenuEntry e : entries) menuW = Math.max(menuW, font.width(e.label()) + 12);
        floppy = addRenderableWidget(new FloppyButton(x, y, b -> setFileMenuOpen(!fileMenuOpen)));
        ItemStack icon = new ItemStack(AppliedSwatches.APPLICATOR.get());
        applicatorBtn = addRenderableWidget(new Button(x + 18, y, 16, 16, Component.empty(), b -> loadApplicator(), s -> s.get()) {
            @Override
            protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
                super.renderWidget(g, mx, my, partial);
                g.pose().pushPose();
                g.pose().translate(getX() + 1, getY() + 1, 0);
                g.pose().scale(14 / 16f, 14 / 16f, 1);
                g.renderItem(icon, 0, 0);
                g.pose().popPose();
            }
        });
        applicatorBtn.setTooltip(Tooltip.create(Component.translatable("gui.appliedswatches.load_applicator")));
        applicatorBtn.active = networkOnline && !exports.isEmpty();
    }

    private void loadApplicator() {
        if (!exports.isEmpty()) PacketDistributor.sendToServer(new LoadApplicatorPayload(List.copyOf(exports), paletteName()));
    }

    private void setFileMenuOpen(boolean open) {
        fileMenuOpen = open;
        // The button's own tooltip would cover the menu.
        if (floppy != null) floppy.setTooltip(open ? null : FloppyButton.tooltip());
    }

    private int menuEntryAt(double mx, double my) {
        if (!fileMenuOpen || mx < menuX || mx >= menuX + menuW || my < menuY + 1) return -1;
        int i = (int) (my - menuY - 1) / MENU_ROW;
        return i < fileMenu.size() ? i : -1;
    }

    private void renderFileMenu(GuiGraphics g, int mx, int my) {
        if (!fileMenuOpen) return;
        int h = fileMenu.size() * MENU_ROW + 2, hover = menuEntryAt(mx, my);
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(menuX - 1, menuY - 1, menuX + menuW + 1, menuY + h + 1, 0xFF15171A);
        g.fill(menuX, menuY, menuX + menuW, menuY + h, 0xFF3C424B);
        for (int i = 0; i < fileMenu.size(); i++) {
            int ry = menuY + 1 + i * MENU_ROW;
            if (i == hover) g.fill(menuX + 1, ry, menuX + menuW - 1, ry + MENU_ROW, 0xFF4A5A86);
            g.drawString(font, fileMenu.get(i).label(), menuX + 6, ry + 3, 0xFFE4E8EE, false);
        }
        g.pose().popPose();
    }

    /** Moves the tolerance slider to the current value, e.g. after loading a file. */
    protected void syncTolerance() {
        if (toleranceSlider == null) return;
        removeWidget(toleranceSlider);
        addToleranceSlider(toleranceSlider.getX(), toleranceSlider.getY(), toleranceSlider.getWidth());
    }

    private void addToleranceSlider(int x, int y, int w) {
        toleranceSlider = addRenderableWidget(new AbstractSliderButton(x, y, w, 14, Component.empty(), tolerance / (double) MAX_TOLERANCE) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                setMessage(Component.translatable("gui.appliedswatches.tolerance", tolerance));
            }

            @Override
            protected void applyValue() {
                tolerance = Math.max(1, (int) Math.round(value * MAX_TOLERANCE));
                TerminalScreen.this.update();
            }
        });
    }

    protected void refresh() {
        PacketDistributor.sendToServer(RefreshPayload.INSTANCE);
    }

    public void setCandidates(CandidatesPayload p) {
        stored = p.items();
        amounts = p.amounts();
        networkOnline = p.online();
        loaded = true;
        update();
    }

    protected static int perItem() {
        return PER_ITEM[perItemIdx];
    }

    /** Re-ranks the stored blocks against the current palette. */
    protected void update() {
        palette = buildPalette();
        float[][] labs = new float[palette.length][];
        for (int i = 0; i < palette.length; i++) labs[i] = ColorMath.lab(palette[i]);
        Set<Item> preferred = preferred();
        matches.clear();
        for (int i = 0; i < stored.size() && labs.length > 0; i++) {
            BlockColorCache.Info info = BlockColorCache.get(stored.get(i));
            if (info == null) continue;
            boolean pinned = preferred.contains(stored.get(i));
            if (!pinned && fullOnly && !info.full()) continue;
            int best = -1;
            float bestDist = Float.MAX_VALUE;
            for (int s = 0; s < labs.length; s++) {
                float d = ColorMath.distance(info.lab(), labs[s]);
                if (d < bestDist) {
                    bestDist = d;
                    best = s;
                }
            }
            // Pinned blocks rank ahead of colour matches in their swatch.
            if (pinned) matches.add(new Match(stored.get(i), amounts[i], info.rgb(), best, -1));
            else if (bestDist <= tolerance) matches.add(new Match(stored.get(i), amounts[i], info.rgb(), best, bestDist));
        }
        Comparator<Match> order = Comparator.comparingInt(Match::swatch).thenComparingDouble(Match::dist);
        matches.sort(order);
        pickExports(order);
        scroll = Mth.clamp(scroll, 0, maxScroll());
        if (perItemBtn != null) {
            perItemBtn.setMessage(Component.translatable("gui.appliedswatches.per_item", perItem()));
            fullBtn.setMessage(Component.translatable(fullOnly ? "gui.appliedswatches.full_only" : "gui.appliedswatches.any_shape"));
        }
        if (exportBtn != null) exportBtn.active = networkOnline && !exports.isEmpty();
        if (applicatorBtn != null) applicatorBtn.active = networkOnline && !exports.isEmpty();
        updatePanel();
    }

    /** Refreshes subclass widgets after {@link #update()}. */
    protected void updatePanel() {
    }

    /**
     * Picks what Export sends: the closest blocks to each swatch in turn, so every palette colour gets its share
     * instead of the commonest one filling the output. Picks go to the front of the grid, still in palette order.
     */
    private void pickExports(Comparator<Match> order) {
        List<List<Match>> bySwatch = new ArrayList<>();
        for (int i = 0; i < palette.length; i++) bySwatch.add(new ArrayList<>());
        for (Match m : matches) if (!excluded().contains(m.item())) bySwatch.get(m.swatch()).add(m);
        List<Match> picked = new ArrayList<>();
        for (int round = 0; picked.size() < SwatchTerminalBlockEntity.SLOTS; round++) {
            boolean any = false;
            for (List<Match> list : bySwatch) {
                if (round >= list.size() || picked.size() >= SwatchTerminalBlockEntity.SLOTS) continue;
                picked.add(list.get(round));
                any = true;
            }
            if (!any) break;
        }
        picked.sort(order);
        exports.clear();
        for (Match m : picked) exports.add(m.item());
        // Skipped blocks stay up front with the picks so a click doesn't send them to the back of the grid.
        List<Match> front = new ArrayList<>(picked);
        for (Match m : matches) if (excluded().contains(m.item())) front.add(m);
        front.sort(order);
        matches.removeAll(front);
        matches.addAll(0, front);
    }

    private void export() {
        if (!exports.isEmpty()) PacketDistributor.sendToServer(new ExportPayload(List.copyOf(exports), perItem()));
    }

    private int maxScroll() {
        return Math.max(0, (matches.size() + COLS - 1) / COLS - ROWS);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.online() != wasOnline) {
            wasOnline = menu.online();
            refresh();
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF2B2F36);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF3C424B);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFF59616D);
        for (Slot s : menu.slots) slotBg(g, x + s.x - 1, y + s.y - 1);
        renderPanel(g, x, y, mx, my);
        renderGrid(g, x, y);
    }

    /** Draws the subclass's panel background. */
    protected abstract void renderPanel(GuiGraphics g, int x, int y, int mx, int my);

    private static void slotBg(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF1E2126);
        g.fill(x + 1, y + 1, x + 18, y + 18, 0xFF5E6672);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF262A30);
    }

    /** Draws palette swatches in a row. */
    protected void renderPalette(GuiGraphics g, int x, int y, int w, int h) {
        int n = Math.max(1, palette.length), cw = Math.min(16, (w - 2 * (n - 1)) / n);
        for (int i = 0; i < palette.length; i++) {
            int bx = x + i * (cw + 2);
            g.fill(bx, y, bx + cw, y + h, 0xFF15171A);
            g.fill(bx + 1, y + 1, bx + cw - 1, y + h - 1, 0xFF000000 | palette[i]);
        }
    }

    private void renderGrid(GuiGraphics g, int x, int y) {
        int gx = x + GRID_X, gy = y + GRID_Y;
        g.fill(gx - 1, gy - 1, gx + COLS * CELL + 1, gy + ROWS * CELL + 1, 0xFF15171A);
        g.fill(gx, gy, gx + COLS * CELL, gy + ROWS * CELL, 0xFF23272D);
        int sbx = gx + COLS * CELL + 2, max = maxScroll();
        g.fill(sbx, gy, sbx + 4, gy + ROWS * CELL, 0xFF15171A);
        int thumb = Math.max(8, ROWS * CELL * ROWS / Math.max(ROWS, ROWS + max));
        int ty = gy + (max == 0 ? 0 : (ROWS * CELL - thumb) * scroll / max);
        g.fill(sbx, ty, sbx + 4, ty + thumb, max == 0 ? 0xFF4A515B : 0xFF8A93A0);
        Set<Item> exported = new HashSet<>(exports);
        for (int i = 0; i < COLS * ROWS; i++) {
            int idx = scroll * COLS + i;
            if (idx >= matches.size()) break;
            Match m = matches.get(idx);
            int cx = gx + (i % COLS) * CELL, cy = gy + (i / COLS) * CELL;
            g.fill(cx + 1, cy + CELL - 3, cx + CELL - 1, cy + CELL - 1, 0xFF000000 | palette[m.swatch()]);
            g.renderItem(new ItemStack(m.item()), cx + 1, cy);
            if (excluded().contains(m.item())) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.fill(cx, cy, cx + CELL, cy + CELL, 0x80000000);
                drawSkipMark(g, cx + CELL - 8, cy);
                g.pose().popPose();
            } else if (!exported.contains(m.item())) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.fill(cx, cy, cx + CELL, cy + CELL, 0x90000000);
                g.pose().popPose();
            }
        }
        if (matches.isEmpty()) {
            Component msg = Component.translatable(!loaded ? "gui.appliedswatches.loading"
                    : !networkOnline ? "gui.appliedswatches.offline" : "gui.appliedswatches.no_matches");
            g.drawCenteredString(font, msg, gx + COLS * CELL / 2, gy + ROWS * CELL / 2 - 4, 0xFF9AA3AF);
        }
    }

    /** Small red x badge, 7x7, marking a skipped block. */
    private static void drawSkipMark(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 7, y + 7, 0xFF15171A);
        g.fill(x + 1, y + 1, x + 6, y + 6, 0xFFB02424);
        for (int i = 0; i < 3; i++) {
            g.fill(x + 2 + i, y + 2 + i, x + 3 + i, y + 3 + i, 0xFFFFFFFF);
            g.fill(x + 4 - i, y + 2 + i, x + 5 - i, y + 3 + i, 0xFFFFFFFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 6, 0xFFE4E8EE, false);
        Component status = Component.translatable(networkOnline ? "gui.appliedswatches.status" : "gui.appliedswatches.status_offline", matches.size())
                .withStyle(networkOnline ? ChatFormatting.GREEN : ChatFormatting.RED);
        g.drawString(font, status, imageWidth - 8 - font.width(status), 6, 0xFFFFFFFF, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFC8CED6, false);
        g.drawString(font, Component.translatable("gui.appliedswatches.output"), SwatchMenu.OUT_X, inventoryLabelY, 0xFFC8CED6, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        renderFileMenu(g, mx, my);
        if (menuEntryAt(mx, my) >= 0) return;
        Match m = matchAt(mx, my);
        if (m != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(new ItemStack(m.item()).getHoverName());
            tip.add(Component.translatable("gui.appliedswatches.stored", String.format("%,d", m.amount())).withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal(m.dist() < 0 ? String.format("#%06X", m.rgb()) : String.format("#%06X  ΔE %.1f", m.rgb(), m.dist()))
                    .withStyle(ChatFormatting.DARK_GRAY));
            if (m.dist() < 0) tip.add(Component.translatable("gui.appliedswatches.pinned").withStyle(ChatFormatting.GOLD));
            tip.add(Component.translatable(excluded().contains(m.item()) ? "gui.appliedswatches.click_include" : "gui.appliedswatches.click_exclude")
                    .withStyle(ChatFormatting.DARK_AQUA));
            g.renderComponentTooltip(font, tip, mx, my);
        } else renderTooltip(g, mx, my);
    }

    private Match matchAt(double mx, double my) {
        int gx = leftPos + GRID_X, gy = topPos + GRID_Y;
        if (mx < gx || my < gy || mx >= gx + COLS * CELL || my >= gy + ROWS * CELL) return null;
        int idx = (scroll + (int) (my - gy) / CELL) * COLS + (int) (mx - gx) / CELL;
        return idx < matches.size() ? matches.get(idx) : null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (fileMenuOpen) {
            int i = menuEntryAt(mx, my);
            setFileMenuOpen(false);
            if (i >= 0) fileMenu.get(i).action().run();
            return true;
        }
        if (button == 0) {
            Match m = matchAt(mx, my);
            if (m != null) {
                if (!excluded().remove(m.item())) excluded().add(m.item());
                update();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int gx = leftPos + GRID_X, gy = topPos + GRID_Y;
        if (mx >= gx && my >= gy && mx < gx + COLS * CELL + 8 && my < gy + ROWS * CELL) {
            scroll = Mth.clamp(scroll - (int) Math.signum(sy), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }
}
