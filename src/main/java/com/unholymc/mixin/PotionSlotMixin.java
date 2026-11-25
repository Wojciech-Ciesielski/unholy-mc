package com.unholymc.mixin;

import com.unholymc.item.HolyWaterItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Holy water bottles fit the brewing stand's bottle slots (DECISIONS #15). */
@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$PotionSlot")
public abstract class PotionSlotMixin {
    @Inject(method = "mayPlaceItem", at = @At("RETURN"), cancellable = true)
    private static void unholymc$allowHolyWater(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof HolyWaterItem) cir.setReturnValue(true);
    }
}
