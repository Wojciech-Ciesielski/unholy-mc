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
        register("floating_leaves", new AnomalyFeatures.FloatingLeaves(NoneFeatureConfiguration.CODEC));
        register("dead_tree", new AnomalyFeatures.DeadTree(NoneFeatureConfiguration.CODEC));
        register("cross", new AnomalyFeatures.Cross(NoneFeatureConfiguration.CODEC));
        register("skull", new AnomalyFeatures.Skull(NoneFeatureConfiguration.CODEC));
        register("blood", new AnomalyFeatures.Blood(NoneFeatureConfiguration.CODEC));
        register("abomination", new AnomalyFeatures.Abomination(NoneFeatureConfiguration.CODEC));
        register("tree_notes", new TreeNotesFeature(NoneFeatureConfiguration.CODEC, false));
        register("decoy_note", new TreeNotesFeature(NoneFeatureConfiguration.CODEC, true));

        BiomeModifications.addFeature(BiomeSelectors.tag(HOLY_POND_BIOMES), GenerationStep.Decoration.LAKES, placed("holy_pond"));
        for (String anomaly : new String[]{"floating_leaves", "dead_tree", "cross", "skull", "blood", "abomination"}) {
            BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.VEGETAL_DECORATION, placed("anomaly_" + anomaly));
        }
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, placed("anomaly_infested"));
        BiomeModifications.addFeature(BiomeSelectors.tag(com.unholymc.entity.SlopSpawning.FOREST), GenerationStep.Decoration.TOP_LAYER_MODIFICATION, placed("tree_notes"));
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld().and(BiomeSelectors.tag(com.unholymc.entity.SlopSpawning.FOREST).negate()),
                GenerationStep.Decoration.VEGETAL_DECORATION, placed("decoy_note"));
    }
}
