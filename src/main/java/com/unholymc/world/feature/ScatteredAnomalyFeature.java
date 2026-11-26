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
        int size = Math.max(24, ConfigManager.get().world.anomalyGridSize);
        int chunkX = ctx.origin().getX() >> 4, chunkZ = ctx.origin().getZ() >> 4;
        // cells are in blocks (not chunk aligned); the one anomaly of a cell sits at a seed-chosen block
        // and is placed by the chunk that contains it
        int minCellX = Math.floorDiv(chunkX << 4, size), maxCellX = Math.floorDiv((chunkX << 4) + 15, size);
        int minCellZ = Math.floorDiv(chunkZ << 4, size), maxCellZ = Math.floorDiv((chunkZ << 4) + 15, size);
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                RandomSource cell = RandomSource.create(level.getSeed() ^ Mth.getSeed(cellX, 0x5EED, cellZ));
                int margin = size / 6;
                int pickX = cellX * size + margin + cell.nextInt(size - 2 * margin);
                int pickZ = cellZ * size + margin + cell.nextInt(size - 2 * margin);
                if ((pickX >> 4) != chunkX || (pickZ >> 4) != chunkZ) continue;
                Feature<NoneFeatureConfiguration> type = anomalies.get(cell.nextInt(anomalies.size())).get();
                boolean ok = tryPlace(ctx, type, pickX, pickZ);
                return ok;
            }
        }
        return false;
    }

    /**
     * Ground level below any canopy. Only the world-gen heightmaps exist while features are placed, and
     * WORLD_SURFACE_WG includes leaves and water, so walk down through leaves, logs and air to the soil.
     */
    private static BlockPos surface(WorldGenLevel level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, top, z);
        for (int i = 0; i < 40 && p.getY() > level.getMinY() + 2; i++) {
            net.minecraft.world.level.block.state.BlockState st = level.getBlockState(p.below());
            if (st.isAir() || st.is(net.minecraft.tags.BlockTags.LEAVES) || st.is(net.minecraft.tags.BlockTags.LOGS) || st.canBeReplaced() && st.getFluidState().isEmpty()) {
                p.move(0, -1, 0);
            } else {
                return st.getFluidState().isEmpty() ? p.immutable() : null;
            }
        }
        return null;
    }

    private static boolean tryPlace(FeaturePlaceContext<NoneFeatureConfiguration> ctx, Feature<NoneFeatureConfiguration> type, int pickX, int pickZ) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        for (int attempt = 0; attempt < 16; attempt++) {
            // first the chosen block, then nearby columns of the same chunk
            int x = attempt == 0 ? pickX : (pickX & ~15) + r.nextInt(16), z = attempt == 0 ? pickZ : (pickZ & ~15) + r.nextInt(16);
            BlockPos pos = surface(level, x, z);
            if (pos == null) continue;
            boolean ok = type.place(FeatureConfiguration.NONE, level, ctx.chunkGenerator(), r, pos);
            if (ok) return true;
        }
        return false;
    }
}
