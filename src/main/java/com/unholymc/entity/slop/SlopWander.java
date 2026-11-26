package com.unholymc.entity.slop;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

/**
 * Slops are restless: they pick a new place to walk to every second or two instead of vanilla's six seconds
 * (the slops stood around when nothing chased them).
 */
public class SlopWander extends WaterAvoidingRandomStrollGoal {
    public SlopWander(PathfinderMob mob, double speed, int interval) {
        super(mob, speed);
        this.interval = Math.max(10, interval);
    }
}
