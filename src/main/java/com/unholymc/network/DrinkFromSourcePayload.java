package com.unholymc.network;

import com.unholymc.UnholyMC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: "I right-clicked with an empty hand"; the server validates the water source itself. */
public record DrinkFromSourcePayload() implements CustomPacketPayload {
    public static final Type<DrinkFromSourcePayload> TYPE = new Type<>(UnholyMC.id("drink_from_source"));
    public static final StreamCodec<ByteBuf, DrinkFromSourcePayload> CODEC = StreamCodec.unit(new DrinkFromSourcePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
