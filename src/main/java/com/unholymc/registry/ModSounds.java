package com.unholymc.registry;

import com.unholymc.UnholyMC;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/**
 * All mod sound events; ids mirror assets/unholymc/sounds.json (owned by the design thread,
 * spec in docs/ASSETS.md section 6). Missing .ogg files only log a warning.
 */
public final class ModSounds {
    public static final Holder.Reference<SoundEvent> JUMPSCARE_SCREAMER = register("jumpscare.screamer");
    public static final Holder.Reference<SoundEvent> JUMPSCARE_SPOOKY_BUILDUP = register("jumpscare.spooky_buildup");
    public static final Holder.Reference<SoundEvent> JUMPSCARE_SPOOKY = register("jumpscare.spooky");
    public static final Holder.Reference<SoundEvent> ENTITY_SLOP_AGGRESSIVE_AMBIENT = register("entity.slop.aggressive.ambient");
    public static final Holder.Reference<SoundEvent> ENTITY_SLOP_AGGRESSIVE_CHASE = register("entity.slop.aggressive.chase");
    public static final Holder.Reference<SoundEvent> ENTITY_SLOP_AGGRESSIVE_HURT = register("entity.slop.aggressive.hurt");
    public static final Holder.Reference<SoundEvent> ENTITY_SLOP_AGGRESSIVE_DEATH = register("entity.slop.aggressive.death");
    public static final Holder.Reference<SoundEvent> ENTITY_SLOP_NEUTRAL_AMBIENT = register("entity.slop.neutral.ambient");
    public static final Holder.Reference<SoundEvent> ENTITY_SLOP_NEUTRAL_SCARE = register("entity.slop.neutral.scare");
    public static final Holder.Reference<SoundEvent> ENTITY_SLOP_NEUTRAL_FLEE = register("entity.slop.neutral.flee");
    public static final Holder.Reference<SoundEvent> ENTITY_SLOP_NEUTRAL_HURT = register("entity.slop.neutral.hurt");
    public static final Holder.Reference<SoundEvent> ENTITY_SLOP_NEUTRAL_DEATH = register("entity.slop.neutral.death");
    public static final Holder.Reference<SoundEvent> ENTITY_SLOP_UNHOLY_AMBIENT = register("entity.slop.unholy.ambient");
    public static final Holder.Reference<SoundEvent> ENTITY_SLOP_UNHOLY_ATTACK = register("entity.slop.unholy.attack");
    public static final Holder.Reference<SoundEvent> ENTITY_SLOP_UNHOLY_HURT = register("entity.slop.unholy.hurt");
    public static final Holder.Reference<SoundEvent> ENTITY_SLOP_UNHOLY_IMMUNE = register("entity.slop.unholy.immune");
    public static final Holder.Reference<SoundEvent> ENTITY_SLOP_UNHOLY_DEATH = register("entity.slop.unholy.death");
    public static final Holder.Reference<SoundEvent> ENTITY_HALLUCINATION_SPAWN = register("entity.hallucination.spawn");
    public static final Holder.Reference<SoundEvent> ENTITY_HALLUCINATION_AMBIENT = register("entity.hallucination.ambient");
    public static final Holder.Reference<SoundEvent> ENTITY_HALLUCINATION_HURT = register("entity.hallucination.hurt");
    public static final Holder.Reference<SoundEvent> ENTITY_HALLUCINATION_DISSOLVE = register("entity.hallucination.dissolve");
    public static final Holder.Reference<SoundEvent> ENTITY_SLENDERMAN_AMBIENT = register("entity.slenderman.ambient");
    public static final Holder.Reference<SoundEvent> ENTITY_SLENDERMAN_PROXIMITY = register("entity.slenderman.proximity");
    public static final Holder.Reference<SoundEvent> ENTITY_SLENDERMAN_TELEPORT = register("entity.slenderman.teleport");
    public static final Holder.Reference<SoundEvent> ENTITY_SLENDERMAN_SUMMON = register("entity.slenderman.summon");
    public static final Holder.Reference<SoundEvent> ENTITY_SLENDERMAN_HURT = register("entity.slenderman.hurt");
    public static final Holder.Reference<SoundEvent> ENTITY_SLENDERMAN_DEATH = register("entity.slenderman.death");
    public static final Holder.Reference<SoundEvent> ENTITY_SLENDERMAN_VANISH = register("entity.slenderman.vanish");
    public static final Holder.Reference<SoundEvent> ENTITY_SLENDERMAN_DEADLY_ROAR = register("entity.slenderman.deadly_roar");
    public static final Holder.Reference<SoundEvent> MUSIC_ELEVEN_REVERSED = register("music.eleven_reversed");
    public static final Holder.Reference<SoundEvent> AMBIENT_LOW_SANITY = register("ambient.low_sanity");
    public static final Holder.Reference<SoundEvent> AMBIENT_HEARTBEAT = register("ambient.heartbeat");
    public static final Holder.Reference<SoundEvent> AMBIENT_DARK_CORNER = register("ambient.dark_corner");
    public static final Holder.Reference<SoundEvent> AMBIENT_BLOOD_MOON_RISE = register("ambient.blood_moon.rise");
    public static final Holder.Reference<SoundEvent> AMBIENT_FULL_MOON_BLESS = register("ambient.full_moon.bless");
    public static final Holder.Reference<SoundEvent> ITEM_HOLY_WATER_SIZZLE = register("item.holy_water.sizzle");
    public static final Holder.Reference<SoundEvent> ITEM_CRUCIFIX_USE = register("item.crucifix.use");
    public static final Holder.Reference<SoundEvent> ITEM_CRUCIFIX_SAVE = register("item.crucifix.save");
    public static final Holder.Reference<SoundEvent> ITEM_FLASHLIGHT_TOGGLE_ON = register("item.flashlight.toggle_on");
    public static final Holder.Reference<SoundEvent> ITEM_FLASHLIGHT_TOGGLE_OFF = register("item.flashlight.toggle_off");
    public static final Holder.Reference<SoundEvent> ITEM_FLASHLIGHT_INSERT_CORE = register("item.flashlight.insert_core");
    public static final Holder.Reference<SoundEvent> ITEM_FLASHLIGHT_REMOVE_CORE = register("item.flashlight.remove_core");
    public static final Holder.Reference<SoundEvent> ITEM_UNHOLY_SWORD_CHARGE = register("item.unholy_sword.charge");
    public static final Holder.Reference<SoundEvent> ITEM_UNHOLY_SWORD_DISCHARGED = register("item.unholy_sword.discharged");
    public static final Holder.Reference<SoundEvent> ITEM_THERMOS_DRINK = register("item.thermos.drink");
    public static final Holder.Reference<SoundEvent> ITEM_THERMOS_FILL = register("item.thermos.fill");
    public static final Holder.Reference<SoundEvent> BLOCK_TREE_NOTE_RIP = register("block.tree_note.rip");
    public static final Holder.Reference<SoundEvent> PLAYER_DEHYDRATED = register("player.dehydrated");

    public static final Holder.Reference<SoundEvent> JUMPSCARE_MEME = register("jumpscare.meme");
    public static final Holder.Reference<SoundEvent> AMBIENT_FOREST_WHISPER = register("ambient.forest.whisper");
    public static final Holder.Reference<SoundEvent> AMBIENT_FOREST_DISTANT_STEPS = register("ambient.forest.distant_steps");
    public static final Holder.Reference<SoundEvent> AMBIENT_FOREST_KNOCK = register("ambient.forest.knock");
    public static final Holder.Reference<SoundEvent> AMBIENT_FOREST_BRANCH_SNAP = register("ambient.forest.branch_snap");
    public static final Holder.Reference<SoundEvent> AMBIENT_FOREST_BREATH = register("ambient.forest.breath");
    public static final Holder.Reference<SoundEvent> AMBIENT_FOREST_TREE_CREAK = register("ambient.forest.tree_creak");
    public static final Holder.Reference<SoundEvent> AMBIENT_FOREST_BIRDS = register("ambient.forest.birds");
    public static final Holder.Reference<SoundEvent> AMBIENT_FOREST_BIRDS_SILENCE = register("ambient.forest.birds_silence");
    public static final Holder.Reference<SoundEvent> AMBIENT_WIND = register("ambient.wind");
    public static final Holder.Reference<SoundEvent> AMBIENT_SANITY_FAKE_STEPS = register("ambient.sanity.fake_steps");
    public static final Holder.Reference<SoundEvent> AMBIENT_SANITY_WALL_SCREAM = register("ambient.sanity.wall_scream");
    public static final Holder.Reference<SoundEvent> ENTITY_SLOP_STEP = register("entity.slop.step");
    public static final Holder.Reference<SoundEvent> ENTITY_SLOP_UNHOLY_STEP = register("entity.slop.unholy.step");
    public static final Holder.Reference<SoundEvent> ENTITY_SLOP_FLOAT = register("entity.slop.float");
    public static final Holder.Reference<SoundEvent> ENTITY_SLENDERMAN_STEP = register("entity.slenderman.step");
    public static final Holder.Reference<SoundEvent> ITEM_THERMOS_OPEN = register("item.thermos.open");
    public static final Holder.Reference<SoundEvent> ITEM_THERMOS_CLOSE = register("item.thermos.close");
    public static final Holder.Reference<SoundEvent> ITEM_CRUCIFIX_HIT = register("item.crucifix.hit");
    public static final Holder.Reference<SoundEvent> ITEM_HOLY_WATER_SHATTER = register("item.holy_water.shatter");
    public static final Holder.Reference<SoundEvent> ITEM_UNHOLY_SWORD_HIT = register("item.unholy_sword.hit");

    private ModSounds() {}

    private static Holder.Reference<SoundEvent> register(String name) {
        var id = UnholyMC.id(name);
        return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() {}
}
