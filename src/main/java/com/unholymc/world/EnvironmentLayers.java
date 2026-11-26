package com.unholymc.world;

import com.unholymc.config.ConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.AmbientAdditionsSettings;
import net.minecraft.world.attribute.AmbientMoodSettings;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.ToDoubleFunction;

/**
 * Extra layer on top of the vanilla environment attribute system: no sleeping during a Blood Moon.
 * (Its look is done for vanilla clients by ServerFx: red screen tint, title, sound.)
 */
public final class EnvironmentLayers {
    private static final BedRule NO_SLEEP = new BedRule(BedRule.Rule.NEVER, BedRule.Rule.ALWAYS, false,
            Optional.of(Component.translatable("message.unholymc.blood_moon.no_sleep")));

    private EnvironmentLayers() {}

    public static void addLayers(EnvironmentAttributeSystem.Builder builder, Level level) {
        builder.addTimeBasedLayer(EnvironmentAttributes.BED_RULE, (base, tick) -> BloodMoon.isActive(level) ? NO_SLEEP : base);

    }

}
