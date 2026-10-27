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

/** Load the terminal's current picks into the player's Swatch Applicator and link it to the terminal. */
public record LoadApplicatorPayload(List<Item> items, String name) implements CustomPacketPayload {
    public static final Type<LoadApplicatorPayload> TYPE = new Type<>(AppliedSwatches.id("load_applicator"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LoadApplicatorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list(SwatchTerminalBlockEntity.SLOTS)), LoadApplicatorPayload::items,
            ByteBufCodecs.stringUtf8(64), LoadApplicatorPayload::name,
            LoadApplicatorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
