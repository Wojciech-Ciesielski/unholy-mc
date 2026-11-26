package com.unholymc.entity;

import com.unholymc.UnholyMC;
import com.unholymc.config.ConfigManager;
import com.unholymc.registry.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Natural spawning (GDD 4, DECISIONS #19): every Overworld biome gets the slops so caves work everywhere;
 * the placement predicates then keep aggressive slops to forests and caves, neutral ones mostly to caves
 * and unholy ones to the surface of jungles, dark forests and birch forests. No slops in the Nether / End.
 */
public final class SlopSpawning {
    public static final TagKey<Biome> FOREST = TagKey.create(Registries.BIOME, UnholyMC.id("slops/forest"));
    public static final TagKey<Biome> UNHOLY_BIOMES = TagKey.create(Registries.BIOME, UnholyMC.id("slops/unholy"));

    private SlopSpawning() {}

    /** Own night-time surface spawner (see UnholyConfig.Slops.surfaceSpawning). */
    private static void surfaceTick(net.minecraft.server.MinecraftServer server) {
        var cfg = ConfigManager.get().slops;
        if (!cfg.surfaceSpawning) return;
        net.minecraft.server.level.ServerLevel level = server.overworld();
        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL || !level.isDarkOutside()) return;
        for (net.minecraft.server.level.ServerPlayer p : level.players()) {
            if ((p.tickCount + p.getId() * 7) % Math.max(20, cfg.surfaceSpawnIntervalTicks) != 0) continue;
            if (!com.unholymc.survival.StatsHelper.isActive(p) || p.isSpectator()) continue;
            RandomSource r = p.getRandom();
            if (r.nextFloat() >= cfg.surfaceSpawnChance) continue;
            int nearby = level.getEntitiesOfClass(com.unholymc.entity.slop.AbstractSlop.class, p.getBoundingBox().inflate(64)).size();
            if (nearby >= cfg.surfaceSpawnMaxNearby) continue;
            BlockPos spot = com.unholymc.horror.HallucinationSpawner.findSpot(level, p, cfg.surfaceSpawnMinDistance, cfg.surfaceSpawnMaxDistance);
            if (spot == null || inCave(level, spot) || level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, spot) > 7) continue;
            boolean forest = level.getBiome(spot).is(FOREST);
            EntityType<? extends net.minecraft.world.entity.Mob> type = ModEntities.AGGRESSIVE_SLOP;
            if (forest) {
                float roll = r.nextFloat();
                type = roll < 0.6f ? ModEntities.AGGRESSIVE_SLOP : roll < 0.9f ? ModEntities.NEUTRAL_SLOP : ModEntities.UNHOLY_SLOP;
                if (type == ModEntities.UNHOLY_SLOP && !level.getBiome(spot).is(UNHOLY_BIOMES)) type = ModEntities.AGGRESSIVE_SLOP;
            } else if (r.nextFloat() >= cfg.openGroundChance) {
                continue; // clearings and open ground: only a single aggressive slop now and then
            }
            net.minecraft.world.entity.Mob mob = type.create(level, EntitySpawnReason.NATURAL);
            if (mob == null) continue;
            mob.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, r.nextFloat() * 360f, 0f);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), EntitySpawnReason.NATURAL, null);
            level.addFreshEntity(mob);
        }
    }

    public static void init() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(SlopSpawning::surfaceTick);
        var cfg = ConfigManager.get().slops;
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, ModEntities.AGGRESSIVE_SLOP, cfg.aggressiveSpawnWeight, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, ModEntities.NEUTRAL_SLOP, cfg.neutralSpawnWeight, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.tag(UNHOLY_BIOMES), MobCategory.MONSTER, ModEntities.UNHOLY_SLOP, cfg.unholySpawnWeight, 1, 1);

        SpawnPlacements.register(ModEntities.AGGRESSIVE_SLOP, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> dark(type, level, reason, pos, random) && (inCave(level, pos) || level.getBiome(pos).is(FOREST)));
        SpawnPlacements.register(ModEntities.NEUTRAL_SLOP, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> dark(type, level, reason, pos, random)
                        && (inCave(level, pos) || (level.getBiome(pos).is(FOREST) && random.nextInt(4) == 0)));
        SpawnPlacements.register(ModEntities.UNHOLY_SLOP, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> dark(type, level, reason, pos, random)
                        && !inCave(level, pos) && level.getBiome(pos).is(UNHOLY_BIOMES));
    }

    private static boolean dark(EntityType<? extends Monster> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }

    /** Underground: well below the surface (leaves ignored) and no sky. */
    public static boolean inCave(ServerLevelAccessor level, BlockPos pos) {
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return pos.getY() < surface - 6 && !level.canSeeSky(pos);
    }
}
