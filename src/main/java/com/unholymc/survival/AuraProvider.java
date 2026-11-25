package com.unholymc.survival;

import net.minecraft.server.level.ServerPlayer;

/** A source of continuous sanity auras. Later stages (items, slops, world) register their own providers. */
@FunctionalInterface
public interface AuraProvider {
    void collect(ServerPlayer player, AuraBreakdown out);
}
