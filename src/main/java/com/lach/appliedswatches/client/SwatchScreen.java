package com.lach.appliedswatches.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lach.appliedswatches.SwatchMenu;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SwatchScreen extends TerminalScreen {
    private static final int SV = 64, HUE_X = 76, HUE_W = 8, PICK_Y = 18;
    private static final int CTRL_X = 92, CTRL_W = 86, FILE_Y = 110;

    private static float hue = 0.58f, sat = 0.7f, val = 0.8f;
    /** Exact colour last typed or loaded, so hex round trips; -1 once the picker moves. */
    private static int exactRgb = -1;
    private static Scheme scheme = Scheme.ANALOGOUS;
    private static final Set<Item> EXCLUDED = new HashSet<>();

    private int dragging;
    private EditBox hex;
    private Button schemeBtn;

    public SwatchScreen(SwatchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected int[] buildPalette() {
        return scheme.palette(hue, sat, val);
    }

    @Override
    protected Set<Item> excluded() {
        return EXCLUDED;
    }

    @Override
    protected void initPanel(int x, int y) {
        hex = new EditBox(font, x + 18, y + PICK_Y + SV + 6, 44, 12, Component.literal("hex"));
        hex.setMaxLength(6);
        hex.setFilter(t -> t.matches("[0-9a-fA-F]{0,6}"));
        hex.setValue(String.format("%06X", rgb()));
        hex.setResponder(t -> {
            if (t.length() == 6) setRgb(Integer.parseInt(t, 16));
        });
        addRenderableWidget(hex);
        schemeBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            scheme = hasShiftDown() ? Scheme.values()[(scheme.ordinal() + Scheme.values().length - 1) % Scheme.values().length] : scheme.next();
            update();
        }).bounds(x + CTRL_X, y + PICK_Y, CTRL_W, 14).build());
        addSettings(x + CTRL_X, y + PICK_Y + 17, CTRL_W);
        addFileMenu(x + 8, y + FILE_Y, List.of(
                new MenuEntry(Component.translatable("gui.appliedswatches.save"), this::save),
                new MenuEntry(Component.translatable("gui.appliedswatches.load"), this::load),
                new MenuEntry(Component.translatable("gui.appliedswatches.folder"), () -> Util.getPlatform().openFile(dir().toFile()))));
    }

    @Override
    protected void updatePanel() {
        if (schemeBtn != null) schemeBtn.setMessage(Component.translatable(scheme.key()));
    }

    private static Path dir() {
        return FileDialogs.dir("swatches");
    }

    private static int rgb() {
        return exactRgb >= 0 ? exactRgb : Mth.hsvToRgb(hue, sat, val) & 0xFFFFFF;
    }

    private void setRgb(int rgb) {
        exactRgb = rgb;
        float r = (rgb >> 16 & 0xFF) / 255f, g = (rgb >> 8 & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        val = max;
        sat = max == 0 ? 0 : d / max;
        if (d > 0) {
            float h = max == r ? (g - b) / d : max == g ? 2 + (b - r) / d : 4 + (r - g) / d;
            hue = ((h / 6f) % 1f + 1f) % 1f;
        }
        update();
    }

    /** Current swatch as JSON. */
    private JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "appliedswatches:swatch");
        o.addProperty("color", Theme.formatColor(rgb()));
        o.addProperty("scheme", scheme.name().toLowerCase(Locale.ROOT));
        o.addProperty("tolerance", tolerance);
        o.addProperty("perItem", perItem());
        o.addProperty("fullOnly", fullOnly);
        JsonArray pal = new JsonArray();
        for (int c : palette) pal.add(Theme.formatColor(c));
        o.add("palette", pal);
        JsonArray ex = new JsonArray();
        for (Item i : EXCLUDED) ex.add(BuiltInRegistries.ITEM.getKey(i).toString());
        o.add("excluded", ex);
        return o;
    }

    private void fromJson(JsonObject o) {
        if (o.has("color")) setRgb(Theme.parseColor(o.get("color").getAsString()));
        else if (o.has("palette")) setRgb(Theme.parseColor(o.getAsJsonArray("palette").get(0).getAsString()));
        else throw new IllegalArgumentException("no colour");
        if (o.has("scheme")) {
            try {
                scheme = Scheme.valueOf(o.get("scheme").getAsString().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (o.has("tolerance")) tolerance = Mth.clamp(o.get("tolerance").getAsInt(), 1, MAX_TOLERANCE);
        if (o.has("perItem")) {
            int want = o.get("perItem").getAsInt();
            for (int i = 0; i < PER_ITEM.length; i++) if (PER_ITEM[i] <= want) perItemIdx = i;
        }
        if (o.has("fullOnly")) fullOnly = o.get("fullOnly").getAsBoolean();
        if (o.has("excluded")) {
            EXCLUDED.clear();
            for (JsonElement e : o.getAsJsonArray("excluded")) {
                ResourceLocation id = ResourceLocation.tryParse(e.getAsString());
                Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
                if (item != Items.AIR) EXCLUDED.add(item);
            }
        }
        hex.setValue(String.format("%06X", rgb()));
        syncTolerance();
        update();
    }

    private void save() {
        JsonObject json = toJson();
        FileDialogs.save(dir().resolve(String.format("swatch_%06x.json", rgb())), p -> {
            try {
                FileDialogs.write(p, json);
                FileDialogs.toast("gui.appliedswatches.saved", p.getFileName().toString());
            } catch (Exception e) {
                FileDialogs.toast("gui.appliedswatches.file_error", e.getMessage());
            }
        });
    }

    private void load() {
        FileDialogs.open(dir(), p -> {
            try {
                fromJson(FileDialogs.read(p));
                FileDialogs.toast("gui.appliedswatches.loaded", p.getFileName().toString());
            } catch (Exception e) {
                FileDialogs.toast("gui.appliedswatches.file_error", e.getMessage());
            }
        });
    }

    @Override
    protected void renderPanel(GuiGraphics g, int x, int y, int mx, int my) {
        int sx = x + 8, sy = y + PICK_Y;
        g.fill(sx - 1, sy - 1, sx + SV + 1, sy + SV + 1, 0xFF15171A);
        for (int i = 0; i < SV; i++) {
            int top = 0xFF000000 | Mth.hsvToRgb(hue, i / (float) (SV - 1), 1);
            g.fillGradient(sx + i, sy, sx + i + 1, sy + SV, top, 0xFF000000);
        }
        int cx = sx + Math.round(sat * (SV - 1)), cy = sy + Math.round((1 - val) * (SV - 1));
        g.fill(cx - 2, cy, cx + 3, cy + 1, 0xFFFFFFFF);
        g.fill(cx, cy - 2, cx + 1, cy + 3, 0xFFFFFFFF);
        int hx = x + HUE_X;
        g.fill(hx - 1, sy - 1, hx + HUE_W + 1, sy + SV + 1, 0xFF15171A);
        for (int j = 0; j < SV; j++) g.fill(hx, sy + j, hx + HUE_W, sy + j + 1, 0xFF000000 | Mth.hsvToRgb(j / (float) SV, 1, 1));
        int hy = sy + Math.round(hue * (SV - 1));
        g.fill(hx - 1, hy - 1, hx + HUE_W + 1, hy + 2, 0xFFFFFFFF);
        g.fill(hx, hy, hx + HUE_W, hy + 1, 0xFF000000 | Mth.hsvToRgb(hue, 1, 1));
        g.drawString(font, "#", sx + 1, hex.getY() + 2, 0xFFAAAAAA, false);
        int px = hex.getX() + hex.getWidth() + 4;
        g.fill(px, hex.getY(), hx + HUE_W, hex.getY() + 12, 0xFF15171A);
        g.fill(px + 1, hex.getY() + 1, hx + HUE_W - 1, hex.getY() + 11, 0xFF000000 | rgb());
        renderPalette(g, x + CTRL_X, y + PICK_Y + 70, CTRL_W, 14);
    }

    /** Applies a click or drag in the picker; returns which part (1 square, 2 hue bar) took it, or 0. */
    private int pick(double mx, double my, int part) {
        int sx = leftPos + 8, sy = topPos + PICK_Y, hx = leftPos + HUE_X;
        if (part == 1 || part == 0 && mx >= sx && mx < sx + SV && my >= sy && my < sy + SV) {
            sat = Mth.clamp((float) (mx - sx) / (SV - 1), 0, 1);
            val = 1 - Mth.clamp((float) (my - sy) / (SV - 1), 0, 1);
            part = 1;
        } else if (part == 2 || part == 0 && mx >= hx - 2 && mx < hx + HUE_W + 2 && my >= sy && my < sy + SV) {
            hue = Mth.clamp((float) (my - sy) / SV, 0, 0.999f);
            part = 2;
        } else return 0;
        exactRgb = -1;
        hex.setValue(String.format("%06X", rgb()));
        update();
        return part;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            dragging = pick(mx, my, 0);
            if (dragging != 0) return true;
        }
        if (!hex.isMouseOver(mx, my)) hex.setFocused(false);
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != 0) {
            pick(mx, my, dragging);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = 0;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (hex.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) return hex.keyPressed(key, scan, mods) || true;
        return super.keyPressed(key, scan, mods);
    }
}
