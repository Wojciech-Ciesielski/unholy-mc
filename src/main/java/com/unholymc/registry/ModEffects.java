package com.unholymc.registry;

import com.unholymc.UnholyMC;
import com.unholymc.config.ConfigManager;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class ModEffects {
    /** High sanity buff: light speed + luck in one effect. */
    public static Holder<MobEffect> WELLBEING;

    private ModEffects() {}

    public static void init() {
        var cfg = ConfigManager.get().sanity;
        WELLBEING = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, UnholyMC.id("wellbeing"),
                new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xE8C26A)
                        .addAttributeModifier(Attributes.MOVEMENT_SPEED, UnholyMC.id("effect.wellbeing.speed"),
                                cfg.wellbeingSpeedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                        .addAttributeModifier(Attributes.LUCK, UnholyMC.id("effect.wellbeing.luck"),
                                cfg.wellbeingLuck, AttributeModifier.Operation.ADD_VALUE));
    }

    private static final class SimpleEffect extends MobEffect {
        SimpleEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
