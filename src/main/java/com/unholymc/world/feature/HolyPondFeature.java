package com.unholymc.world.feature;

import com.mojang.serialization.Codec;
import com.unholymc.world.HolyPonds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A small (radius 2-3) round pond, 1-2 blocks deep, on flat desert/savanna ground. Recorded as a holy pond. */
public class HolyPondFeature extends Feature<NoneFeatureConfiguration> {
    public HolyPondFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos surface = ctx.origin();
        BlockPos center = surface.below();
        BlockState ground = level.getBlockState(center);
        if (!ground.isSolidRender() || !ground.getFluidState().isEmpty()) return false;
        int r = 2 + random.nextInt(2);

        // flat enough: the ground of every column of the pond within 1 block of the centre height
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r + 0.5) continue;
                int top = Integer.MIN_VALUE;
                for (int y = 2; y >= -2; y--) {
                    if (level.getBlockState(center.offset(dx, y, dz)).isSolidRender()) { top = y; break; }
                }
                if (top < -1 || top > 1) return false;
            }
        }

        BlockState border = ground.is(Blocks.GRASS_BLOCK) ? Blocks.DIRT.defaultBlockState()
                : ground.is(Blocks.SAND) ? Blocks.SANDSTONE.defaultBlockState()
                : ground.is(Blocks.RED_SAND) ? Blocks.RED_SANDSTONE.defaultBlockState()
                : ground.getBlock() instanceof net.minecraft.world.level.block.FallingBlock ? Blocks.STONE.defaultBlockState() : ground;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > r * r + 0.5) continue;
                int depth = d2 <= (r - 1) * (r - 1) ? 2 : 1;
                for (int y = 1; y <= 2; y++) {
                    BlockPos above = center.offset(dx, y, dz);
                    if (!level.getBlockState(above).isAir()) setBlock(level, above, Blocks.AIR.defaultBlockState());
                }
                for (int d = 0; d < depth; d++) {
                    setBlock(level, center.offset(dx, -d, dz), Blocks.WATER.defaultBlockState());
                }
            }
        }
        // seal the sides and the bottom so the water stays put
        for (int dx = -r - 1; dx <= r + 1; dx++) {
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                for (int d = 0; d <= 2; d++) {
                    BlockPos p = center.offset(dx, -d, dz);
                    if (!level.getBlockState(p).is(Blocks.WATER)) continue;
                    for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN}) {
                        BlockPos n = p.relative(dir);
                        BlockState ns = level.getBlockState(n);
                        if (!ns.is(Blocks.WATER) && !ns.isSolidRender()) setBlock(level, n, border);
                    }
                }
            }
        }
        HolyPonds.record(level.getChunk(center), new HolyPonds.Pond(center.immutable(), r));
        return true;
    }
}
