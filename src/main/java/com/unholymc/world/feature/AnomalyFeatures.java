package com.unholymc.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.List;

/**
 * "Something is wrong with this world" decorations (GDD 5.4, DECISIONS #32): floating leaves, leafless trees,
 * wooden crosses, skulls, redstone "blood", and abominations (wrong leaves on a trunk / a tree that does not
 * belong to the biome). Frequencies are in the placed_feature JSON files (rarity_filter).
 */
public final class AnomalyFeatures {
    private static final List<Block> LOGS = List.of(Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.SPRUCE_LOG, Blocks.DARK_OAK_LOG, Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG);
    private static final List<Block> LEAVES = List.of(Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.DARK_OAK_LEAVES,
            Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.CHERRY_LEAVES, Blocks.AZALEA_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.PALE_OAK_LEAVES);
    private static final List<ResourceKey<ConfiguredFeature<?, ?>>> FOREIGN_TREES = List.of(TreeFeatures.CHERRY, TreeFeatures.JUNGLE_TREE,
            TreeFeatures.ACACIA, TreeFeatures.SPRUCE, TreeFeatures.BIRCH, TreeFeatures.AZALEA_TREE, TreeFeatures.PALE_OAK, TreeFeatures.MANGROVE);

    private AnomalyFeatures() {}

    static boolean groundOk(WorldGenLevel level, BlockPos surface) {
        BlockState below = level.getBlockState(surface.below());
        BlockState at = level.getBlockState(surface);
        return below.isSolidRender() && below.getFluidState().isEmpty() && (at.isAir() || at.canBeReplaced()) && at.getFluidState().isEmpty();
    }

    static <T> T pick(List<T> list, RandomSource r) {
        return list.get(r.nextInt(list.size()));
    }

    static BlockState leaves(Block block) {
        BlockState s = block.defaultBlockState();
        return s.hasProperty(LeavesBlock.PERSISTENT) ? s.setValue(LeavesBlock.PERSISTENT, true) : s;
    }

    static void blob(WorldGenLevel level, BlockPos center, int radius, BlockState state, RandomSource r) {
        for (int dx = -radius; dx <= radius; dx++)
            for (int dy = -radius; dy <= radius; dy++)
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius + 1 || r.nextInt(6) == 0) continue;
                    BlockPos p = center.offset(dx, dy, dz);
                    if (level.getBlockState(p).isAir()) level.setBlock(p, state, Block.UPDATE_CLIENTS);
                }
    }

    /** Clumps of leaves hanging in the air with no tree. */
    public static class FloatingLeaves extends Feature<NoneFeatureConfiguration> {
        public FloatingLeaves(Codec<NoneFeatureConfiguration> codec) { super(codec); }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            if (!groundOk(level, ctx.origin())) return false;
            BlockPos center = ctx.origin().above(4 + r.nextInt(4));
            blob(level, center, 1 + r.nextInt(2), leaves(pick(LEAVES, r)), r);
            return true;
        }
    }

    /** A dead, leafless tree with a few crooked branches. */
    public static class DeadTree extends Feature<NoneFeatureConfiguration> {
        public DeadTree(Codec<NoneFeatureConfiguration> codec) { super(codec); }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            BlockPos base = ctx.origin();
            if (!groundOk(level, base)) return false;
            Block log = pick(LOGS, r);
            int height = 4 + r.nextInt(5);
            for (int y = 0; y < height; y++) {
                BlockState at = level.getBlockState(base.above(y));
                if (!at.isAir() && !at.canBeReplaced()) return y > 2;
                setBlock(level, base.above(y), log.defaultBlockState());
            }
            int branches = 1 + r.nextInt(3);
            for (int i = 0; i < branches; i++) {
                Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(r);
                BlockPos start = base.above(height - 1 - r.nextInt(3));
                int len = 1 + r.nextInt(2);
                for (int j = 1; j <= len; j++) {
                    BlockPos p = start.relative(dir, j).above(j == len ? 1 : 0);
                    if (level.getBlockState(p).isAir()) {
                        setBlock(level, p, log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, j == len ? Direction.Axis.Y : dir.getAxis()));
                    }
                }
            }
            return true;
        }
    }

    /** A wooden grave cross. */
    public static class Cross extends Feature<NoneFeatureConfiguration> {
        public Cross(Codec<NoneFeatureConfiguration> codec) { super(codec); }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            BlockPos base = ctx.origin();
            if (!groundOk(level, base)) return false;
            Block fence = r.nextBoolean() ? Blocks.DARK_OAK_FENCE : Blocks.SPRUCE_FENCE;
            boolean alongX = r.nextBoolean();
            for (int y = 0; y < 4; y++) {
                BlockState at = level.getBlockState(base.above(y));
                if (!at.isAir() && !at.canBeReplaced()) return false;
            }
            BlockState post = fence.defaultBlockState();
            for (int y = 0; y < 4; y++) {
                BlockState s = post;
                if (y == 2) s = alongX ? s.setValue(FenceBlock.EAST, true).setValue(FenceBlock.WEST, true)
                        : s.setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
                setBlock(level, base.above(y), s);
            }
            BlockPos armA = alongX ? base.above(2).east() : base.above(2).north();
            BlockPos armB = alongX ? base.above(2).west() : base.above(2).south();
            if (level.getBlockState(armA).isAir())
                setBlock(level, armA, alongX ? post.setValue(FenceBlock.WEST, true) : post.setValue(FenceBlock.SOUTH, true));
            if (level.getBlockState(armB).isAir())
                setBlock(level, armB, alongX ? post.setValue(FenceBlock.EAST, true) : post.setValue(FenceBlock.NORTH, true));
            if (r.nextInt(3) == 0) {
                BlockPos front = alongX ? base.south() : base.east();
                if (groundOk(level, front)) setBlock(level, front, Blocks.WITHER_ROSE.defaultBlockState());
            }
            return true;
        }
    }

    /** A skeleton skull lying on the ground, sometimes with a few bones. */
    public static class Skull extends Feature<NoneFeatureConfiguration> {
        public Skull(Codec<NoneFeatureConfiguration> codec) { super(codec); }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            BlockPos base = ctx.origin();
            if (!groundOk(level, base)) return false;
            setBlock(level, base, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, r.nextInt(16)));
            int bones = r.nextInt(3);
            for (int i = 0; i < bones; i++) {
                BlockPos p = base.offset(r.nextInt(5) - 2, 0, r.nextInt(5) - 2);
                if (!p.equals(base) && groundOk(level, p)) {
                    setBlock(level, p, Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, r.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z));
                }
            }
            return true;
        }
    }

    /** Splashes of redstone dust on the ground, like dried blood. */
    public static class Blood extends Feature<NoneFeatureConfiguration> {
        public Blood(Codec<NoneFeatureConfiguration> codec) { super(codec); }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            int placed = 0;
            int count = 3 + r.nextInt(6);
            for (int i = 0; i < count; i++) {
                BlockPos p = ctx.origin().offset(r.nextInt(5) - 2, 0, r.nextInt(5) - 2);
                for (int dy = 1; dy >= -1; dy--) {
                    BlockPos q = p.above(dy);
                    if (groundOk(level, q) && level.getBlockState(q.below()).isFaceSturdy(level, q.below(), Direction.UP)) {
                        setBlock(level, q, Blocks.REDSTONE_WIRE.defaultBlockState());
                        placed++;
                        break;
                    }
                }
            }
            return placed > 0;
        }
    }

    /** A tree that does not belong here: a foreign species, or a trunk with the wrong leaves. */
    public static class Abomination extends Feature<NoneFeatureConfiguration> {
        public Abomination(Codec<NoneFeatureConfiguration> codec) { super(codec); }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            BlockPos base = ctx.origin();
            if (!groundOk(level, base)) return false;
            if (r.nextBoolean() && level.getBlockState(base.below()).is(BlockTags.DIRT)) {
                var tree = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(pick(FOREIGN_TREES, r));
                if (tree.isPresent() && tree.get().value().place(level, ctx.chunkGenerator(), r, base)) return true;
            }
            // trunk of one species with a crown of another
            Block log = pick(LOGS, r);
            Block leaf;
            do { leaf = pick(LEAVES, r); } while (sameWood(log, leaf));
            int height = 4 + r.nextInt(3);
            for (int y = 0; y < height; y++) {
                BlockState at = level.getBlockState(base.above(y));
                if (!at.isAir() && !at.canBeReplaced()) return false;
            }
            for (int y = 0; y < height; y++) setBlock(level, base.above(y), log.defaultBlockState());
            blob(level, base.above(height), 2, leaves(leaf), r);
            return true;
        }

        private static boolean sameWood(Block log, Block leaf) {
            String l = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(log).getPath().replace("_log", "");
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(leaf).getPath().startsWith(l);
        }
    }
}
