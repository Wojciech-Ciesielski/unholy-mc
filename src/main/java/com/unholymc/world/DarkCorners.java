package com.unholymc.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.unholymc.UnholyMC;
import com.unholymc.config.ConfigManager;
import com.unholymc.entity.SlopSpawning;
import com.unholymc.entity.slop.AbstractSlop;
import com.unholymc.registry.ModEntities;
import com.unholymc.registry.ModSounds;
import com.unholymc.survival.SanityAuras;
import com.unholymc.survival.SleepTracker;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dark corners (GDD 2.1): slop spawning zones in forests, >= 500 blocks from the world spawn and >= 750
 * blocks apart (one seed-chosen candidate per 1000x1000 cell, jittered by +-125). At night: a cloud of
 * black particles, a -50 / 2 min aura at the centre (5 less per block), extra slops. By day they are inactive
 * and leave a mound of stone and coal ore that is rebuilt every day, overwriting whatever players placed
 * there (DECISIONS #19).
 */
public final class DarkCorners {
    public record CornerState(BlockPos pos, long lastResetDay, long forcedUntil, boolean forcedActive, int baseY) {
        public static final Codec<CornerState> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(CornerState::pos),
                Codec.LONG.optionalFieldOf("last_reset_day", Long.MIN_VALUE).forGetter(CornerState::lastResetDay),
                Codec.LONG.optionalFieldOf("forced_until", 0L).forGetter(CornerState::forcedUntil),
                Codec.BOOL.optionalFieldOf("forced_active", false).forGetter(CornerState::forcedActive),
                Codec.INT.optionalFieldOf("base_y", Integer.MIN_VALUE).forGetter(CornerState::baseY)
        ).apply(i, CornerState::new));
    }

    /** Manually placed corners (/thm dark_corner spawn). */
    public static final AttachmentType<List<BlockPos>> MANUAL = AttachmentRegistry.create(UnholyMC.id("dark_corners_manual"),
            b -> b.persistent(BlockPos.CODEC.listOf()));
    /** Per-corner state (daily mound reset, forced day/night). */
    public static final AttachmentType<List<CornerState>> STATES = AttachmentRegistry.create(UnholyMC.id("dark_corners_state"),
            b -> b.persistent(CornerState.CODEC.listOf()));

    private static final Map<Long, Integer> SPAWN_TIMERS = new HashMap<>();

    private DarkCorners() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(DarkCorners::tick);
        SanityAuras.register((player, out) -> {
            if (player.level() != player.level().getServer().overworld()) return;
            var cfg = ConfigManager.get().darkCorners;
            float strongest = 0f;
            for (BlockPos c : cornersNear(player.level(), player.blockPosition(), 16)) {
                if (!isActive(player.level(), c)) continue;
                double d = Math.sqrt(c.distToCenterSqr(player.getX(), c.getY() + 0.5, player.getZ()));
                float value = Math.min(0f, cfg.centerAuraPer2Min + cfg.auraFalloffPerBlock * (float) d);
                strongest = Math.min(strongest, value);
            }
            if (strongest < 0f) out.add("dark_corner", strongest);
        });
    }

    // --- where the corners are ---

    private static @Nullable BlockPos candidate(ServerLevel level, int cx, int cz) {
        var cfg = ConfigManager.get().darkCorners;
        RandomSource r = RandomSource.create(level.getSeed() ^ (SlenderForests.cellKey(cx, cz) * 0xC2B2AE3D27D4EB4FL) ^ 0x5DEECE66DL);
        int g = cfg.gridSize;
        int x = cx * g + g / 2 + r.nextInt(cfg.jitter * 2 + 1) - cfg.jitter;
        int z = cz * g + g / 2 + r.nextInt(cfg.jitter * 2 + 1) - cfg.jitter;
        BlockPos spawn = level.getRespawnData().pos();
        if ((long) (x - spawn.getX()) * (x - spawn.getX()) + (long) (z - spawn.getZ()) * (z - spawn.getZ()) < (long) cfg.minSpawnDistance * cfg.minSpawnDistance) {
            return null;
        }
        if (!BiomeSampler.is(level, x, z, SlopSpawning.FOREST)) return null;
        return new BlockPos(x, level.getSeaLevel(), z);
    }

    /** Corners (natural and manual) whose centre is within {@code radius} blocks horizontally. */
    public static List<BlockPos> cornersNear(ServerLevel level, BlockPos pos, double radius) {
        List<BlockPos> out = new ArrayList<>();
        int g = ConfigManager.get().darkCorners.gridSize;
        int cx = Math.floorDiv(pos.getX(), g), cz = Math.floorDiv(pos.getZ(), g);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos c = candidate(level, cx + dx, cz + dz);
                if (c != null && horizontalDistSqr(c, pos) <= radius * radius) out.add(c);
            }
        }
        for (BlockPos c : level.getAttachedOrElse(MANUAL, List.of())) {
            if (horizontalDistSqr(c, pos) <= radius * radius) out.add(c);
        }
        return out;
    }

    public static @Nullable BlockPos locate(ServerLevel level, BlockPos from, int maxCells) {
        int g = ConfigManager.get().darkCorners.gridSize;
        int cx = Math.floorDiv(from.getX(), g), cz = Math.floorDiv(from.getZ(), g);
        BlockPos best = null;
        for (BlockPos c : level.getAttachedOrElse(MANUAL, List.of())) {
            if (best == null || horizontalDistSqr(c, from) < horizontalDistSqr(best, from)) best = c;
        }
        for (int r = 0; r <= maxCells; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    BlockPos c = candidate(level, cx + dx, cz + dz);
                    if (c != null && (best == null || horizontalDistSqr(c, from) < horizontalDistSqr(best, from))) best = c;
                }
            }
            if (best != null && r >= 1) return best;
        }
        return best;
    }

    private static double horizontalDistSqr(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    // --- state ---

    private static CornerState state(ServerLevel level, BlockPos c) {
        for (CornerState s : level.getAttachedOrElse(STATES, List.of())) {
            if (s.pos().getX() == c.getX() && s.pos().getZ() == c.getZ()) return s;
        }
        return new CornerState(c, Long.MIN_VALUE, 0L, false, Integer.MIN_VALUE);
    }

    private static void putState(ServerLevel level, CornerState state) {
        List<CornerState> list = new ArrayList<>(level.getAttachedOrElse(STATES, List.of()));
        list.removeIf(s -> s.pos().getX() == state.pos().getX() && s.pos().getZ() == state.pos().getZ());
        list.add(state);
        level.setAttached(STATES, List.copyOf(list));
    }

    public static boolean isActive(ServerLevel level, BlockPos corner) {
        CornerState s = state(level, corner);
        if (level.getGameTime() < s.forcedUntil()) return s.forcedActive();
        return level.isDarkOutside();
    }

    /** /thm dark_corner toggle_day_cycle: flip day/night of a corner for 5 minutes. */
    public static boolean toggle(ServerLevel level, BlockPos corner) {
        boolean nowActive = !isActive(level, corner);
        CornerState s = state(level, corner);
        CornerState next = new CornerState(corner, nowActive ? s.lastResetDay() : Long.MIN_VALUE, level.getGameTime() + 6000, nowActive, s.baseY());
        putState(level, next);
        if (!nowActive) resetMound(level, corner);
        return nowActive;
    }

    public static void addManual(ServerLevel level, BlockPos pos) {
        List<BlockPos> list = new ArrayList<>(level.getAttachedOrElse(MANUAL, List.of()));
        list.add(pos.immutable());
        level.setAttached(MANUAL, List.copyOf(list));
    }

    // --- ticking ---

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        ServerLevel level = server.overworld();
        var cfg = ConfigManager.get().darkCorners;
        java.util.Set<BlockPos> handled = new java.util.HashSet<>();
        for (ServerPlayer p : level.players()) {
            for (BlockPos c : cornersNear(level, p.blockPosition(), cfg.activeRange)) {
                if (!handled.add(c)) continue;
                if (isActive(level, c)) activeTick(level, c);
                else {
                    long day = level.getOverworldClockTime() / SleepTracker.DAY_TICKS;
                    if (state(level, c).lastResetDay() < day) resetMound(level, c);
                }
            }
        }
    }

    private static int surfaceY(ServerLevel level, BlockPos c) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ());
    }

    private static void activeTick(ServerLevel level, BlockPos c) {
        var cfg = ConfigManager.get().darkCorners;
        RandomSource r = level.getRandom();
        int y = surfaceY(level, c);
        // black particle cloud
        for (int i = 0; i < 24; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = r.nextDouble() * cfg.cloudRadius;
            level.sendParticles(i % 3 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SQUID_INK,
                    c.getX() + 0.5 + Math.cos(a) * d, y + 0.5 + r.nextDouble() * 4, c.getZ() + 0.5 + Math.sin(a) * d, 1, 0.1, 0.1, 0.1, 0.0);
        }
        if (r.nextInt(10) == 0) {
            level.playSound(null, c.getX() + 0.5, y + 1, c.getZ() + 0.5, ModSounds.AMBIENT_DARK_CORNER, SoundSource.AMBIENT, 1.5f, 1f);
        }
        // slop spawning zone
        long key = c.asLong();
        int t = SPAWN_TIMERS.merge(key, 20, Integer::sum);
        if (t < cfg.slopSpawnIntervalTicks) return;
        SPAWN_TIMERS.put(key, 0);
        int nearby = level.getEntitiesOfClass(AbstractSlop.class, new net.minecraft.world.phys.AABB(c).inflate(32, 16, 32)).size();
        if (nearby >= cfg.maxSlopsNearby) return;
        float roll = r.nextFloat();
        EntityType<? extends Mob> type = roll < 0.6f ? ModEntities.AGGRESSIVE_SLOP : roll < 0.9f ? ModEntities.NEUTRAL_SLOP : ModEntities.UNHOLY_SLOP;
        if (type == ModEntities.UNHOLY_SLOP && !BiomeSampler.is(level, c.getX(), c.getZ(), SlopSpawning.UNHOLY_BIOMES)) type = ModEntities.AGGRESSIVE_SLOP;
        int x = c.getX() + Mth.floor((r.nextDouble() * 2 - 1) * cfg.slopSpawnRadius);
        int z = c.getZ() + Mth.floor((r.nextDouble() * 2 - 1) * cfg.slopSpawnRadius);
        BlockPos spot = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        if (!level.getBlockState(spot).getCollisionShape(level, spot).isEmpty()) return;
        Mob mob = type.create(level, EntitySpawnReason.EVENT);
        if (mob == null) return;
        mob.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, r.nextFloat() * 360f, 0f);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), EntitySpawnReason.EVENT, null);
        level.addFreshEntity(mob);
    }

    /** Rebuild the stone-and-coal mound left by the corner in daylight (overwrites player blocks). */
    public static void resetMound(ServerLevel level, BlockPos c) {
        var cfg = ConfigManager.get().darkCorners;
        CornerState s = state(level, c);
        int baseY = s.baseY() != Integer.MIN_VALUE ? s.baseY() : surfaceY(level, c);
        RandomSource layout = RandomSource.create(c.asLong() * 31L + 7L); // same shape every day
        RandomSource ores = level.getRandom();                             // fresh ores every day
        int r = cfg.moundRadius;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                int h = d <= 1.0 ? 3 : d <= 2.0 ? 2 : d <= r + 0.2 ? 1 : 0;
                if (layout.nextInt(5) == 0) h = Math.max(0, h - 1);
                for (int y = 0; y < h; y++) {
                    BlockState block = ores.nextFloat() < cfg.coalChance ? Blocks.COAL_ORE.defaultBlockState()
                            : switch (layout.nextInt(4)) {
                                case 0 -> Blocks.COBBLESTONE.defaultBlockState();
                                case 1 -> Blocks.ANDESITE.defaultBlockState();
                                case 2 -> Blocks.TUFF.defaultBlockState();
                                default -> Blocks.STONE.defaultBlockState();
                            };
                    level.setBlock(new BlockPos(c.getX() + dx, baseY + y, c.getZ() + dz), block, Block.UPDATE_ALL);
                }
            }
        }
        long day = level.getOverworldClockTime() / SleepTracker.DAY_TICKS;
        putState(level, new CornerState(c, day, s.forcedUntil(), s.forcedActive(), baseY));
    }
}
