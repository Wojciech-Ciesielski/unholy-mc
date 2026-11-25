package com.unholymc.light;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/** Contributes real light blocks around a player (flashlight, crucifix). Called every light update. */
@FunctionalInterface
public interface LightSource {
    /** Put desired positions -> light level into {@code out}; keep the max if a position is already present. */
    void collect(ServerPlayer player, Map<BlockPos, Integer> out);

    static void put(Map<BlockPos, Integer> out, BlockPos pos, int level) {
        out.merge(pos.immutable(), level, Math::max);
    }
}
