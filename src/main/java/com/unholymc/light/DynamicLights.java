package com.unholymc.light;

import com.mojang.serialization.Codec;
import com.unholymc.UnholyMC;
import com.unholymc.config.ConfigManager;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Real, moving light (DECISIONS #1): invisible minecraft:light blocks placed into air (or waterlogged into
 * water sources) around players and removed when no longer wanted. Every placed position is also stored
 * in a persistent level attachment, so leftovers from a crash are cleaned up on the next start.
 */
public final class DynamicLights {
    private static final List<LightSource> SOURCES = new ArrayList<>();
    private static final Map<ServerLevel, Map<BlockPos, Integer>> PLACED = new IdentityHashMap<>();

    /** Positions of light blocks owned by the mod, per level (persistent, for crash cleanup). */
    public static final AttachmentType<List<BlockPos>> OWNED = AttachmentRegistry.create(UnholyMC.id("dynamic_lights"),
            b -> b.persistent(BlockPos.CODEC.listOf()));

    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private DynamicLights() {}

    public static void register(LightSource source) {
        SOURCES.add(source);
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(DynamicLights::tick);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            for (ServerLevel level : server.getAllLevels()) cleanupLeftovers(level);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (ServerLevel level : server.getAllLevels()) apply(level, Map.of());
            PLACED.clear();
        });
    }

    private static void tick(MinecraftServer server) {
        int interval = Math.max(1, ConfigManager.get().flashlight.updateIntervalTicks);
        if (server.getTickCount() % interval != 0) return;

        Map<ServerLevel, Map<BlockPos, Integer>> desired = new IdentityHashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            Map<BlockPos, Integer> out = desired.computeIfAbsent(player.level(), l -> new HashMap<>());
            for (LightSource source : SOURCES) source.collect(player, out);
        }
        for (ServerLevel level : server.getAllLevels()) {
            Map<BlockPos, Integer> want = desired.getOrDefault(level, Map.of());
            if (want.isEmpty() && PLACED.getOrDefault(level, Map.of()).isEmpty()) continue;
            apply(level, want);
        }
    }

    private static void apply(ServerLevel level, Map<BlockPos, Integer> want) {
        Map<BlockPos, Integer> placed = PLACED.computeIfAbsent(level, l -> new HashMap<>());
        boolean changed = false;

        // remove lights that are no longer wanted
        var it = placed.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            if (!want.containsKey(e.getKey())) {
                removeLight(level, e.getKey());
                it.remove();
                changed = true;
            }
        }
        // place / update wanted lights
        for (var e : want.entrySet()) {
            BlockPos pos = e.getKey();
            int lightLevel = Math.clamp(e.getValue(), 1, LightBlock.MAX_LEVEL);
            Integer current = placed.get(pos);
            if (current != null && current == lightLevel) continue;
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            boolean ours = current != null && state.is(Blocks.LIGHT);
            boolean water = state.getFluidState().is(Fluids.WATER) && state.getFluidState().isSource();
            if (!ours && !(state.isAir() || (water && state.is(Blocks.WATER)))) continue;
            boolean waterlogged = ours ? state.getValue(LightBlock.WATERLOGGED) : water;
            level.setBlock(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lightLevel)
                    .setValue(LightBlock.WATERLOGGED, waterlogged), FLAGS);
            placed.put(pos, lightLevel);
            changed = true;
        }
        if (changed) {
            level.setAttached(OWNED, placed.isEmpty() ? null : List.copyOf(placed.keySet()));
        }
    }

    private static void removeLight(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.LIGHT)) {
            level.setBlock(pos, state.getValue(LightBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), FLAGS);
        }
    }

    private static void cleanupLeftovers(ServerLevel level) {
        List<BlockPos> owned = level.getAttached(OWNED);
        if (owned == null || owned.isEmpty()) return;
        for (BlockPos pos : owned) {
            BlockState state = level.getBlockState(pos); // loads the chunk if needed; only a handful of positions
            if (state.is(Blocks.LIGHT)) {
                level.setBlock(pos, state.getValue(LightBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), FLAGS);
            }
        }
        level.removeAttached(OWNED);
        UnholyMC.LOGGER.info("Removed {} leftover dynamic light blocks in {}", owned.size(), level.dimension().identifier());
    }
}
