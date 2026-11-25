package com.unholymc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.unholymc.registry.ModItems;
import com.unholymc.world.HolyPonds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A glass bottle filled at a holy pond on a full moon night gives holy water (GDD 1.1). */
@Mixin(BottleItem.class)
public abstract class BottleItemMixin {
    @WrapOperation(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/alchemy/PotionContents;createItemStack(Lnet/minecraft/world/item/Item;Lnet/minecraft/core/Holder;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack unholymc$holyWater(Item item, Holder<Potion> potion, Operation<ItemStack> original,
                                         @Local(argsOnly = true) Level level, @Local BlockPos pos) {
        if (HolyPonds.isHolyWater(level, pos)) return new ItemStack(ModItems.HOLY_WATER);
        return original.call(item, potion);
    }
}
