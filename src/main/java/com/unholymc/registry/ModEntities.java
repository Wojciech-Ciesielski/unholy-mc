package com.unholymc.registry;

import com.unholymc.UnholyMC;
import com.unholymc.entity.Hallucination;
import com.unholymc.entity.SlenderHallucination;
import com.unholymc.entity.ThrownHolyWater;
import com.unholymc.entity.slop.AggressiveSlop;
import com.unholymc.entity.slop.NeutralSlop;
import com.unholymc.entity.slop.UnholySlop;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final EntityType<ThrownHolyWater> THROWN_HOLY_WATER = register("splash_holy_water",
            EntityType.Builder.<ThrownHolyWater>of(ThrownHolyWater::new, MobCategory.MISC)
                    .noLootTable().sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10));

    // sizes are per variant (getDefaultDimensions); these are the largest of each group
    public static final EntityType<AggressiveSlop> AGGRESSIVE_SLOP = register("aggressive_slop",
            EntityType.Builder.of(AggressiveSlop::new, MobCategory.MONSTER).sized(0.9f, 2.0f).clientTrackingRange(10));
    public static final EntityType<NeutralSlop> NEUTRAL_SLOP = register("neutral_slop",
            EntityType.Builder.of(NeutralSlop::new, MobCategory.MONSTER).noLootTable().sized(0.9f, 1.4f).clientTrackingRange(10));
    public static final EntityType<UnholySlop> UNHOLY_SLOP = register("unholy_slop",
            EntityType.Builder.of(UnholySlop::new, MobCategory.MONSTER).sized(0.8f, 2.6f).fireImmune().clientTrackingRange(10));
    public static final EntityType<Hallucination> HALLUCINATION = register("hallucination",
            EntityType.Builder.of(Hallucination::new, MobCategory.MISC).noLootTable().sized(0.9f, 2.0f).clientTrackingRange(8));
    public static final EntityType<SlenderHallucination> SLENDERMAN_HALLUCINATION = register("slenderman_hallucination",
            EntityType.Builder.of(SlenderHallucination::new, MobCategory.MONSTER).noLootTable().sized(0.9f, 2.0f).clientTrackingRange(10));

    public static final EntityType<com.unholymc.entity.Slenderman> SLENDERMAN = register("slenderman",
            EntityType.Builder.of(com.unholymc.entity.Slenderman::new, MobCategory.MONSTER).sized(0.7f, 3.2f).eyeHeight(2.9f).clientTrackingRange(16).fireImmune());

    private ModEntities() {}

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, UnholyMC.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(AGGRESSIVE_SLOP, AggressiveSlop.createAttributes());
        FabricDefaultAttributeRegistry.register(NEUTRAL_SLOP, NeutralSlop.createAttributes());
        FabricDefaultAttributeRegistry.register(UNHOLY_SLOP, UnholySlop.createAttributes());
        FabricDefaultAttributeRegistry.register(HALLUCINATION, Hallucination.createAttributes());
        FabricDefaultAttributeRegistry.register(SLENDERMAN_HALLUCINATION, SlenderHallucination.createAttributes());
        FabricDefaultAttributeRegistry.register(SLENDERMAN, com.unholymc.entity.Slenderman.createAttributes());
    }
}
