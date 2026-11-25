package com.unholymc.mixin;

import com.unholymc.world.EnvironmentLayers;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnvironmentAttributeSystem.class)
public abstract class EnvironmentAttributeSystemMixin {
    @Inject(method = "addDefaultLayers(Lnet/minecraft/world/attribute/EnvironmentAttributeSystem$Builder;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private static void unholymc$addLayers(EnvironmentAttributeSystem.Builder builder, Level level, CallbackInfo ci) {
        EnvironmentLayers.addLayers(builder, level);
    }
}
