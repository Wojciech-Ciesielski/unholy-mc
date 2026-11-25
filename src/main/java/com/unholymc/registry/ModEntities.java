package com.unholymc.registry;

import com.unholymc.UnholyMC;
import com.unholymc.entity.ThrownHolyWater;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final EntityType<ThrownHolyWater> THROWN_HOLY_WATER = register("splash_holy_water",
            EntityType.Builder.<ThrownHolyWater>of(ThrownHolyWater::new, MobCategory.MISC)
                    .noLootTable().sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10));

    private ModEntities() {}

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, UnholyMC.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void init() {}
}
