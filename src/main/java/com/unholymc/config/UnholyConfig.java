package com.unholymc.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side configuration. Every number of the design lives here (no hardcoded gameplay values).
 * Rates written as "per2Min" are points per 2 minutes (2400 ticks) and are applied smoothly every tick.
 * Serialized as JSON to config/unholymc.json; missing fields fall back to these defaults.
 */
public class UnholyConfig {
    public General general = new General();
    public Hydration hydration = new Hydration();
    public Sanity sanity = new Sanity();
    public Auras auras = new Auras();
    public Consumption consumption = new Consumption();
    public Sleep sleep = new Sleep();

    public static class General {
        /** How often (ticks) aura scans and threshold effects are evaluated. Rates are still applied every tick. */
        public int scanIntervalTicks = 20;
        /** Duration (ticks) of refreshed status effects; should be > scanIntervalTicks. */
        public int effectRefreshTicks = 60;
    }

    public static class Hydration {
        public float max = 20f;
        public float basePer2Min = 2f;
        public float dryBiomePer2Min = 4f;
        public float wetBiomePer2Min = 1f;
        public float exertionExtraPer2Min = 4f;
        /** DECISIONS #7: wet biome + sprint = -3 pts / min. */
        public float wetBiomeExertionPer2Min = 6f;
        /** DECISIONS #7: Nether = dry biome x2. */
        public float netherPer2Min = 8f;
        /** DECISIONS #7: Nether + sprint = 12 / 2 min (8 + 4). */
        public float netherExertionExtraPer2Min = 4f;
        public List<String> dryBiomes = new ArrayList<>(List.of("minecraft:desert", "#minecraft:is_savanna", "#minecraft:is_badlands", "#minecraft:is_jungle"));
        public List<String> wetBiomes = new ArrayList<>(List.of("minecraft:mushroom_fields", "minecraft:swamp", "minecraft:mangrove_swamp"));

        /** Threshold stages (inclusive upper bounds). */
        public int mildMax = 14;      // 14-10: blur + mining fatigue
        public int moderateMax = 9;   // 9-5: no sprint, nausea, weakness I
        public int severeMax = 4;     // 4-0: blink, strong blur, nausea, weakness II
        public int miningFatigueAmplifier = 0;
        public int weaknessModerateAmplifier = 0;
        public int weaknessSevereAmplifier = 1;
        public boolean nauseaEnabled = true;
        /** Damage while at exactly 0 (DECISIONS #30: independent of difficulty). */
        public float dehydrationDamage = 1f;
        public int dehydrationDamageIntervalTicks = 20;
    }

    public static class Sanity {
        public float max = 20f;
        /** 20-15: Wellbeing (speed + luck). */
        public int wellbeingMin = 15;
        public int wellbeingAmplifier = 0;
        /** Applied at registration (restart needed). */
        public double wellbeingSpeedBonus = 0.1;
        public double wellbeingLuck = 1.0;
        /** Upper bounds of the lower stages (used by jumpscares/hallucinations later). */
        public int unsettledMax = 14;
        public int disturbedMax = 9;
        public int brokenMax = 4;
        /** DECISIONS #4 (to confirm): "low sanity" for hallucinations = <= 9. */
        public int lowSanityThreshold = 9;
    }

    public static class Auras {
        public float nightPer2Min = -2f;
        public float darknessPer2Min = -4f;
        public int darknessLightBelow = 5;
        public float monstersPer2Min = -5f;
        public double monstersRadius = 10;
        public float bossesPer2Min = -8f;
        public double bossesRadius = 20;
        public float friendlyAnimalsPer2Min = 4f;
        public double friendlyAnimalsRadius = 10;
        public float tamedPer2Min = 8f;
        public double tamedRadius = 10;
        public float playersPer2MinEach = 4f;
        public float playersPer2MinMax = 20f;
        public double playersRadius = 10;
        /** Multiplier for negative continuous auras while holding the Unholy Sword (DECISIONS #10). */
        public float unholySwordNegativeMultiplier = 1.5f;
        /** Multiplier of the night aura during a Blood Moon. */
        public float bloodMoonNightMultiplier = 3f;
    }

    public static class Consumption {
        public float waterBottleHydration = 4f;
        public float honeyBottleHydration = 2f;
        public float sourceSipHydration = 4f;
        public int sourceSipCooldownTicks = 30;
        public double sourceSipReach = 4.5;
        public float cookedFoodSanity = 2f;
        public float sweetFoodSanity = 4f;
        public float cakeSliceSanity = 2f;
        public float rawFoodSanity = -3f;
        public float enchantedGoldenAppleSanity = 20f;
        public int enchantedGoldenAppleImmunityTicks = 1200;
    }

    public static class Sleep {
        public float hungerCost = 10f;
        public float hydrationCost = 10f;
        public int deadlyStreak = 10;
    }
}
