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

    /** 2-15 notes on trunks around the origin. */
    private boolean placeOnTrees(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        int wanted = 2 + r.nextInt(14);
        int placed = 0;
        for (int attempt = 0; attempt < wanted * 6 && placed < wanted; attempt++) {
            int x = ctx.origin().getX() + r.nextInt(16) - 8, z = ctx.origin().getZ() + r.nextInt(16) - 8;
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            for (int y = ground; y < ground + 3 && placed < wanted; y++) {
                BlockPos log = new BlockPos(x, y, z);
                if (!level.getBlockState(log).is(BlockTags.LOGS)) continue;
                Direction face = Direction.Plane.HORIZONTAL.getRandomDirection(r);
                BlockPos at = log.relative(face);
                if (level.getBlockState(at).isAir()) {
                    level.setBlock(at, note(r, face), Block.UPDATE_CLIENTS);
                    placed++;
                    break;
                }
            }
        }
        return placed > 0;
    }

    /** Decoy: a lone note on a dead stump in the open, >= 200 blocks from any Slenderman forest. */
    private boolean placeDecoy(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos base = ctx.origin();
        if (!AnomalyFeatures.groundOk(level, base) || !AnomalyFeatures.groundOk(level, base)) return false;
        if (SlenderForests.slenderForestWithin(level.getLevel(), base.getX(), base.getZ(), 200)) return false;
        int h = 1 + r.nextInt(2);
        for (int y = 0; y < h; y++) setBlock(level, base.above(y), Blocks.DARK_OAK_LOG.defaultBlockState());
        Direction face = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        BlockPos at = base.above(h - 1).relative(face);
        if (level.getBlockState(at).isAir() || level.getBlockState(at).canBeReplaced()) setBlock(level, at, note(r, face));
        return true;
    }
}
