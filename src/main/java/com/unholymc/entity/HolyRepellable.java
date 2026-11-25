package com.unholymc.entity;

import net.minecraft.world.phys.Vec3;

/** Mod creatures that flee from holy items (holy water, crucifix). Implemented by slops in stage 3. */
public interface HolyRepellable {
    void repelFrom(Vec3 source, int ticks);
}
