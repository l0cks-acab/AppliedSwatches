package com.lach.appliedswatches.net;

import com.lach.appliedswatches.AppliedSwatches;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Ask the server for a fresh {@link CandidatesPayload}. */
public record RefreshPayload() implements CustomPacketPayload {
    public static final RefreshPayload INSTANCE = new RefreshPayload();
    public static final Type<RefreshPayload> TYPE = new Type<>(AppliedSwatches.id("refresh"));
    public static final StreamCodec<ByteBuf, RefreshPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
