package com.unholymc.entity;

import net.minecraft.world.entity.player.Player;

/** An aura-emitting creature that only affects some players (a hallucination only affects the one who sees it). */
public interface AuraFilter {
    boolean affects(Player player);
}
