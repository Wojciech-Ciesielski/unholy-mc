package com.unholymc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.unholymc.item.UnholySwordItem;
import com.unholymc.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** DECISIONS #25: the Unholy Sword never breaks; at 0 durability it is discharged instead. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @WrapOperation(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isBroken()Z"))
    private boolean unholymc$neverBreakSword(ItemStack self, Operation<Boolean> original) {
        if (self.is(ModItems.UNHOLY_SWORD)) {
            UnholySwordItem.syncCharge(self, null);
            return false;
        }
        return original.call(self);
    }
}
