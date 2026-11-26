package com.unholymc.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.unholymc.UnholyMC;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * Overworld-wide state, stored on the overworld level and synced to every client in it.
 *
 * @param bloodMoon        the Blood Moon is active right now
 * @param rolledNight      night index (see SleepTracker.nightKey) whose full moon was already rolled
 * @param lastRainTime     game time of the last rain (for the "rain at least every 3 days" rule)
 */
public record WorldState(boolean bloodMoon, long rolledNight, long lastRainTime) {
    public static final WorldState DEFAULT = new WorldState(false, Long.MIN_VALUE / 2, 0L);

    public static final Codec<WorldState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("blood_moon", false).forGetter(WorldState::bloodMoon),
            Codec.LONG.optionalFieldOf("rolled_night", Long.MIN_VALUE / 2).forGetter(WorldState::rolledNight),
            Codec.LONG.optionalFieldOf("last_rain", 0L).forGetter(WorldState::lastRainTime)
    ).apply(i, WorldState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorldState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, WorldState::bloodMoon,
            ByteBufCodecs.VAR_LONG, WorldState::rolledNight,
            ByteBufCodecs.VAR_LONG, WorldState::lastRainTime,
            WorldState::new);

    public static final AttachmentType<WorldState> TYPE = AttachmentRegistry.create(UnholyMC.id("world_state"),
            b -> b.persistent(CODEC).initializer(() -> DEFAULT));

    public WorldState withBloodMoon(boolean v) {
        return new WorldState(v, rolledNight, lastRainTime);
    }

    public WorldState withRolledNight(long v) {
        return new WorldState(bloodMoon, v, lastRainTime);
    }

    public WorldState withLastRain(long v) {
        return new WorldState(bloodMoon, rolledNight, v);
    }

    public static void init() {}
}
