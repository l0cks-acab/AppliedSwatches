package com.lach.appliedswatches.net;

import com.lach.appliedswatches.AppliedSwatches;
import com.lach.appliedswatches.SwatchTerminalBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;

import java.util.List;

/** Ask the server to start AE2 crafting jobs for {@code amount} of each item. */
public record CraftPayload(List<Item> items, int amount) implements CustomPacketPayload {
    public static final Type<CraftPayload> TYPE = new Type<>(AppliedSwatches.id("craft"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CraftPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list(SwatchTerminalBlockEntity.SLOTS)), CraftPayload::items,
            ByteBufCodecs.VAR_INT.map(v -> Math.clamp(v, 1, 64 * SwatchTerminalBlockEntity.SLOTS), v -> v), CraftPayload::amount,
            CraftPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
