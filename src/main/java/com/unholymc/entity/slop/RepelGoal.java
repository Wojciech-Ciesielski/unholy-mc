package com.unholymc.entity.slop;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.Supplier;

/** Runs away from a point while {@code active} is true (holy items, neutral slop after a scare). */
public class RepelGoal extends Goal {
    private final PathfinderMob mob;
    private final double speed;
    private final Supplier<Vec3> source;

    public RepelGoal(PathfinderMob mob, double speed, Supplier<Vec3> source) {
        this.mob = mob;
        this.speed = speed;
        this.source = source;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        return source.get() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return source.get() != null;
    }

    @Override
    public void start() {
        mob.setTarget(null);
    }

    @Override
    public void tick() {
        Vec3 from = source.get();
        if (from == null) return;
        if (mob.getNavigation().isDone() || mob.tickCount % 20 == 0) {
            Vec3 away = DefaultRandomPos.getPosAway(mob, 16, 7, from);
            if (away != null) mob.getNavigation().moveTo(away.x, away.y, away.z, speed);
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
