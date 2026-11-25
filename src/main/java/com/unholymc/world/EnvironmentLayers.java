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
 * Extra layers on top of the vanilla environment attribute system (added to every level, client and server):
 * Blood Moon look (ASSETS 5a) and "no sleeping", and more frequent vanilla cave mood sounds (GDD 5.4).
 */
public final class EnvironmentLayers {
    /** 0..1 strength of the Blood Moon look. The client replaces this with a smoothed (200 tick) version. */
    public static ToDoubleFunction<Level> bloodMoonFactor = level -> BloodMoon.isActive(level) ? 1.0 : 0.0;

    private static final BedRule NO_SLEEP = new BedRule(BedRule.Rule.NEVER, BedRule.Rule.ALWAYS, false,
            Optional.of(Component.translatable("message.unholymc.blood_moon.no_sleep")));

    private EnvironmentLayers() {}

    public static void addLayers(EnvironmentAttributeSystem.Builder builder, Level level) {
        builder.addTimeBasedLayer(EnvironmentAttributes.BED_RULE, (base, tick) -> BloodMoon.isActive(level) ? NO_SLEEP : base);

        builder.addTimeBasedLayer(EnvironmentAttributes.SKY_COLOR, (base, tick) -> lerpColor(level, base, ConfigManager.get().bloodMoon.skyColor));
        builder.addTimeBasedLayer(EnvironmentAttributes.FOG_COLOR, (base, tick) -> lerpColor(level, base, ConfigManager.get().bloodMoon.fogColor));
        builder.addTimeBasedLayer(EnvironmentAttributes.CLOUD_COLOR, (base, tick) -> lerpColor(level, base, ConfigManager.get().bloodMoon.cloudColor));
        builder.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_COLOR, (base, tick) -> {
            float f = factor(level);
            return f <= 0 ? base : ARGB.srgbLerp(f, base, ARGB.multiply(base, 0xFF000000 | ConfigManager.get().bloodMoon.skyLightTint));
        });
        builder.addTimeBasedLayer(EnvironmentAttributes.FOG_END_DISTANCE,
                (base, tick) -> base * Mth.lerp(factor(level), 1f, ConfigManager.get().bloodMoon.fogDistanceMultiplier));
        builder.addTimeBasedLayer(EnvironmentAttributes.STAR_BRIGHTNESS,
                (base, tick) -> base * Mth.lerp(factor(level), 1f, ConfigManager.get().bloodMoon.starBrightnessMultiplier));

        builder.addTimeBasedLayer(EnvironmentAttributes.AMBIENT_SOUNDS, (base, tick) -> boostMood(base));
    }

    private static float factor(Level level) {
        return (float) Mth.clamp(bloodMoonFactor.applyAsDouble(level), 0.0, 1.0);
    }

    private static int lerpColor(Level level, int base, int target) {
        float f = factor(level);
        if (f <= 0) return base;
        return ARGB.srgbLerp(f, base, (base & 0xFF000000) | (target & 0xFFFFFF));
    }

    private static AmbientSounds boostMood(AmbientSounds base) {
        float m = ConfigManager.get().ambience.moodSoundMultiplier;
        if (m == 1f || base.mood().isEmpty() && base.additions().isEmpty()) return base;
        return new AmbientSounds(base.loop(),
                base.mood().map(mood -> new AmbientMoodSettings(mood.soundEvent(), Math.max(1, Math.round(mood.tickDelay() / m)),
                        mood.blockSearchExtent(), mood.soundPositionOffset())),
                base.additions().stream().map(a -> new AmbientAdditionsSettings(a.soundEvent(), Math.min(1.0, a.tickChance() * m))).toList());
    }
}
