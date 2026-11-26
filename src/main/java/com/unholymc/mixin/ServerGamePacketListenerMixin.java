package com.unholymc.mixin;

import com.unholymc.survival.ConsumptionHandler;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A left click into open water with an empty hand drinks from it (vanilla clients send only the arm swing). */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleAnimate", at = @At("TAIL"))
    private void unholymc$drinkOnSwing(ServerboundSwingPacket packet, CallbackInfo ci) {
        if (packet.getHand() == InteractionHand.MAIN_HAND) ConsumptionHandler.tryDrinkFromSource(player);
    }
}
