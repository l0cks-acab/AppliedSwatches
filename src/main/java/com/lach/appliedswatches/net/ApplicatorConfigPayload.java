package com.lach.appliedswatches.net;

import com.lach.appliedswatches.AppliedSwatches;
import com.lach.appliedswatches.PlaceMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;

/** Settings changed in the applicator screen: selected block, placement mode and size. */
public record ApplicatorConfigPayload(boolean offhand, int selected, PlaceMode mode, int size) implements CustomPacketPayload {
    public static final Type<ApplicatorConfigPayload> TYPE = new Type<>(AppliedSwatches.id("applicator_config"));
    public static final StreamCodec<ByteBuf, ApplicatorConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ApplicatorConfigPayload::offhand,
            ByteBufCodecs.VAR_INT, ApplicatorConfigPayload::selected,
            PlaceMode.STREAM_CODEC, ApplicatorConfigPayload::mode,
            ByteBufCodecs.VAR_INT, ApplicatorConfigPayload::size,
            ApplicatorConfigPayload::new);

    public InteractionHand hand() {
        return offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
