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

/** Ask the server to pull {@code perItem} of each item, in order, into the terminal. */
public record ExportPayload(List<Item> items, int perItem) implements CustomPacketPayload {
    public static final Type<ExportPayload> TYPE = new Type<>(AppliedSwatches.id("export"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExportPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list(SwatchTerminalBlockEntity.SLOTS)), ExportPayload::items,
            ByteBufCodecs.VAR_INT.map(v -> Math.clamp(v, 1, 64 * SwatchTerminalBlockEntity.SLOTS), v -> v), ExportPayload::perItem,
            ExportPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
