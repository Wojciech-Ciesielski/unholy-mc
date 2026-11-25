package com.unholymc.world.feature;

import com.mojang.serialization.Codec;
import com.unholymc.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.List;
import java.util.function.Supplier;

/**
 * Spreads the surface anomalies evenly: the world is split into cells of {@code world.anomalyGridSize} blocks
 * (default 128) and each cell gets exactly one random anomaly in one seed-chosen chunk, so they show up roughly
 * every 100-150 blocks instead of in random clumps.
 */
public class ScatteredAnomalyFeature extends Feature<NoneFeatureConfiguration> {
    private final List<Supplier<Feature<NoneFeatureConfiguration>>> anomalies;

    public ScatteredAnomalyFeature(Codec<NoneFeatureConfiguration> codec, List<Supplier<Feature<NoneFeatureConfiguration>>> anomalies) {
        super(codec);
        this.anomalies = anomalies;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        int cellChunks = Math.max(1, ConfigManager.get().world.anomalyGridSize / 16);
        int chunkX = ctx.origin().getX() >> 4, chunkZ = ctx.origin().getZ() >> 4;
        int cellX = Math.floorDiv(chunkX, cellChunks), cellZ = Math.floorDiv(chunkZ, cellChunks);
        RandomSource cell = RandomSource.create(level.getSeed() ^ Mth.getSeed(cellX, 0x5EED, cellZ));
        // keep the chosen chunk off the cell border so neighbouring anomalies are not glued together
        int margin = cellChunks >= 4 ? 1 : 0;
        int pickX = cellX * cellChunks + margin + cell.nextInt(cellChunks - 2 * margin);
        int pickZ = cellZ * cellChunks + margin + cell.nextInt(cellChunks - 2 * margin);
        if (pickX != chunkX || pickZ != chunkZ) return false;

        Feature<NoneFeatureConfiguration> type = anomalies.get(cell.nextInt(anomalies.size())).get();
        RandomSource r = ctx.random();
        for (int attempt = 0; attempt < 12; attempt++) {
            int x = (chunkX << 4) + r.nextInt(16), z = (chunkZ << 4) + r.nextInt(16);
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (type.place(FeatureConfiguration.NONE, level, ctx.chunkGenerator(), r, pos)) return true;
        }
        return false;
    }
}
