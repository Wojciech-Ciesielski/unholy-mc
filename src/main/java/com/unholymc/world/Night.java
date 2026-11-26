package com.unholymc.world;

import net.minecraft.world.level.Level;

/**
 * Night by the clock, not by light. Vanilla's {@code isDarkOutside()} also turns true in daytime rain and storms, which
 * (now that rain is common, DECISIONS #39) would make a Blood Moon outlast the night, keep daytime slops around and
 * drain sanity "at night" on rainy days. The window is vanilla's bed window (12542 - 23459).
 */
public final class Night {
    private Night() {}

    public static boolean isNight(Level level) {
        if (level.dimension() != Level.OVERWORLD) return false;
        long t = Math.floorMod(level.getOverworldClockTime(), 24000L);
        return t >= 12542L && t <= 23459L;
    }
}
