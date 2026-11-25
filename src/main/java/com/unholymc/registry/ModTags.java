package com.unholymc.registry;

import com.unholymc.UnholyMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

public final class ModTags {
    public static final TagKey<Item> COOKED_FOOD = TagKey.create(Registries.ITEM, UnholyMC.id("sanity/cooked_food"));
    public static final TagKey<Item> SWEET_FOOD = TagKey.create(Registries.ITEM, UnholyMC.id("sanity/sweet_food"));
    public static final TagKey<Item> RAW_FOOD = TagKey.create(Registries.ITEM, UnholyMC.id("sanity/raw_food"));

    public static final TagKey<EntityType<?>> COMFORTING_ANIMALS = TagKey.create(Registries.ENTITY_TYPE, UnholyMC.id("sanity/comforting_animals"));
    public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, UnholyMC.id("sanity/bosses"));
    /** Mod creatures that emit the -8 slop aura (slops, hallucinations, Slenderman's minions). Filled in later stages. */
    public static final TagKey<EntityType<?>> SLOPS = TagKey.create(Registries.ENTITY_TYPE, UnholyMC.id("sanity/slops"));

    private ModTags() {}
}
