package com.lach.appliedswatches;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config: {@code <world>/serverconfig/appliedswatches-server.toml}. */
public final class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue SWATCH_IDLE_POWER;
    public static final ModConfigSpec.DoubleValue THEME_IDLE_POWER;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("power");
        SWATCH_IDLE_POWER = b.comment("AE drawn per tick by an idle Swatch Terminal.")
                .defineInRange("swatchTerminalIdlePower", 2.0, 0.0, 1000.0);
        THEME_IDLE_POWER = b.comment("AE drawn per tick by an idle Theme Terminal.")
                .defineInRange("themeTerminalIdlePower", 2.0, 0.0, 1000.0);
        b.pop();
        SPEC = b.build();
    }

    private Config() {
    }
}
