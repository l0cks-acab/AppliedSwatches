package com.lach.appliedswatches;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config: {@code config/appliedswatches-server.toml}. */
public final class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue SWATCH_IDLE_POWER;
    public static final ModConfigSpec.DoubleValue THEME_IDLE_POWER;
    public static final ModConfigSpec.IntValue APPLICATOR_RANGE;
    public static final ModConfigSpec.IntValue LISTING_COOLDOWN;
    public static final ModConfigSpec.IntValue ACTION_COOLDOWN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("power");
        SWATCH_IDLE_POWER = b.comment("AE drawn per tick by an idle Swatch Terminal.")
                .defineInRange("swatchTerminalIdlePower", 2.0, 0.0, 1000.0);
        THEME_IDLE_POWER = b.comment("AE drawn per tick by an idle Theme Terminal.")
                .defineInRange("themeTerminalIdlePower", 2.0, 0.0, 1000.0);
        b.pop();
        b.push("applicator");
        APPLICATOR_RANGE = b.comment("How far, in blocks, a Swatch Applicator reaches its linked terminal. 0 = anywhere in the same dimension.")
                .defineInRange("range", 64, 0, 100000);
        b.pop();
        b.push("antispam");
        LISTING_COOLDOWN = b.comment("Fewest ticks between two network listings sent to one terminal screen. Extra refreshes are merged.")
                .defineInRange("listingCooldownTicks", 10, 0, 1200);
        ACTION_COOLDOWN = b.comment("Fewest ticks between Export / Return / Craft / Load applicator from one terminal screen. Extra requests are dropped.")
                .defineInRange("actionCooldownTicks", 5, 0, 1200);
        b.pop();
        SPEC = b.build();
    }

    public static int listingCooldownTicks() {
        return safe(LISTING_COOLDOWN, 10);
    }

    public static int actionCooldownTicks() {
        return safe(ACTION_COOLDOWN, 5);
    }

    private static int safe(ModConfigSpec.IntValue v, int fallback) {
        try {
            return v.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    private Config() {
    }
}
