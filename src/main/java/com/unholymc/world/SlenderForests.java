package com.unholymc.world;

import com.mojang.serialization.Codec;
import com.unholymc.UnholyMC;
import com.unholymc.config.ConfigManager;
import com.unholymc.entity.Slenderman;
import com.unholymc.horror.HallucinationSpawner;
import com.unholymc.registry.ModEntities;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slenderman forests (GDD 2.3, DECISIONS #20): every connected Dark Forest area (flood fill over a coarse
 * grid of the biome source) is identified by its smallest grid cell (the anchor). 10% of the areas, decided
 * by the world seed, are Slenderman forests: exactly one Slenderman appears there at night while a player is
 * inside, and never again once he has been defeated.
 */
public final class SlenderForests {
    public static final TagKey<Biome> SLENDER_BIOMES = TagKey.create(Registries.BIOME, UnholyMC.id("slender_forest"));

    /** Anchors of forests whose Slenderman was defeated (overworld, persistent). */
    public static final AttachmentType<List<Long>> DEFEATED = AttachmentRegistry.create(UnholyMC.id("slender_defeated"),
            b -> b.persistent(Codec.LONG.listOf()));

    private static final Map<Long, Long> CELL_TO_ANCHOR = new ConcurrentHashMap<>();
    private static final Map<Long, UUID> ACTIVE = new ConcurrentHashMap<>();

    private SlenderForests() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(SlenderForests::tick);
        // the caches belong to one world (seed): drop them when singleplayer switches worlds
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            CELL_TO_ANCHOR.clear();
            ACTIVE.clear();
        });
    }

    public static long cellKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xffffffffL);
    }

    private static int step() {
        return Math.max(8, ConfigManager.get().slenderman.forestFloodFillStep);
    }

    private static boolean cellIsForest(ServerLevel level, int cx, int cz) {
        int s = step();
        return BiomeSampler.is(level, cx * s + s / 2, cz * s + s / 2, SLENDER_BIOMES);
    }

    /** Anchor of the Dark Forest area containing the block, or null if it is not in one. */
    public static @Nullable Long anchorAt(ServerLevel level, int x, int z) {
        int s = step();
        int cx = Math.floorDiv(x, s), cz = Math.floorDiv(z, s);
        if (!cellIsForest(level, cx, cz)) return null;
        Long cached = CELL_TO_ANCHOR.get(cellKey(cx, cz));
        if (cached != null) return cached;
        // flood fill (4-neighbourhood) over the coarse grid
        Set<Long> seen = new HashSet<>();
        ArrayDeque<long[]> queue = new ArrayDeque<>();
        queue.add(new long[]{cx, cz});
        seen.add(cellKey(cx, cz));
        long min = cellKey(cx, cz);
        int minX = cx, minZ = cz;
        int max = ConfigManager.get().slenderman.forestFloodFillMaxCells;
        while (!queue.isEmpty() && seen.size() < max) {
            long[] c = queue.poll();
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : dirs) {
                int nx = (int) c[0] + d[0], nz = (int) c[1] + d[1];
                long k = cellKey(nx, nz);
                if (seen.contains(k) || !cellIsForest(level, nx, nz)) continue;
                seen.add(k);
                queue.add(new long[]{nx, nz});
                if (nx < minX || (nx == minX && nz < minZ)) {
                    minX = nx;
                    minZ = nz;
                }
            }
        }
        long anchor = cellKey(minX, minZ);
        for (long k : seen) CELL_TO_ANCHOR.put(k, anchor);
        return anchor;
    }

    public static boolean isSlenderForest(ServerLevel level, long anchor) {
        RandomSource r = RandomSource.create(level.getSeed() ^ (anchor * 0x9E3779B97F4A7C15L));
        return r.nextFloat() < ConfigManager.get().slenderman.forestChance;
    }

    public static boolean isDefeated(ServerLevel level, long anchor) {
        List<Long> d = level.getServer().overworld().getAttached(DEFEATED);
        return d != null && d.contains(anchor);
    }

    public static void markDefeated(ServerLevel level, long anchor) {
        ServerLevel ow = level.getServer().overworld();
        List<Long> d = new java.util.ArrayList<>(ow.getAttachedOrElse(DEFEATED, List.of()));
        if (!d.contains(anchor)) d.add(anchor);
        ow.setAttached(DEFEATED, List.copyOf(d));
        ACTIVE.remove(anchor);
    }

    /** Block position of the anchor cell's centre (for /thm dark_corner locate). */
    public static BlockPos anchorPos(long anchor) {
        int s = step();
        return new BlockPos((int) (anchor >> 32) * s + s / 2, 0, (int) anchor * s + s / 2);
    }

    /** Is there a Slenderman forest within {@code radius} blocks? (decoy notes keep their distance) */
    public static boolean slenderForestWithin(ServerLevel level, int x, int z, int radius) {
        int s = step();
        for (int dx = -radius; dx <= radius; dx += s) {
            for (int dz = -radius; dz <= radius; dz += s) {
                if (dx * dx + dz * dz > radius * radius) continue;
                Long a = anchorAt(level, x + dx, z + dz);
                if (a != null && isSlenderForest(level, a)) return true;
            }
        }
        return false;
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 100 != 0) return;
        ServerLevel level = server.overworld();
        if (!Night.isNight(level)) return;
        var cfg = ConfigManager.get().slenderman;
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator()) continue;
            Long anchor = anchorAt(level, p.getBlockX(), p.getBlockZ());
            if (anchor == null || !isSlenderForest(level, anchor) || isDefeated(level, anchor)) continue;
            UUID id = ACTIVE.get(anchor);
            Entity existing = id != null ? level.getEntity(id) : null;
            if (existing != null && existing.isAlive()) continue;
            BlockPos spot = HallucinationSpawner.findSpot(level, p, cfg.spawnMinDistance, cfg.spawnMaxDistance);
            if (spot == null || !BiomeSampler.is(level, spot.getX(), spot.getZ(), SLENDER_BIOMES)) continue;
            Slenderman s = spawn(level, spot);
            if (s != null) {
                s.setForestAnchor(anchor);
                ACTIVE.put(anchor, s.getUUID());
            }
        }
    }

    public static @Nullable Slenderman spawn(ServerLevel level, BlockPos pos) {
        Slenderman s = ModEntities.SLENDERMAN.create(level, EntitySpawnReason.EVENT);
        if (s == null) return null;
        s.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        s.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
        s.setDeadly(BloodMoon.isActive(level));
        level.addFreshEntity(s);
        return s;
    }

    /** Nearest Slenderman forest found by scanning outwards (admin command; may take a moment). */
    public static @Nullable BlockPos locate(ServerLevel level, BlockPos from, int maxRadius) {
        int s = 64;
        for (int r = 0; r <= maxRadius; r += s) {
            for (int dx = -r; dx <= r; dx += s) {
                for (int dz = -r; dz <= r; dz += s) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int x = from.getX() + dx, z = from.getZ() + dz;
                    Long a = anchorAt(level, x, z);
                    if (a != null && isSlenderForest(level, a) && !isDefeated(level, a)) return new BlockPos(x, from.getY(), z);
                }
            }
        }
        return null;
    }
}
