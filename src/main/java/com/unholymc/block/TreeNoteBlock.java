package com.unholymc.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;
import java.util.Map;

/**
 * A paper note pinned to a tree trunk (GDD 2.2): attaches like a ladder (facing away from the trunk),
 * no collision, not climbable, drops 1 paper (DECISIONS #21).
 */
public class TreeNoteBlock extends LadderBlock {
    public enum Variant implements StringRepresentable {
        HELP, HE_IS_HERE, BLOOD, SYMBOL;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Variant> VARIANT = EnumProperty.create("variant", Variant.class);
    public static final MapCodec<TreeNoteBlock> CODEC = simpleCodec(TreeNoteBlock::new);
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.boxZ(14.0, 15.0, 16.0));

    public TreeNoteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(VARIANT, Variant.HELP));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<LadderBlock> codec() {
        return (MapCodec) CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VARIANT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }
}
