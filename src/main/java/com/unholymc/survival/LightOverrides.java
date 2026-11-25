package com.unholymc.survival;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Extra light sources that count against the darkness aura even before the real light block
 * propagates (flashlight, crucifix: DECISIONS #1). Registered by later stages.
 */
public final class LightOverrides {
    private static final List<ToIntFunction<ServerPlayer>> SOURCES = new ArrayList<>();

    private LightOverrides() {}

    public static void register(ToIntFunction<ServerPlayer> source) {
        SOURCES.add(source);
    }

    public static boolean isLit(ServerPlayer player, int threshold) {
        for (var s : SOURCES) {
            if (s.applyAsInt(player) >= threshold) return true;
        }
        return false;
    }
}
