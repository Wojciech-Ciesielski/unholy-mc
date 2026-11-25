package com.unholymc.survival;

import com.unholymc.attachment.ModAttachments;
import com.unholymc.attachment.PlayerStats;
import com.unholymc.config.ConfigManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Per-player streak of slept nights (DECISIONS #28). A night counts when the player wakes up
 * normally (not forcefully) after sleeping long enough - either by the night skip or naturally at dawn.
 * Letting a whole night pass without sleeping resets the streak.
 *
 * Night K spans overworld clock time [24000*K + 12000, 24000*(K+1) + 12000), i.e. evening to evening,
 * so a wake-up just before or just after the day rollover belongs to the same night.
 */
public final class SleepTracker {
    public static final long DAY_TICKS = 24000L;
    private static final List<Consumer<ServerPlayer>> DEADLY_STREAK_LISTENERS = new ArrayList<>();

    private SleepTracker() {}

    /** Called when the streak reaches the deadly value (Spooky Jumpscare, stage 3). */
    public static void onDeadlyStreak(Consumer<ServerPlayer> listener) {
        DEADLY_STREAK_LISTENERS.add(listener);
    }

    public static long nightKey(ServerPlayer player) {
        return Math.floorDiv(player.level().getOverworldClockTime() - DAY_TICKS / 2, DAY_TICKS);
    }

    /** Called from the wake-up mixin, before vanilla resets the sleep counter. */
    public static void onWokeUp(ServerPlayer player) {
        if (!StatsHelper.isActive(player)) return;
        PlayerStats s = ModAttachments.stats(player);
        long night = nightKey(player);
        if (s.lastSleepNight == night) return; // this night was already counted
        var cfg = ConfigManager.get();

        s.sleepStreak = s.lastSleepNight == night - 1 ? s.sleepStreak + 1 : 1;
        s.lastSleepNight = night;

        // Sleeping through the night restores full sanity at the cost of hunger and hydration.
        StatsHelper.setSanity(player, cfg.sanity.max);
        StatsHelper.addHydration(player, -cfg.sleep.hydrationCost);
        var food = player.getFoodData();
        food.setFoodLevel(Math.max(0, food.getFoodLevel() - Math.round(cfg.sleep.hungerCost)));

        if (s.sleepStreak >= cfg.sleep.deadlyStreak) {
            for (var l : DEADLY_STREAK_LISTENERS) l.accept(player);
        }
    }

    /** Resets the streak once the night after the last counted one has fully passed. */
    public static void checkStreak(ServerPlayer player) {
        PlayerStats s = ModAttachments.stats(player);
        if (s.sleepStreak > 0 && nightKey(player) > s.lastSleepNight + 1) {
            s.sleepStreak = 0;
        }
    }

    /** Used by /thm sleep_streak set: the next night the player sleeps through continues this streak. */
    public static void setStreak(ServerPlayer player, int value) {
        PlayerStats s = ModAttachments.stats(player);
        s.sleepStreak = Math.max(0, value);
        if (value <= 0) {
            s.lastSleepNight = Long.MIN_VALUE / 2;
            return;
        }
        long timeOfDay = Math.floorMod(player.level().getOverworldClockTime(), DAY_TICKS);
        // Before the evening the current key is the night that just ended; after it, the one that started.
        s.lastSleepNight = timeOfDay < DAY_TICKS / 2 ? nightKey(player) : nightKey(player) - 1;
    }
}
