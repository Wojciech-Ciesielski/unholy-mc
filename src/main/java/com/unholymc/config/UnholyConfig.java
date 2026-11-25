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
    public HolyWater holyWater = new HolyWater();
    public Crucifix crucifix = new Crucifix();
    public Flashlight flashlight = new Flashlight();
    public UnholySword unholySword = new UnholySword();
    public Thermos thermos = new Thermos();
    public Loot loot = new Loot();

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

    public static class HolyWater {
        public float drinkHydration = 8f;
        public float drinkSanity = 6f;
        /** "Thrown under yourself": players this close to the impact get the self-splash bonus. */
        public double selfSplashRadius = 2.5;
        public float selfSplashHydration = 8f;
        public float selfSplashSanity = 2f;
        public double splashRadius = 5;
        public int blindnessTicks = 200;
        public int repelTicks = 100;
        /** Damage to Unholy Slops per splash, already including the 150% holy multiplier (DECISIONS #11). */
        public float unholySlopDamage = 6f;
    }

    public static class Crucifix {
        /** Registration-time value (restart needed). */
        public int durability = 20;
        public int cooldownTicks = 40;
        public double radius = 5;
        public int blindnessTicks = 200;
        public int repelTicks = 100;
        /** Holiness I..III damage (HP) to mod creatures (before the unholy multiplier). */
        public float[] modMobDamage = {6f, 8f, 10f};
        /** Holiness I..III damage (HP) to ordinary hostile mobs. */
        public float[] regularMobDamage = {4f, 6f, 7f};
        public float unholyMultiplier = 1.5f;
        public float passiveSanityPer2Min = 6f;
        public int lightLevel = 6;
        /** DECISIONS #24 (to confirm): passive effects also work in the off hand. The totem never does. */
        public boolean passiveInOffhand = true;
        public int repairPerHolyWater = 10;
        public float totemHealth = 1f;
    }

    public static class Flashlight {
        public double rangeNone = 3;
        public double rangeLapisRedstoneEmerald = 6;
        public double rangeDiamondNetherite = 11.5;
        public double radiusNetherStar = 14;
        public double rangeNightmareGem = 6;
        /** Half-angle of the light cone (degrees), used for the nightmare gem effect. */
        public double coneHalfAngle = 30;
        public int beamLightLevel = 13;
        public int nearLightLevel = 9;
        public int updateIntervalTicks = 2;
    }

    public static class UnholySword {
        /** Registration-time values (restart needed). */
        public int durability = 666;
        public float attackDamage = 10f;
        public float attackSpeed = -2.4f;
        public float dischargedAttackDamage = 4f;
        public int repairPerGem = 50;
        public float slopDamageBonus = 0.5f;
        public float passiveSanityPer2Min = -4f;
    }

    public static class Thermos {
        public int waterPortions = 20;
        public int holyWaterPortions = 15;
        public int honeyPortions = 5;
        public int milkPortions = 5;
        public float waterHydration = 4f;
        public float holyWaterHydration = 8f;
        public float holyWaterSanity = 6f;
        public float honeyHydration = 2f;
        public float milkHydration = 10f;
        public int drinkTicks = 32;
    }

    public static class Loot {
        /** Chance that a Woodland Mansion / End City / Mineshaft chest contains a Holiness book. */
        public float holinessBookChance = 0.15f;
    }
}
