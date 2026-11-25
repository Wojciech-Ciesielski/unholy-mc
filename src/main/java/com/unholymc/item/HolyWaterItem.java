package com.unholymc.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Drinkable holy water: +8 hydration, +6 sanity (handled in ConsumptionHandler).
 * Extends PotionItem only so the vanilla brewing stand accepts it as a container for the
 * holy water + gunpowder -> splash recipe; it carries a plain water potion component internally.
 */
public class HolyWaterItem extends PotionItem {
    public HolyWaterItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS; // no turning dirt into mud with holy water
    }
}
