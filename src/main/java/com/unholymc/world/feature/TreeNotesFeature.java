package com.unholymc.world.feature;

import com.mojang.serialization.Codec;
import com.unholymc.block.TreeNoteBlock;
import com.unholymc.registry.ModBlocks;
import com.unholymc.world.SlenderForests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Notes pinned to tree trunks (GDD 2.2): {@code decoy} = a lone note on a stump out in the open. */
public class TreeNotesFeature extends Feature<NoneFeatureConfiguration> {
    private final boolean decoy;

    public TreeNotesFeature(Codec<NoneFeatureConfiguration> codec, boolean decoy) {
        super(codec);
        this.decoy = decoy;
    }

    static BlockState note(RandomSource r, Direction facing) {
        TreeNoteBlock.Variant[] v = TreeNoteBlock.Variant.values();
        return ModBlocks.TREE_NOTE.defaultBlockState().setValue(LadderBlock.FACING, facing).setValue(TreeNoteBlock.VARIANT, v[r.nextInt(v.length)]);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        return decoy ? placeDecoy(ctx) : placeOnTrees(ctx);
    }

    /** Forest: 1-3 notes, each on a different random tree somewhere in this chunk. */
    private boolean placeOnTrees(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        return pinToRandomTrees(ctx.level(), ctx.random(), ctx.origin(), 1 + ctx.random().nextInt(3)) > 0;
    }

    /** Decoy: a single note on a random tree out in the open, >= 200 blocks from any Slenderman forest. */
    private boolean placeDecoy(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        BlockPos o = ctx.origin();
        if (SlenderForests.slenderForestWithin(ctx.level().getLevel(), o.getX(), o.getZ(), 200)) return false;
        return pinToRandomTrees(ctx.level(), ctx.random(), o, 1) > 0;
    }

    /**
     * Finds natural trees (a log column standing on the ground with leaves at its top) at random columns of the
     * origin's chunk and pins one note per tree, 1-3 blocks above the ground on a random free side of the trunk.
     */
    static int pinToRandomTrees(WorldGenLevel level, RandomSource r, BlockPos origin, int wanted) {
        int cx = origin.getX() & ~15, cz = origin.getZ() & ~15;
        java.util.Set<Long> used = new java.util.HashSet<>();
        int placed = 0;
        for (int attempt = 0; attempt < 64 && placed < wanted; attempt++) {
            int x = cx + r.nextInt(16), z = cz + r.nextInt(16);
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos topPos = new BlockPos(x, top, z);
            if (!level.getBlockState(topPos).is(BlockTags.LOGS)) continue;
            int ground = top;
            while (ground > top - 32 && level.getBlockState(new BlockPos(x, ground - 1, z)).is(BlockTags.LOGS)) ground--;
            if (top - ground < 2 || !level.getBlockState(new BlockPos(x, ground - 1, z)).is(BlockTags.DIRT)) continue;
            if (!hasLeavesNear(level, topPos)) continue; // a real tree, not a log pile or a building
            // one note per tree: neighbouring trunk columns (2x2 dark oaks) count as the same tree
            long key = ((long) (x >> 2) << 32) ^ (z >> 2);
            if (!used.add(key)) continue;
            BlockPos log = new BlockPos(x, ground + 1 + r.nextInt(Math.min(3, top - ground)), z);
            Direction start = Direction.Plane.HORIZONTAL.getRandomDirection(r);
            for (int i = 0; i < 4; i++) {
                Direction face = Direction.from2DDataValue((start.get2DDataValue() + i) & 3);
                BlockPos at = log.relative(face);
                if (level.getBlockState(at).isAir()) {
                    level.setBlock(at, note(r, face), Block.UPDATE_CLIENTS);
                    placed++;
                    break;
                }
            }
        }
        return placed;
    }

    private static boolean hasLeavesNear(WorldGenLevel level, BlockPos top) {
        for (BlockPos p : BlockPos.betweenClosed(top.offset(-2, -1, -2), top.offset(2, 2, 2))) {
            if (level.getBlockState(p).is(BlockTags.LEAVES)) return true;
        }
        return false;
    }
}
