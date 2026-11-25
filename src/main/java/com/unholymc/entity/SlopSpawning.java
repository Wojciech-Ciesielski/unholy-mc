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

    public static void init() {
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
