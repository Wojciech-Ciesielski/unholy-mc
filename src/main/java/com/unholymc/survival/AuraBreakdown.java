package com.unholymc.survival;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of one aura scan: every continuous sanity source acting on a player, in points / 2 min.
 * DECISIONS #9: one entry per aura kind (no stacking within a kind); different kinds add up.
 */
public final class AuraBreakdown {
    /** {@code scaled}: affected by the negative multiplier (all auras except the Unholy Sword's own drain). */
    public record Entry(String key, float per2Min, boolean scaled) {
        public Entry(String key, float per2Min) {
            this(key, per2Min, true);
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private float negativeMultiplier = 1f;
    private boolean negativeImmune;

    public void add(String key, float per2Min) {
        if (per2Min != 0f) {
            entries.add(new Entry(key, per2Min));
        }
    }

    /** An aura not scaled by the negative multiplier (GDD 5.2: the sword's own -4 plus +50% to every other aura). */
    public void addUnscaled(String key, float per2Min) {
        if (per2Min != 0f) {
            entries.add(new Entry(key, per2Min, false));
        }
    }

    /** Multipliers stack multiplicatively (e.g. Unholy Sword x1.5). Applies only to continuous negative auras. */
    public void multiplyNegative(float factor) {
        negativeMultiplier *= factor;
    }

    public void setNegativeImmune(boolean immune) {
        this.negativeImmune = immune;
    }

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public float negativeMultiplier() {
        return negativeMultiplier;
    }

    public boolean negativeImmune() {
        return negativeImmune;
    }

    public float positiveTotal() {
        float sum = 0f;
        for (Entry e : entries) if (e.per2Min > 0f) sum += e.per2Min;
        return sum;
    }

    public float negativeTotal() {
        if (negativeImmune) return 0f;
        float scaled = 0f, unscaled = 0f;
        for (Entry e : entries) {
            if (e.per2Min >= 0f) continue;
            if (e.scaled) scaled += e.per2Min;
            else unscaled += e.per2Min;
        }
        return scaled * negativeMultiplier + unscaled;
    }

    /** Net change in points per 2 minutes. */
    public float total() {
        return positiveTotal() + negativeTotal();
    }
}
