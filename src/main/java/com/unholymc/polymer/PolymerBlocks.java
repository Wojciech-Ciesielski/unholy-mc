package com.unholymc.polymer;

import com.unholymc.UnholyMC;
import com.unholymc.block.GraveCrossBlock;
import com.unholymc.block.TreeNoteBlock;
import com.unholymc.registry.ModBlocks;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Our blocks for vanilla clients, as textured Polymer blocks: the client gets a spare vanilla block state whose
 * model the resource pack replaces with ours.
 * <ul>
 * <li>Tree note: an open trapdoor of the same facing has the same thin outline on the trunk's side.</li>
 * <li>Grave cross: a sapling state (no collision, small outline); the cross is symmetric front to back, so two
 *     axes x two tilts cover all eight block states.</li>
 * </ul>
 */
public final class PolymerBlocks {
    private static final Map<BlockState, BlockState> CLIENT_STATES = new HashMap<>();

    private PolymerBlocks() {}

    private record Textured() implements PolymerTexturedBlock {
        @Override
        public BlockState getPolymerBlockState(BlockState state, @Nullable PacketContext context) {
            return CLIENT_STATES.getOrDefault(state, Blocks.AIR.defaultBlockState());
        }
    }

    public static void init() {
        for (BlockState state : ModBlocks.TREE_NOTE.getStateDefinition().getPossibleStates()) {
            Direction facing = state.getValue(LadderBlock.FACING);
            String variant = state.getValue(TreeNoteBlock.VARIANT).getSerializedName();
            BlockModelType type = switch (facing) {
                case EAST -> BlockModelType.TRAPDOOR_EAST;
                case SOUTH -> BlockModelType.TRAPDOOR_SOUTH;
                case WEST -> BlockModelType.TRAPDOOR_WEST;
                default -> BlockModelType.TRAPDOOR_NORTH;
            };
            map(state, type, PolymerBlockModel.of(UnholyMC.id("block/tree_note_" + variant), 0, yRot(facing)));
        }

        Map<String, BlockState> crosses = new HashMap<>();
        for (BlockState state : ModBlocks.GRAVE_CROSS.getStateDefinition().getPossibleStates()) {
            Direction facing = state.getValue(GraveCrossBlock.FACING);
            boolean tilted = state.getValue(GraveCrossBlock.TILTED);
            int y = facing.getAxis() == Direction.Axis.X ? 90 : 0;
            String key = y + "/" + tilted;
            BlockState client = crosses.computeIfAbsent(key, k -> PolymerBlockResourceUtils.requestBlock(BlockModelType.PLANT,
                    PolymerBlockModel.of(UnholyMC.id(tilted ? "block/grave_cross_tilted" : "block/grave_cross"), 0, y)));
            if (client != null) CLIENT_STATES.put(state, client);
            else UnholyMC.LOGGER.warn("No spare client block state left for {}", state);
        }

        PolymerTexturedBlock overlay = new Textured();
        eu.pb4.polymer.core.api.block.PolymerBlock.registerOverlay(ModBlocks.TREE_NOTE, overlay);
        eu.pb4.polymer.core.api.block.PolymerBlock.registerOverlay(ModBlocks.GRAVE_CROSS, overlay);
    }

    private static void map(BlockState state, BlockModelType type, PolymerBlockModel model) {
        BlockState client = PolymerBlockResourceUtils.requestBlock(type, model);
        if (client != null) CLIENT_STATES.put(state, client);
        else UnholyMC.LOGGER.warn("No spare client block state left for {}", state);
    }

    private static int yRot(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }
}
