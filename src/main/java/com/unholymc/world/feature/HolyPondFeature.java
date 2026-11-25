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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A small pond of irregular shape: the union of 1-4 random ellipses (round, oval, kidney or blobby),
 * 1-2 blocks deep, on fairly flat desert/savanna ground. Recorded as a holy pond.
 */
public class HolyPondFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MAX_EXTENT = 5;

    public HolyPondFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static Set<Long> shape(RandomSource r) {
        Set<Long> cells = new HashSet<>();
        int blobs = 1 + r.nextInt(4);
        double cx = 0, cz = 0;
        for (int b = 0; b < blobs; b++) {
            double rx = 1.3 + r.nextDouble() * 1.7, rz = 1.3 + r.nextDouble() * 1.7;
            for (int dx = -MAX_EXTENT; dx <= MAX_EXTENT; dx++) {
                for (int dz = -MAX_EXTENT; dz <= MAX_EXTENT; dz++) {
                    double nx = (dx - cx) / rx, nz = (dz - cz) / rz;
                    if (nx * nx + nz * nz <= 1.0) cells.add(key(dx, dz));
                }
            }
            // next blob overlaps the previous one, drifting in a random direction
            double a = r.nextDouble() * Math.PI * 2;
            cx = Math.clamp(cx + Math.cos(a) * (1.5 + r.nextDouble()), -2.5, 2.5);
            cz = Math.clamp(cz + Math.sin(a) * (1.5 + r.nextDouble()), -2.5, 2.5);
        }
        cells.add(key(0, 0));
        return cells;
    }

    private static long key(int dx, int dz) {
        return ((long) dx << 32) | (dz & 0xffffffffL);
    }

    private static int kx(long k) {
        return (int) (k >> 32);
    }

    private static int kz(long k) {
        return (int) k;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos center = ctx.origin().below();
        BlockState ground = level.getBlockState(center);
        if (!ground.isSolidRender() || !ground.getFluidState().isEmpty()) return false;
        Set<Long> cells = shape(random);

        // flat enough: the ground of every cell within 1 block of the centre height
        for (long k : cells) {
            int top = Integer.MIN_VALUE;
            for (int y = 2; y >= -2; y--) {
                if (level.getBlockState(center.offset(kx(k), y, kz(k))).isSolidRender()) { top = y; break; }
            }
            if (top < -1 || top > 1) return false;
        }

        BlockState border = ground.is(Blocks.GRASS_BLOCK) ? Blocks.DIRT.defaultBlockState()
                : ground.is(Blocks.SAND) ? Blocks.SANDSTONE.defaultBlockState()
                : ground.is(Blocks.RED_SAND) ? Blocks.RED_SANDSTONE.defaultBlockState()
                : ground.getBlock() instanceof net.minecraft.world.level.block.FallingBlock ? Blocks.STONE.defaultBlockState() : ground;
        List<Integer> packed = new ArrayList<>();
        int radius = 0;
        for (long k : cells) {
            int dx = kx(k), dz = kz(k);
            radius = Math.max(radius, Math.max(Math.abs(dx), Math.abs(dz)));
            packed.add(HolyPonds.Pond.pack(dx, dz));
            boolean interior = cells.contains(key(dx + 1, dz)) && cells.contains(key(dx - 1, dz))
                    && cells.contains(key(dx, dz + 1)) && cells.contains(key(dx, dz - 1));
            for (int y = 1; y <= 2; y++) {
                BlockPos above = center.offset(dx, y, dz);
                if (!level.getBlockState(above).isAir()) setBlock(level, above, Blocks.AIR.defaultBlockState());
            }
            for (int d = 0; d < (interior ? 2 : 1); d++) {
                setBlock(level, center.offset(dx, -d, dz), Blocks.WATER.defaultBlockState());
            }
        }
        // seal the sides and the bottom so the water stays put
        for (long k : cells) {
            for (int d = 0; d <= 1; d++) {
                BlockPos p = center.offset(kx(k), -d, kz(k));
                if (!level.getBlockState(p).is(Blocks.WATER)) continue;
                for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN}) {
                    BlockPos n = p.relative(dir);
                    BlockState ns = level.getBlockState(n);
                    if (!ns.is(Blocks.WATER) && !ns.isSolidRender()) setBlock(level, n, border);
                }
            }
        }
        HolyPonds.record(level.getChunk(center), new HolyPonds.Pond(center.immutable(), radius, List.copyOf(packed)));
        return true;
    }
}
