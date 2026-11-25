package com.unholymc.network;

import com.unholymc.UnholyMC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> player: play a jumpscare. kind 0 = screamer (variant = slop texture), 1 = Spooky Jumpscare. */
public record JumpscarePayload(int kind, int variant) implements CustomPacketPayload {
    public static final int SCREAMER = 0, SPOOKY = 1;
    public static final Type<JumpscarePayload> TYPE = new Type<>(UnholyMC.id("jumpscare"));
    public static final StreamCodec<ByteBuf, JumpscarePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, JumpscarePayload::kind,
            ByteBufCodecs.VAR_INT, JumpscarePayload::variant,
            JumpscarePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
