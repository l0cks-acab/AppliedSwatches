package com.lach.appliedswatches.net;

import com.lach.appliedswatches.AppliedSwatches;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sneak + scroll with an applicator in the main hand: step its selection by {@code dir}. */
public record CycleApplicatorPayload(int dir) implements CustomPacketPayload {
    public static final Type<CycleApplicatorPayload> TYPE = new Type<>(AppliedSwatches.id("cycle_applicator"));
    public static final StreamCodec<FriendlyByteBuf, CycleApplicatorPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .map(d -> new CycleApplicatorPayload(Integer.signum(d)), CycleApplicatorPayload::dir).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
