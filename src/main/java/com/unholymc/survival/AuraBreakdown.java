package com.unholymc.survival;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of one aura scan: every continuous sanity source acting on a player, in points / 2 min.
 * DECISIONS #9: one entry per aura kind (no stacking within a kind); different kinds add up.
 */
public final class AuraBreakdown {
    public record Entry(String key, float per2Min) {}

    private final List<Entry> entries = new ArrayList<>();
    private float negativeMultiplier = 1f;
    private boolean negativeImmune;

    public void add(String key, float per2Min) {
        if (per2Min != 0f) {
            entries.add(new Entry(key, per2Min));
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
        float sum = 0f;
        for (Entry e : entries) if (e.per2Min < 0f) sum += e.per2Min;
        return sum * negativeMultiplier;
    }

    /** Net change in points per 2 minutes. */
    public float total() {
        return positiveTotal() + negativeTotal();
    }
}
