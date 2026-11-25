package com.unholymc.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Authoritative, server-only survival state of a player. Mutable on purpose: it changes every tick,
 * so it is not synced directly. The compact {@link HudState} is synced instead, only when it changes.
 */
public final class PlayerStats {
    public static final Codec<PlayerStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("sanity", 20f).forGetter(s -> s.sanity),
            Codec.FLOAT.optionalFieldOf("hydration", 20f).forGetter(s -> s.hydration),
            Codec.INT.optionalFieldOf("sleep_streak", 0).forGetter(s -> s.sleepStreak),
            Codec.LONG.optionalFieldOf("last_sleep_night", Long.MIN_VALUE / 2).forGetter(s -> s.lastSleepNight),
            Codec.LONG.optionalFieldOf("aura_immunity_until", 0L).forGetter(s -> s.auraImmunityUntil)
    ).apply(i, PlayerStats::new));

    public float sanity;
    public float hydration;
    public int sleepStreak;
    /** Index of the last night (see SleepTracker.nightKey) the player slept through. */
    public long lastSleepNight;
    /** Game time until which negative auras / negative food effects are ignored (enchanted golden apple). */
    public long auraImmunityUntil;

    // transient runtime fields (not saved)
    public int dehydrationTimer;
    public long lastSourceSip;

    public PlayerStats(float sanity, float hydration, int sleepStreak, long lastSleepNight, long auraImmunityUntil) {
        this.sanity = sanity;
        this.hydration = hydration;
        this.sleepStreak = sleepStreak;
        this.lastSleepNight = lastSleepNight;
        this.auraImmunityUntil = auraImmunityUntil;
    }

    public static PlayerStats createDefault() {
        return new PlayerStats(20f, 20f, 0, Long.MIN_VALUE / 2, 0L);
    }
}
