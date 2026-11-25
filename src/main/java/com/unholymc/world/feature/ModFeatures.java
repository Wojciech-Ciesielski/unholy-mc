package com.unholymc.world.feature;

import com.unholymc.UnholyMC;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Custom world-gen features and their biome placement (configured/placed JSON in data/unholymc/worldgen). */
public final class ModFeatures {
    public static final TagKey<Biome> HOLY_POND_BIOMES = TagKey.create(Registries.BIOME, UnholyMC.id("holy_pond_biomes"));

    private ModFeatures() {}

    private static <F extends Feature<?>> F register(String name, F feature) {
        return Registry.register(BuiltInRegistries.FEATURE, UnholyMC.id(name), feature);
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, UnholyMC.id(name));
    }

    public static void init() {
        register("holy_pond", new HolyPondFeature(NoneFeatureConfiguration.CODEC));
        var leaves = register("floating_leaves", new AnomalyFeatures.FloatingLeaves(NoneFeatureConfiguration.CODEC));
        var deadTree = register("dead_tree", new AnomalyFeatures.DeadTree(NoneFeatureConfiguration.CODEC));
        var cross = register("cross", new AnomalyFeatures.Cross(NoneFeatureConfiguration.CODEC));
        var skull = register("skull", new AnomalyFeatures.Skull(NoneFeatureConfiguration.CODEC));
        var blood = register("blood", new AnomalyFeatures.Blood(NoneFeatureConfiguration.CODEC));
        var abomination = register("abomination", new AnomalyFeatures.Abomination(NoneFeatureConfiguration.CODEC));
        register("scattered_anomaly", new ScatteredAnomalyFeature(NoneFeatureConfiguration.CODEC,
                java.util.List.of(() -> leaves, () -> deadTree, () -> cross, () -> skull, () -> blood, () -> abomination)));
        register("tree_notes", new TreeNotesFeature(NoneFeatureConfiguration.CODEC, false));
        register("decoy_note", new TreeNotesFeature(NoneFeatureConfiguration.CODEC, true));

        BiomeModifications.addFeature(BiomeSelectors.tag(HOLY_POND_BIOMES), GenerationStep.Decoration.LAKES, placed("holy_pond"));
        // single anomaly roughly every 100-150 blocks; the individual anomaly_* features stay available for /place
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.TOP_LAYER_MODIFICATION, placed("anomaly"));
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, placed("anomaly_infested"));
        BiomeModifications.addFeature(BiomeSelectors.tag(com.unholymc.entity.SlopSpawning.FOREST), GenerationStep.Decoration.TOP_LAYER_MODIFICATION, placed("tree_notes"));
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld().and(BiomeSelectors.tag(com.unholymc.entity.SlopSpawning.FOREST).negate()),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION, placed("decoy_note"));
    }
}
