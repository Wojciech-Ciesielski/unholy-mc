package com.unholymc.mixin;

import com.unholymc.polymer.ServerHud;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Action bar messages ("You can sleep only at night", ...) pause our action bar HUD so they can be read. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "sendSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"))
    private void unholymc$pauseHud(Component message, boolean overlay, CallbackInfo ci) {
        if (overlay) ServerHud.pause((ServerPlayer) (Object) this);
    }
}
