package com.unholymc.registry;

import com.unholymc.UnholyMC;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/** Enchantments are data-driven (data/unholymc/enchantment/holiness.json). */
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> HOLINESS = ResourceKey.create(Registries.ENCHANTMENT, UnholyMC.id("holiness"));

    private ModEnchantments() {}

    public static int level(Level level, ResourceKey<Enchantment> key, ItemStack stack) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key)
                .map(h -> EnchantmentHelper.getItemEnchantmentLevel((Holder<Enchantment>) h, stack)).orElse(0);
    }
}
