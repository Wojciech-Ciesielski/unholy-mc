package com.unholymc.entity;

import com.unholymc.entity.slop.SlopVariant;

/** Entities drawn as a camera-facing PNG billboard (slops and hallucinations). */
public interface BillboardEntity {
    SlopVariant variant();

    /** Hallucination look: black translucent silhouette, fullbright. */
    default boolean isHallucinationLook() {
        return false;
    }

    /** Slenderman's minions are more opaque and flash red now and then. */
    default boolean isSlenderMinion() {
        return false;
    }

    /** Chasing: the renderer adds a small position jitter. */
    boolean isChasing();
}
