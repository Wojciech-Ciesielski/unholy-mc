package com.unholymc.registry;

import com.unholymc.UnholyMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

public final class ModTags {
    // items
    public static final TagKey<Item> COOKED_FOOD = item("sanity/cooked_food");
    public static final TagKey<Item> SWEET_FOOD = item("sanity/sweet_food");
    public static final TagKey<Item> RAW_FOOD = item("sanity/raw_food");
    public static final TagKey<Item> FLASHLIGHT_CORES = item("flashlight_cores");
    public static final TagKey<Item> CRUCIFIX_REPAIR = item("crucifix_repair");
    public static final TagKey<Item> ENCHANTABLE_CRUCIFIX = item("enchantable/crucifix");

    // entities
    public static final TagKey<EntityType<?>> COMFORTING_ANIMALS = entity("sanity/comforting_animals");
    public static final TagKey<EntityType<?>> BOSSES = entity("sanity/bosses");
    /** Mod creatures that emit the -8 slop aura (slops, hallucinations, Slenderman's minions). Filled in stage 3. */
    public static final TagKey<EntityType<?>> SLOPS = entity("sanity/slops");
    /** All mod creatures affected by holy items (repel + blindness). */
    public static final TagKey<EntityType<?>> HOLY_VULNERABLE = entity("holy/vulnerable");
    /** Unholy Slops: take direct holy damage from splash holy water and get the 150% multiplier. */
    public static final TagKey<EntityType<?>> UNHOLY = entity("holy/unholy");
    /** Ignore holy items entirely (Slenderman). */
    public static final TagKey<EntityType<?>> HOLY_IMMUNE = entity("holy/immune");
    /** Targets of the Unholy Sword's +50% bonus (slops and hallucinations). */
    public static final TagKey<EntityType<?>> UNHOLY_SWORD_BONUS = entity("unholy_sword_bonus");

    // damage types
    public static final TagKey<DamageType> IS_HOLY = TagKey.create(Registries.DAMAGE_TYPE, UnholyMC.id("is_holy"));

    private ModTags() {}

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, UnholyMC.id(path));
    }

    private static TagKey<EntityType<?>> entity(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, UnholyMC.id(path));
    }
}
