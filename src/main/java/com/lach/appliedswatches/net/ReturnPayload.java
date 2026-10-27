package com.lach.appliedswatches.net;

import com.lach.appliedswatches.AppliedSwatches;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Ask the server to put the output slots back into the network. */
public record ReturnPayload() implements CustomPacketPayload {
    public static final ReturnPayload INSTANCE = new ReturnPayload();
    public static final Type<ReturnPayload> TYPE = new Type<>(AppliedSwatches.id("return"));
    public static final StreamCodec<ByteBuf, ReturnPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
