package com.unholymc.registry;

import com.unholymc.UnholyMC;
import com.unholymc.block.GraveCrossBlock;
import com.unholymc.block.TreeNoteBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
    public static final Block TREE_NOTE = register("tree_note", new TreeNoteBlock(props("tree_note")
            .noCollision().strength(0.2f).sound(SoundType.GRASS).noOcclusion().pushReaction(PushReaction.DESTROY).forceSolidOff()));

    public static final Block GRAVE_CROSS = register("grave_cross", new GraveCrossBlock(props("grave_cross")
            .strength(1.0f).sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.DESTROY).ignitedByLava()));

    private ModBlocks() {}

    private static BlockBehaviour.Properties props(String name) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, UnholyMC.id(name)));
    }

    private static Block register(String name, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK, UnholyMC.id(name), block);
    }

    public static void init() {}
}
