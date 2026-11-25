package com.unholymc.mixin;

import com.unholymc.item.HolyWaterItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hoppers may put holy water into the bottle slots too. */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {
    @Inject(method = "canPlaceItem", at = @At("RETURN"), cancellable = true)
    private void unholymc$allowHolyWater(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (slot < 3 && stack.getItem() instanceof HolyWaterItem
                && ((BrewingStandBlockEntity) (Object) this).getItem(slot).isEmpty()) {
            cir.setReturnValue(true);
        }
    }
}
