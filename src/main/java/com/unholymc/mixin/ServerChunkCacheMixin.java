package com.unholymc.mixin;

import com.unholymc.world.BloodMoon;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks which dimension is running natural spawning, so the Blood Moon cap boost applies to the Overworld only. */
@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin {
    @Shadow @Final ServerLevel level;

    @Inject(method = "tickChunks()V", at = @At("HEAD"))
    private void unholymc$spawnStart(CallbackInfo ci) {
        BloodMoon.spawningInOverworld = level.dimension() == Level.OVERWORLD;
    }

    @Inject(method = "tickChunks()V", at = @At("RETURN"))
    private void unholymc$spawnEnd(CallbackInfo ci) {
        BloodMoon.spawningInOverworld = false;
    }
}
