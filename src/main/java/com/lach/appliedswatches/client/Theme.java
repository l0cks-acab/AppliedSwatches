package com.lach.appliedswatches.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A named palette, optionally with blocks that always belong to it.
 * JSON: {"name": "Desert", "palette": ["#DBCF9F", ...], "blocks": ["minecraft:sandstone", ...], "tolerance": 20}
 */
public record Theme(String name, int[] palette, List<ResourceLocation> blocks, int tolerance, boolean builtin) {
    public static final int MAX_COLORS = 8;

    public static Theme fromJson(JsonObject json, String fallbackName, boolean builtin) {
        String name = json.has("name") ? json.get("name").getAsString() : fallbackName;
        List<Integer> cols = new ArrayList<>();
        for (JsonElement e : json.getAsJsonArray("palette")) {
            if (cols.size() < MAX_COLORS) cols.add(parseColor(e.getAsString()));
        }
        if (cols.isEmpty()) throw new IllegalArgumentException("palette is empty");
        List<ResourceLocation> blocks = new ArrayList<>();
        if (json.has("blocks")) for (JsonElement e : json.getAsJsonArray("blocks")) {
            ResourceLocation id = ResourceLocation.tryParse(e.getAsString());
            if (id != null) blocks.add(id);
        }
        int tol = json.has("tolerance") ? json.get("tolerance").getAsInt() : -1;
        return new Theme(name, cols.stream().mapToInt(Integer::intValue).toArray(), blocks, tol, builtin);
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        JsonArray pal = new JsonArray();
        for (int c : palette) pal.add(formatColor(c));
        o.add("palette", pal);
        if (!blocks.isEmpty()) {
            JsonArray b = new JsonArray();
            for (ResourceLocation id : blocks) b.add(id.toString());
            o.add("blocks", b);
        }
        if (tolerance > 0) o.addProperty("tolerance", tolerance);
        return o;
    }

    /** Registered items for {@link #blocks()}; unknown ids are skipped. */
    public Set<Item> blockItems() {
        Set<Item> out = new LinkedHashSet<>();
        for (ResourceLocation id : blocks) {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != Items.AIR) out.add(item);
        }
        return out;
    }

    public static int parseColor(String s) {
        s = s.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (!s.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("bad colour " + s);
        return Integer.parseInt(s, 16);
    }

    public static String formatColor(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    /** Safe file name for this theme. */
    public String fileName() {
        String base = name.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        return (base.isEmpty() ? "theme" : base) + ".json";
    }
}
