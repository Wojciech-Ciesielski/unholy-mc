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
    public Slops slops = new Slops();
    public Hallucinations hallucinations = new Hallucinations();
    public Jumpscares jumpscares = new Jumpscares();
    public World world = new World();
    public BloodMoon bloodMoon = new BloodMoon();
    public HolyPonds holyPonds = new HolyPonds();
    public Ambience ambience = new Ambience();
    public Slenderman slenderman = new Slenderman();
    public DarkCorners darkCorners = new DarkCorners();

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

        /** DECISIONS #35: one threshold, in points (12 = fewer than 6 icons). Below it: no sprint, screen blinks, darkness. */
        public int lowBelow = 12;
        /** Mining Fatigue and Weakness need hydration AND hunger below this many points (DECISIONS #35). */
        public int exhaustedBelow = 12;
        public int miningFatigueAmplifier = 0;
        public int weaknessAmplifier = 0;
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
        public float slopsPer2Min = -8f;
        public double slopsRadius = 30;
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

    public static class Slops {
        /**
         * Surface spawning near players at night, independent of the vanilla monster cap (which crowds slops out).
         * Every {@code surfaceSpawnIntervalTicks} each player rolls {@code surfaceSpawnChance}; forests spawn any category
         * (unholy only in its biomes), open ground only single aggressive slops ({@code openGroundChance} on top).
         */
        /** How often (ticks, on average) an idle slop picks somewhere new to walk to (vanilla mobs: 120). */
        public int wanderIntervalTicks = 40;
        public boolean surfaceSpawning = true;
        public int surfaceSpawnIntervalTicks = 200;
        public float surfaceSpawnChance = 0.5f;
        public float openGroundChance = 0.5f;
        public int surfaceSpawnMinDistance = 24;
        public int surfaceSpawnMaxDistance = 48;
        /** No new spawn while this many slops are within 64 blocks of the player. */
        public int surfaceSpawnMaxNearby = 3;
        /** Billboard and hitbox size multiplier on top of the design sizes (applies to new spawns). */
        public float sizeMultiplier = 1.3f;
        /** Registration-time values (restart needed). Category shares 60/30/10 via spawn weights. */
        public int aggressiveSpawnWeight = 12;
        public int neutralSpawnWeight = 6;
        public int unholySpawnWeight = 2;
        public float aggressiveHealth = 20f, aggressiveDamage = 6f, aggressiveSpeed = 0.27f;
        public float neutralHealth = 10f, neutralSpeed = 0.36f;
        public float unholyHealth = 30f, unholyDamage = 10f, unholySpeed = 0.31f;
        public int unholyDarknessTicks = 200;
        public double aggressiveDespawnDistance = 80;
        public double neutralDespawnDistance = 160;
        public double unholyDespawnDistance = 80;
        /** Neutral slops: approach, scare (one-off sanity loss), then run away. */
        public double neutralScareTriggerRange = 3.5;
        /**
         * DECISIONS #38: some neutral slops are "watchers": they stand and stare, and attack only after a player looks
         * them in the eyes (view direction within about 14 degrees, in line of sight) for a moment.
         */
        public float neutralWatcherChance = 0.3f;
        public double watcherRange = 24;
        public double watcherGazeCos = 0.97;
        public int watcherGazeTicks = 10;
        public float watcherDamage = 6f;
        public double watcherSpeedMultiplier = 1.3;
        public int watcherChaseTicks = 300;
        public double neutralScareNoticeRange = 16;
        public int neutralScareCooldownTicks = 900;
        public int neutralFleeTicks = 120;
    }

    public static class Hallucinations {
        /** DECISIONS #4: one spawn attempt every 15 s within 20 blocks of a player with low sanity. */
        public int spawnIntervalTicks = 300;
        public double spawnRadius = 20;
        public double minSpawnDistance = 8;
        /** GDD 2.4: hallucinations are 4x as frequent in the End (interval divided). */
        public float endFrequencyMultiplier = 4f;
        public int maxPerPlayer = 3;
        public int lifetimeTicks = 1800;
        public float health = 10f, damage = 2f, speed = 0.28f;
        public float killSanity = 2f;
        public double vanishDistance = 48;
        /** Slenderman's summoned hallucinations (GDD 3.2). */
        public float slenderHealth = 24f, slenderDamage = 8f, slenderSpeed = 0.3f;
        public int slenderLifetimeTicks = 2400;
    }

    public static class Jumpscares {
        /** How long the screamer image stays on screen (ticks; half of it fading out). */
        public int screamerTicks = 14;
        public int rollIntervalTicks = 60;
        /** Chance per roll for sanity stages unsettled (14-10), disturbed (9-5), broken (<=4). */
        public float unsettledChance = 0.005f;
        public float disturbedChance = 0.03f;
        public float brokenChance = 0.10f;
        public float sanityLoss = -10f;
        /** Background noises at 9-5: random interval range (ticks). */
        public int lowSanitySoundMinTicks = 400;
        public int lowSanitySoundMaxTicks = 1200;
        public int heartbeatIntervalTicks = 40;
        /** Spooky Jumpscare: the server kills the player this many ticks after it starts. */
        public int spookyDeathDelayTicks = 110;
    }

    public static class World {
        /** DECISIONS #39: rain is common, sun is rare. The vanilla clear-weather countdown runs this many times faster... */
        public float rainFrequencyMultiplier = 5f;
        /** ...and rain lasts this many times longer than in vanilla. */
        public float rainDurationMultiplier = 2.5f;
        /** Storms: same idea, more frequent and a bit longer. */
        public float thunderFrequencyMultiplier = 4f;
        public float thunderDurationMultiplier = 1.5f;
        /** At least one rain or storm every N days. */
        public int guaranteedRainEveryDays = 3;
        public int guaranteedRainDurationTicks = 6000;
        /** Chance that a naturally spawned zombie wears a carved pumpkin (world anomaly). */
        public float zombiePumpkinChance = 0.12f;
        /** Surface anomalies (crosses, skulls, dead trees...): about one per grid cell of this many blocks. */
        public int anomalyGridSize = 56;
    }

    public static class BloodMoon {
        /** DECISIONS #17: full moon every 8 days (vanilla phases); chance of a Blood Moon on each full moon. */
        public float chance = 0.25f;
        /** "+200% spawns": monster caps are multiplied during a Blood Moon. */
        public float spawnCapMultiplier = 3f;
        /** Client visuals (ASSETS 5a). */
        public int transitionTicks = 200;
        public int skyColor = 0x3A0608;
        public int fogColor = 0x2A0505;
        public float fogDistanceMultiplier = 0.8f;
        public int cloudColor = 0x5A1010;
        public float starBrightnessMultiplier = 0.6f;
        public int skyLightTint = 0xFFB8B0;
        public float moonScale = 1.6f;
    }

    public static class HolyPonds {
        /** DECISIONS #16: aura +20 / 2 min within 10 blocks, wading +1 / s. Works on any night/day (user decision);
         *  bottling holy water still needs the full moon. Set true to limit both to holy nights. */
        public boolean auraOnlyWhenHoly = false;
        public float auraPer2Min = 20f;
        public double auraRadius = 10;
        public float wadingPerSecond = 1f;
        public int glowLightLevel = 7;
        public double lightRange = 48;
    }

    public static class Ambience {
        /** Looping slowed / reversed disc 11, played to every player by the server. */
        public float musicVolume = 0.35f;
        /** Length of the music track in ticks: it is started again after this long. */
        public int musicLoopTicks = 2840;
        /** Chance per second of an extra cave ambience sound while underground in the dark. */
        public float caveSoundChancePerSecond = 0.02f;
        public float musicNightBoost = 1.10f;
        public float musicCaveBoost = 1.15f;
    }

    public static class Slenderman {
        public float health = 150f;
        /** "Iron armor" level of natural protection. */
        public float armor = 15f;
        public float damage = 16f;
        public float speed = 0.3f;
        /** DECISIONS #13: Deadly Slenderman (Blood Moon) = +200% health, 24 damage. */
        public float deadlyHealthMultiplier = 3f;
        public float deadlyDamage = 24f;
        public int summonIntervalTicks = 1200;
        public int hallucinationsPerPlayer = 2;
        public double summonRadius = 100;
        public double teleportWhenFartherThan = 14;
        public int teleportCooldownTicks = 100;
        public int experience = 160;
        /** DECISIONS #20: each connected Dark Forest area has this chance to be a Slenderman forest. */
        public float forestChance = 0.10f;
        public int forestFloodFillStep = 32;
        public int forestFloodFillMaxCells = 6000;
        public double spawnMinDistance = 24, spawnMaxDistance = 40;
    }

    public static class DarkCorners {
        /** GDD 2.1: only in forests, >= 500 blocks from the world spawn, >= 750 blocks from each other. */
        public int minSpawnDistance = 500;
        /** Grid cell size; with the jitter below, candidates are always >= 750 blocks apart. */
        public int gridSize = 1000;
        public int jitter = 125;
        /** Centre aura -50 / 2 min, weakening by 5 per block (0 at 10 blocks). */
        public float centerAuraPer2Min = -50f;
        public float auraFalloffPerBlock = 5f;
        public double activeRange = 64;
        public double cloudRadius = 6;
        public int slopSpawnIntervalTicks = 200;
        public int maxSlopsNearby = 4;
        public double slopSpawnRadius = 12;
        public int moundRadius = 3;
        public float coalChance = 0.4f;
    }
}
