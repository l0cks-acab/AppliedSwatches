package com.lach.appliedswatches;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config: {@code config/appliedswatches-server.toml}. */
public final class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue SWATCH_IDLE_POWER;
    public static final ModConfigSpec.DoubleValue THEME_IDLE_POWER;
    public static final ModConfigSpec.IntValue APPLICATOR_RANGE;

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
        SPEC = b.build();
    }

    private Config() {
    }
}
