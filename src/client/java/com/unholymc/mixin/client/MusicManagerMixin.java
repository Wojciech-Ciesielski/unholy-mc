package com.unholymc.mixin.client;

import com.unholymc.client.sound.ElevenLoop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** In a world the reversed disc 11 replaces the vanilla soundtrack. */
@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void unholymc$noVanillaMusic(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null && ElevenLoop.isPlaying()) {
            ((MusicManager) (Object) this).stopPlaying();
            ci.cancel();
        }
    }
}
