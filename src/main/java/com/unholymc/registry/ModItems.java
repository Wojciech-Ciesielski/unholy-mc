package com.unholymc.registry;

import com.unholymc.UnholyMC;
import com.unholymc.config.ConfigManager;
import com.unholymc.item.CrucifixItem;
import com.unholymc.item.FlashlightItem;
import com.unholymc.item.HolyWaterItem;
import com.unholymc.item.SplashHolyWaterItem;
import com.unholymc.item.ThermosItem;
import com.unholymc.item.UnholySwordItem;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.function.Function;

public final class ModItems {
    public static Item NIGHTMARE_GEM;
    public static Item HOLY_WATER;
    public static Item SPLASH_HOLY_WATER;
    public static Item CRUCIFIX;
    public static Item FLASHLIGHT;
    public static Item UNHOLY_SWORD;
    public static Item THERMOS;

    private ModItems() {}

    public static void init() {
        var cfg = ConfigManager.get();
        NIGHTMARE_GEM = register("nightmare_gem", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
        HOLY_WATER = register("holy_water", HolyWaterItem::new, new Item.Properties().stacksTo(1)
                .component(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER))
                .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
                .usingConvertsTo(Items.GLASS_BOTTLE));
        SPLASH_HOLY_WATER = register("splash_holy_water", SplashHolyWaterItem::new, new Item.Properties().stacksTo(1)
                .component(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER)));
        CRUCIFIX = register("crucifix", CrucifixItem::new, new Item.Properties()
                .durability(cfg.crucifix.durability).enchantable(15).repairable(ModTags.CRUCIFIX_REPAIR).rarity(Rarity.UNCOMMON));
        FLASHLIGHT = register("flashlight", FlashlightItem::new, new Item.Properties().stacksTo(1)
                .component(ModComponents.FLASHLIGHT, ModComponents.FlashlightData.EMPTY));
        HolderGetter<Block> blocks = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        UNHOLY_SWORD = register("unholy_sword", UnholySwordItem::new, new Item.Properties()
                .durability(cfg.unholySword.durability).enchantable(15).repairable(NIGHTMARE_GEM).rarity(Rarity.EPIC)
                .attributes(UnholySwordItem.attributes(cfg.unholySword.attackDamage, cfg.unholySword.attackSpeed))
                .component(DataComponents.WEAPON, new Weapon(1))
                .component(DataComponents.TOOL, new Tool(List.of(
                        Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15f),
                        Tool.Rule.overrideSpeed(blocks.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES), Float.MAX_VALUE),
                        Tool.Rule.overrideSpeed(blocks.getOrThrow(BlockTags.SWORD_EFFICIENT), 1.5f)), 1f, 2, false)));
        THERMOS = register("thermos", ThermosItem::new, new Item.Properties().stacksTo(1)
                .component(ModComponents.THERMOS, ModComponents.ThermosContents.EMPTY));

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, UnholyMC.id("main"), FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.unholymc"))
                .icon(() -> new ItemStack(CRUCIFIX))
                .displayItems((params, output) -> {
                    output.accept(NIGHTMARE_GEM);
                    output.accept(HOLY_WATER);
                    output.accept(SPLASH_HOLY_WATER);
                    output.accept(CRUCIFIX);
                    output.accept(FLASHLIGHT);
                    output.accept(UNHOLY_SWORD);
                    output.accept(THERMOS);
                    for (String fluid : ThermosItem.FLUIDS) {
                        ItemStack full = new ItemStack(THERMOS);
                        ThermosItem.fill(full, fluid);
                        output.accept(full);
                    }
                })
                .build());
    }

    private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, UnholyMC.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
    }
}
