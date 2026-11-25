package com.unholymc.item;

import com.unholymc.config.ConfigManager;
import com.unholymc.light.DynamicLights;
import com.unholymc.light.LightSource;
import com.unholymc.registry.ModItems;
import com.unholymc.survival.LightOverrides;
import com.unholymc.survival.SanityAuras;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.registry.FabricPotionBrewingBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

import java.util.Set;

/** Wires the items into the sanity, light, brewing and loot systems. */
public final class ItemHooks {
    private ItemHooks() {}

    public static void init() {
        // GDD 1.2: the crucifix takes only Mending, Unbreaking and Holiness. The durability tag it is in also feeds
        // Curse of Vanishing (vanilla #enchantable/vanishing includes #enchantable/durability), so block the rest.
        net.fabricmc.fabric.api.item.v1.EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, stack, context) -> {
            if (!stack.is(com.unholymc.registry.ModItems.CRUCIFIX)) return net.fabricmc.fabric.api.util.TriState.DEFAULT;
            boolean ok = enchantment.is(net.minecraft.world.item.enchantment.Enchantments.MENDING)
                    || enchantment.is(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING)
                    || enchantment.is(com.unholymc.registry.ModEnchantments.HOLINESS);
            return ok ? net.fabricmc.fabric.api.util.TriState.DEFAULT : net.fabricmc.fabric.api.util.TriState.FALSE;
        });
        // --- continuous sanity auras ---
        SanityAuras.register((player, out) -> {
            if (CrucifixItem.isHeldForPassive(player)) out.add("crucifix", ConfigManager.get().crucifix.passiveSanityPer2Min);
            if (UnholySwordItem.isHeld(player)) {
                out.addUnscaled("unholy_sword", ConfigManager.get().unholySword.passiveSanityPer2Min);
                out.multiplyNegative(ConfigManager.get().auras.unholySwordNegativeMultiplier);
            }
        });

        // --- light: counts against the darkness aura right away, plus real light blocks ---
        LightOverrides.register(player -> {
            int lvl = 0;
            if (!FlashlightItem.activeFlashlight(player).isEmpty()) lvl = 15;
            if (CrucifixItem.isHeldForPassive(player)) lvl = Math.max(lvl, ConfigManager.get().crucifix.lightLevel);
            return lvl;
        });
        DynamicLights.register(FlashlightItem.LIGHTS);
        DynamicLights.register((player, out) -> {
            if (!CrucifixItem.isHeldForPassive(player)) return;
            BlockPos feet = player.blockPosition();
            BlockPos pos = player.level().getBlockState(feet).isAir() || player.level().getBlockState(feet).is(net.minecraft.world.level.block.Blocks.LIGHT)
                    ? feet : feet.above();
            LightSource.put(out, pos, ConfigManager.get().crucifix.lightLevel);
        });

        // --- brewing: holy water + gunpowder -> splash holy water (DECISIONS #15) ---
        FabricPotionBrewingBuilder.BUILD.register(builder -> {
            builder.addContainer(ModItems.HOLY_WATER);
            builder.addContainer(ModItems.SPLASH_HOLY_WATER);
            builder.addContainerRecipe(ModItems.HOLY_WATER, Items.GUNPOWDER, ModItems.SPLASH_HOLY_WATER);
        });

        // --- Holiness books only in mansions, end cities and mineshafts (DECISIONS #24) ---
        Set<Object> tables = Set.of(BuiltInLootTables.WOODLAND_MANSION, BuiltInLootTables.END_CITY_TREASURE, BuiltInLootTables.ABANDONED_MINESHAFT);
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            if (!source.isBuiltin() || !tables.contains(key)) return;
            var holiness = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(com.unholymc.registry.ModEnchantments.HOLINESS);
            builder.withPool(LootPool.lootPool()
                    .when(LootItemRandomChanceCondition.randomChance(ConfigManager.get().loot.holinessBookChance))
                    .add(LootItem.lootTableItem(Items.BOOK).apply(new EnchantRandomlyFunction.Builder().withEnchantment(holiness))));
        });
    }
}
