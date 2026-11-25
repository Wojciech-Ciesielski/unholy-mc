package com.unholymc.registry;

import com.unholymc.UnholyMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

/** Damage types are data-driven (data/unholymc/damage_type/*.json); these are just the keys. */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> DEHYDRATION = ResourceKey.create(Registries.DAMAGE_TYPE, UnholyMC.id("dehydration"));
    public static final ResourceKey<DamageType> HOLY = ResourceKey.create(Registries.DAMAGE_TYPE, UnholyMC.id("holy"));
    public static final ResourceKey<DamageType> SPOOKY_JUMPSCARE = ResourceKey.create(Registries.DAMAGE_TYPE, UnholyMC.id("spooky_jumpscare"));

    private ModDamageTypes() {}
}
