package com.unholymc.world.feature;

import com.mojang.serialization.Codec;
import com.unholymc.registry.ModBlocks;
import com.unholymc.block.GraveCrossBlock;
import com.unholymc.world.Graves;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A lonely grave: coarse dirt mound, cross at the head, skull, webs and dead plants; a bone-capped barrel with loot 3 blocks down. */
public class GraveFeature extends Feature<NoneFeatureConfiguration> {
    public GraveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        int x = o.getX() + r.nextInt(16), z = o.getZ() + r.nextInt(16);
        BlockPos base = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        Direction along = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        Direction side = along.getClockWise();
        // flat enough, dry, on soil
        for (int i = -1; i <= 3; i++) {
            for (int s = -1; s <= 1; s++) {
                BlockPos col = base.relative(along, i).relative(side, s);
                BlockPos top = new BlockPos(col.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col.getX(), col.getZ()), col.getZ());
                if (Math.abs(top.getY() - base.getY()) > 1) return false;
                BlockState ground = level.getBlockState(top.below());
                if (!ground.isSolidRender() || !level.getFluidState(top).isEmpty() || !level.getFluidState(top.below()).isEmpty()) return false;
                if (!ground.is(net.minecraft.tags.BlockTags.DIRT) && !ground.is(net.minecraft.tags.BlockTags.SAND)
                        && !ground.is(Blocks.GRAVEL) && !ground.is(Blocks.PODZOL) && !ground.is(Blocks.COARSE_DIRT) && !ground.is(Blocks.MUD)) return false;
            }
        }
        // the mound: 3 long x 1 wide raised one block, coarse dirt around it
        BlockState coarse = Blocks.COARSE_DIRT.defaultBlockState();
        for (int i = 0; i <= 2; i++) {
            BlockPos mound = base.relative(along, i);
            setBlock(level, mound.below(), coarse);
            setBlock(level, mound, coarse);
            for (int s : new int[]{-1, 1}) {
                BlockPos edge = base.relative(along, i).relative(side, s);
                if (r.nextInt(3) != 0) setBlock(level, edge.below(), coarse);
            }
        }
        // the cross at the head, the skull on the mound
        BlockPos head = base.relative(along.getOpposite()).above(0);
        if (level.getBlockState(head).isAir() || level.getBlockState(head).canBeReplaced()) {
            setBlock(level, head, ModBlocks.GRAVE_CROSS.defaultBlockState().setValue(GraveCrossBlock.FACING, along)
                    .setValue(GraveCrossBlock.TILTED, r.nextInt(3) == 0));
        }
        BlockPos skullAt = base.relative(along, 2).above();
        if (r.nextInt(3) != 0 && level.getBlockState(skullAt).isAir()) {
            setBlock(level, skullAt, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, r.nextInt(16)));
        }
        // cobwebs, dead plants, a wither rose
        for (int i = 0; i < 4; i++) {
            BlockPos p = base.relative(along, r.nextInt(5) - 1).relative(side, r.nextInt(3) - 1).above(mound(level, base));
            if (!level.getBlockState(p).isAir()) continue;
            int roll = r.nextInt(6);
            if (roll < 2) setBlock(level, p, Blocks.COBWEB.defaultBlockState());
            else if (roll < 4) setBlock(level, p, Blocks.DEAD_BUSH.defaultBlockState());
            else if (roll == 4) setBlock(level, p, Blocks.WITHER_ROSE.defaultBlockState());
        }
        // buried: bone blocks as a hint, the barrel below them
        BlockPos middle = base.relative(along, 1);
        setBlock(level, middle.below(2), Blocks.BONE_BLOCK.defaultBlockState());
        BlockPos barrel = middle.below(3);
        setBlock(level, barrel, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
        net.minecraft.world.RandomizableContainer.setBlockEntityLootTable(level, r, barrel, Graves.LOOT);
        Graves.record(level.getChunk(middle), middle);
        return true;
    }

    private static int mound(WorldGenLevel level, BlockPos base) {
        return 1; // plants sit on the raised mound
    }
}
