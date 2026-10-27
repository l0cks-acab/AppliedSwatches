package com.lach.appliedswatches.client;

import com.lach.appliedswatches.SwatchMenu;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ThemeScreen extends TerminalScreen {
    private static final int LIST_X = 8, LIST_Y = 18, LIST_W = 92, ROW_H = 11, LIST_ROWS = 8;
    private static final int CTRL_X = 104, CTRL_W = 74, FILE_Y = 72;

    /** Selected theme, by name, kept between screens. */
    private static String selectedName = "Cyberpunk";
    private static final Set<Item> EXCLUDED = new HashSet<>();

    private List<Theme> themes = List.of();
    private Theme selected;
    private int listScroll;

    public ThemeScreen(SwatchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected String paletteName() {
        return selected == null ? "" : selected.name();
    }

    @Override
    protected int[] buildPalette() {
        return selected == null ? new int[0] : selected.palette();
    }

    @Override
    protected Set<Item> excluded() {
        return EXCLUDED;
    }

    @Override
    protected Set<Item> preferred() {
        return selected == null ? Set.of() : selected.blockItems();
    }

    @Override
    protected void initPanel(int x, int y) {
        reloadThemes();
        addSettings(x + CTRL_X, y + LIST_Y, CTRL_W);
        addFileMenu(x + CTRL_X, y + FILE_Y, List.of(
                new MenuEntry(Component.translatable("gui.appliedswatches.import"), this::importTheme),
                new MenuEntry(Component.translatable("gui.appliedswatches.export_json"), this::exportTheme),
                new MenuEntry(Component.translatable("gui.appliedswatches.folder"), () -> Util.getPlatform().openFile(ThemeManager.userDir().toFile()))));
    }

    private void reloadThemes() {
        themes = ThemeManager.load();
        selected = themes.stream().filter(t -> t.name().equals(selectedName)).findFirst().orElse(themes.isEmpty() ? null : themes.get(0));
        int i = themes.indexOf(selected);
        if (i >= 0 && (i < listScroll || i >= listScroll + LIST_ROWS)) listScroll = i - LIST_ROWS / 2;
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, themes.size() - LIST_ROWS));
    }

    private void select(Theme t) {
        selected = t;
        selectedName = t.name();
        EXCLUDED.clear();
        if (t.tolerance() > 0) {
            tolerance = Mth.clamp(t.tolerance(), 1, MAX_TOLERANCE);
            syncTolerance();
        }
        update();
    }

    private void importTheme() {
        FileDialogs.open(ThemeManager.userDir(), p -> {
            try {
                String n = p.getFileName().toString();
                Theme t = Theme.fromJson(FileDialogs.read(p), n.endsWith(".json") ? n.substring(0, n.length() - 5) : n, false);
                Path dest = ThemeManager.userDir().resolve(t.fileName());
                for (int i = 2; Files.exists(dest) && !Files.isSameFile(dest, p); i++)
                    dest = ThemeManager.userDir().resolve(t.fileName().replace(".json", "_" + i + ".json"));
                if (!Files.exists(dest)) FileDialogs.write(dest, t.toJson());
                selectedName = t.name();
                reloadThemes();
                if (selected != null) select(selected);
                FileDialogs.toast("gui.appliedswatches.imported", t.name());
            } catch (Exception e) {
                FileDialogs.toast("gui.appliedswatches.file_error", e.getMessage());
            }
        });
    }

    private void exportTheme() {
        if (selected == null) return;
        Theme t = selected;
        FileDialogs.save(FileDialogs.dir("exports").resolve(t.fileName()), p -> {
            try {
                FileDialogs.write(p, t.toJson());
                FileDialogs.toast("gui.appliedswatches.saved", p.getFileName().toString());
            } catch (Exception e) {
                FileDialogs.toast("gui.appliedswatches.file_error", e.getMessage());
            }
        });
    }

    @Override
    protected void renderPanel(GuiGraphics g, int x, int y, int mx, int my) {
        int lx = x + LIST_X, ly = y + LIST_Y;
        g.fill(lx - 1, ly - 1, lx + LIST_W + 1, ly + LIST_ROWS * ROW_H + 1, 0xFF15171A);
        g.fill(lx, ly, lx + LIST_W, ly + LIST_ROWS * ROW_H, 0xFF23272D);
        for (int r = 0; r < LIST_ROWS; r++) {
            int i = listScroll + r;
            if (i >= themes.size()) break;
            Theme t = themes.get(i);
            int ry = ly + r * ROW_H;
            boolean hover = mx >= lx && mx < lx + LIST_W && my >= ry && my < ry + ROW_H;
            if (t == selected) g.fill(lx, ry, lx + LIST_W, ry + ROW_H, 0xFF4A5A86);
            else if (hover) g.fill(lx, ry, lx + LIST_W, ry + ROW_H, 0xFF333944);
            int stripW = Math.min(t.palette().length, 5) * 4;
            String name = font.plainSubstrByWidth(t.name(), LIST_W - stripW - 7);
            g.drawString(font, name, lx + 3, ry + 2, t.builtin() ? 0xFFE4E8EE : 0xFF9CD3FF, false);
            for (int c = 0; c < Math.min(t.palette().length, 5); c++) {
                int sx = lx + LIST_W - stripW - 2 + c * 4;
                g.fill(sx, ry + 2, sx + 4, ry + ROW_H - 2, 0xFF000000 | t.palette()[c]);
            }
        }
        if (themes.size() > LIST_ROWS) {
            int max = themes.size() - LIST_ROWS, h = LIST_ROWS * ROW_H, thumb = Math.max(8, h * LIST_ROWS / themes.size());
            int ty = ly + (h - thumb) * listScroll / max;
            g.fill(lx + LIST_W - 1, ty, lx + LIST_W, ty + thumb, 0xFF8A93A0);
        }
        renderPalette(g, lx, y + 110, LIST_W, 14);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        Theme t = themeAt(mx, my);
        if (t != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal(t.name()));
            tip.add(Component.translatable(t.builtin() ? "gui.appliedswatches.builtin" : "gui.appliedswatches.user_theme")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            if (!t.blocks().isEmpty())
                tip.add(Component.translatable("gui.appliedswatches.theme_blocks", t.blocks().size()).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(font, tip, mx, my);
        }
    }

    private Theme themeAt(double mx, double my) {
        int lx = leftPos + LIST_X, ly = topPos + LIST_Y;
        if (mx < lx || mx >= lx + LIST_W || my < ly || my >= ly + LIST_ROWS * ROW_H) return null;
        int i = listScroll + (int) (my - ly) / ROW_H;
        return i < themes.size() ? themes.get(i) : null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        Theme t = button == 0 ? themeAt(mx, my) : null;
        if (t != null) {
            select(t);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int lx = leftPos + LIST_X, ly = topPos + LIST_Y;
        if (mx >= lx && mx < lx + LIST_W && my >= ly && my < ly + LIST_ROWS * ROW_H) {
            listScroll = Mth.clamp(listScroll - (int) Math.signum(sy), 0, Math.max(0, themes.size() - LIST_ROWS));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }
}
