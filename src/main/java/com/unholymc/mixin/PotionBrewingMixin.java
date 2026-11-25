package com.unholymc.mixin;

import com.unholymc.item.HolyWaterItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Holy water carries a plain water potion component internally; keep vanilla potion mixes
 * (nether wart -> awkward, ...) away from it. Only the holy water + gunpowder container recipe applies.
 */
@Mixin(PotionBrewing.class)
public abstract class PotionBrewingMixin {
    @Inject(method = "hasPotionMix", at = @At("HEAD"), cancellable = true)
    private void unholymc$noPotionMix(ItemStack source, ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        if (source.getItem() instanceof HolyWaterItem) cir.setReturnValue(false);
    }

    @Inject(method = "mix", at = @At("HEAD"), cancellable = true)
    private void unholymc$onlyContainerMix(ItemStack ingredient, ItemStack source, CallbackInfoReturnable<ItemStack> cir) {
        if (source.getItem() instanceof HolyWaterItem && !((PotionBrewing) (Object) this).hasContainerMix(source, ingredient)) {
            cir.setReturnValue(source);
        }
    }
}
