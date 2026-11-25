package com.unholymc.network;

import com.unholymc.UnholyMC;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** Server -> saved player: show the crucifix (instead of a totem) in the item activation animation. */
public record CrucifixSavePayload(ItemStack stack) implements CustomPacketPayload {
    public static final Type<CrucifixSavePayload> TYPE = new Type<>(UnholyMC.id("crucifix_save"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CrucifixSavePayload> CODEC =
            ItemStack.STREAM_CODEC.map(CrucifixSavePayload::new, CrucifixSavePayload::stack);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
