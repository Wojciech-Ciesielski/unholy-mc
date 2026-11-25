package com.unholymc.registry;

import com.unholymc.UnholyMC;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/**
 * All mod sound events. sounds.json currently points them at vanilla placeholder events;
 * see docs/SOUNDS.md for the .ogg files the user will provide.
 */
public final class ModSounds {
    public static final Holder.Reference<SoundEvent> JUMPSCARE = register("jumpscare");
    public static final Holder.Reference<SoundEvent> SPOOKY_JUMPSCARE = register("spooky_jumpscare");
    public static final Holder.Reference<SoundEvent> AMBIENT_ELEVEN_REVERSED = register("ambient.eleven_reversed");
    public static final Holder.Reference<SoundEvent> AMBIENT_LOW_SANITY = register("ambient.low_sanity");
    public static final Holder.Reference<SoundEvent> DRINK_SOURCE = register("player.drink_source");
    public static final Holder.Reference<SoundEvent> HEARTBEAT = register("player.heartbeat");
    public static final Holder.Reference<SoundEvent> SLOP_AGGRESSIVE_AMBIENT = register("entity.slop_aggressive.ambient");
    public static final Holder.Reference<SoundEvent> SLOP_AGGRESSIVE_HURT = register("entity.slop_aggressive.hurt");
    public static final Holder.Reference<SoundEvent> SLOP_AGGRESSIVE_DEATH = register("entity.slop_aggressive.death");
    public static final Holder.Reference<SoundEvent> SLOP_NEUTRAL_SCREAM = register("entity.slop_neutral.scream");
    public static final Holder.Reference<SoundEvent> SLOP_NEUTRAL_HURT = register("entity.slop_neutral.hurt");
    public static final Holder.Reference<SoundEvent> SLOP_UNHOLY_AMBIENT = register("entity.slop_unholy.ambient");
    public static final Holder.Reference<SoundEvent> SLOP_UNHOLY_HURT = register("entity.slop_unholy.hurt");
    public static final Holder.Reference<SoundEvent> SLOP_UNHOLY_DEATH = register("entity.slop_unholy.death");
    public static final Holder.Reference<SoundEvent> HALLUCINATION_AMBIENT = register("entity.hallucination.ambient");
    public static final Holder.Reference<SoundEvent> HALLUCINATION_VANISH = register("entity.hallucination.vanish");
    public static final Holder.Reference<SoundEvent> SLENDERMAN_AMBIENT = register("entity.slenderman.ambient");
    public static final Holder.Reference<SoundEvent> SLENDERMAN_TELEPORT = register("entity.slenderman.teleport");
    public static final Holder.Reference<SoundEvent> SLENDERMAN_SUMMON = register("entity.slenderman.summon");
    public static final Holder.Reference<SoundEvent> SLENDERMAN_DEATH = register("entity.slenderman.death");
    public static final Holder.Reference<SoundEvent> CRUCIFIX_USE = register("item.crucifix.use");
    public static final Holder.Reference<SoundEvent> CRUCIFIX_SAVE = register("item.crucifix.save");
    public static final Holder.Reference<SoundEvent> FLASHLIGHT_TOGGLE = register("item.flashlight.toggle");
    public static final Holder.Reference<SoundEvent> THERMOS_DRINK = register("item.thermos.drink");
    public static final Holder.Reference<SoundEvent> THERMOS_FILL = register("item.thermos.fill");
    public static final Holder.Reference<SoundEvent> UNHOLY_SWORD_HUM = register("item.unholy_sword.hum");
    public static final Holder.Reference<SoundEvent> BLOOD_MOON_START = register("event.blood_moon.start");
    public static final Holder.Reference<SoundEvent> DARK_CORNER_AMBIENT = register("block.dark_corner.ambient");

    private ModSounds() {}

    private static Holder.Reference<SoundEvent> register(String name) {
        var id = UnholyMC.id(name);
        return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() {}
}
