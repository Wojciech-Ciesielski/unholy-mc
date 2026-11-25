package com.unholymc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.unholymc.config.ConfigManager;
import com.unholymc.registry.ModItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Custom per-item repair amounts: crucifix +10 per holy water bottle, Unholy Sword +50 per Nightmare Gem
 * (vanilla repairs 25% of max durability per material item).
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    private AnvilMenuMixin(MenuType<?> type, int id, Inventory inv, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition def) {
        super(type, id, inv, access, def);
    }

    @WrapOperation(method = "createResult", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    private int unholymc$repairAmount(int damage, int quarter, Operation<Integer> original) {
        ItemStack input = this.inputSlots.getItem(0);
        if (input.is(ModItems.CRUCIFIX)) return Math.min(damage, ConfigManager.get().crucifix.repairPerHolyWater);
        if (input.is(ModItems.UNHOLY_SWORD)) return Math.min(damage, ConfigManager.get().unholySword.repairPerGem);
        return original.call(damage, quarter);
    }
}
