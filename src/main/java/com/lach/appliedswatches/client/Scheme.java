package com.lach.appliedswatches.client;

import net.minecraft.util.Mth;

/** Turns one picked colour into a palette. */
public enum Scheme {
    SINGLE, MONOCHROME, ANALOGOUS, COMPLEMENTARY, SPLIT, TRIADIC;

    public String key() {
        return "scheme.appliedswatches." + name().toLowerCase();
    }

    public Scheme next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** Palette colours as 0xRRGGBB, in export order. */
    public int[] palette(float h, float s, float v) {
        return switch (this) {
            case SINGLE -> new int[]{hsv(h, s, v)};
            case MONOCHROME -> new int[]{hsv(h, s, v * 0.55f), hsv(h, s, v * 0.78f), hsv(h, s, v),
                    hsv(h, s * 0.85f, v + (1 - v) * 0.4f), hsv(h, s * 0.65f, v + (1 - v) * 0.75f)};
            case ANALOGOUS -> new int[]{hsv(h - 2 / 24f, s, v), hsv(h - 1 / 24f, s, v), hsv(h, s, v), hsv(h + 1 / 24f, s, v), hsv(h + 2 / 24f, s, v)};
            case COMPLEMENTARY -> new int[]{hsv(h, s, v * 0.7f), hsv(h, s, v), hsv(h + 0.5f, s, v), hsv(h + 0.5f, s, v * 0.7f)};
            case SPLIT -> new int[]{hsv(h, s, v), hsv(h + 5 / 12f, s, v), hsv(h + 7 / 12f, s, v)};
            case TRIADIC -> new int[]{hsv(h, s, v), hsv(h + 1 / 3f, s, v), hsv(h + 2 / 3f, s, v)};
        };
    }

    private static int hsv(float h, float s, float v) {
        return Mth.hsvToRgb(((h % 1f) + 1f) % 1f, Mth.clamp(s, 0, 1), Mth.clamp(v, 0, 1)) & 0xFFFFFF;
    }
}
