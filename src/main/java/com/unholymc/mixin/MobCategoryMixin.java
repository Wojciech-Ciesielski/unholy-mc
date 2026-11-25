package com.unholymc.mixin;

import com.unholymc.config.ConfigManager;
import com.unholymc.world.BloodMoon;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Blood Moon: "+200% spawns" - the monster cap is multiplied while it lasts. */
@Mixin(MobCategory.class)
public abstract class MobCategoryMixin {
    @Inject(method = "getMaxInstancesPerChunk", at = @At("RETURN"), cancellable = true)
    private void unholymc$bloodMoonCap(CallbackInfoReturnable<Integer> cir) {
        if (BloodMoon.activeOnServer() && (Object) this == MobCategory.MONSTER) {
            cir.setReturnValue(Math.round(cir.getReturnValueI() * ConfigManager.get().bloodMoon.spawnCapMultiplier));
        }
    }
}
