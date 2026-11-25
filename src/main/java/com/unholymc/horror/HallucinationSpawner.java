package com.unholymc.horror;

import com.unholymc.attachment.ModAttachments;
import com.unholymc.config.ConfigManager;
import com.unholymc.entity.Hallucination;
import com.unholymc.entity.slop.SlopKind;
import com.unholymc.entity.slop.SlopVariant;
import com.unholymc.registry.ModEntities;
import com.unholymc.registry.ModSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * DECISIONS #4: every 15 s a hallucination appears within 20 blocks of each player whose sanity is low
 * (<= threshold), up to a per-player cap; 4x as often in the End. Crucifix holders are immune.
 */
public final class HallucinationSpawner {
    private static final Map<UUID, Integer> TIMERS = new HashMap<>();

    private HallucinationSpawner() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(HallucinationSpawner::tick);
    }

    private static void tick(MinecraftServer server) {
        var cfg = ConfigManager.get();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isAlive() || Jumpscares.immune(p) || ModAttachments.hud(p).sanity() > cfg.sanity.lowSanityThreshold) {
                TIMERS.remove(p.getUUID());
                continue;
            }
            float freq = p.level().dimension() == Level.END ? cfg.hallucinations.endFrequencyMultiplier : 1f;
            int interval = Math.max(20, Math.round(cfg.hallucinations.spawnIntervalTicks / Math.max(0.01f, freq)));
            int t = TIMERS.merge(p.getUUID(), 1, Integer::sum);
            if (t >= interval) {
                TIMERS.put(p.getUUID(), 0);
                if (countOwned(p) < cfg.hallucinations.maxPerPlayer) spawnFor(p);
            }
        }
    }

    public static int countOwned(ServerPlayer p) {
        double r = ConfigManager.get().hallucinations.vanishDistance;
        return p.level().getEntitiesOfClass(Hallucination.class, p.getBoundingBox().inflate(r), h -> p.getUUID().equals(h.owner())).size();
    }

    public static @Nullable Hallucination spawnFor(ServerPlayer p) {
        ServerLevel level = p.level();
        var cfg = ConfigManager.get().hallucinations;
        BlockPos pos = findSpot(level, p, cfg.minSpawnDistance, cfg.spawnRadius);
        if (pos == null) return null;
        Hallucination h = ModEntities.HALLUCINATION.create(level, EntitySpawnReason.EVENT);
        if (h == null) return null;
        h.setup(p, SlopVariant.random(SlopKind.AGGRESSIVE, p.getRandom()));
        h.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, p.getRandom().nextFloat() * 360f, 0f);
        level.addFreshEntity(h);
        Jumpscares.playBehind(p, ModSounds.ENTITY_HALLUCINATION_SPAWN, 0.8f);
        return h;
    }

    public static void clearFor(ServerPlayer p) {
        double r = ConfigManager.get().hallucinations.vanishDistance * 2;
        for (Hallucination h : p.level().getEntitiesOfClass(Hallucination.class, p.getBoundingBox().inflate(r), h -> p.getUUID().equals(h.owner()))) {
            h.vanish();
        }
    }

    /** A random standing spot (solid floor, two free blocks) between min and max distance from the player. */
    public static @Nullable BlockPos findSpot(ServerLevel level, ServerPlayer p, double min, double max) {
        RandomSource r = p.getRandom();
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = r.nextDouble() * Math.PI * 2;
            double dist = min + r.nextDouble() * (max - min);
            int x = Mth.floor(p.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(p.getZ() + Math.sin(angle) * dist);
            for (int dy = 6; dy >= -8; dy--) {
                BlockPos pos = new BlockPos(x, Mth.floor(p.getY()) + dy, z);
                if (!level.isLoaded(pos)) break;
                if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                        && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                        && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                        && level.getFluidState(pos).isEmpty()) {
                    return pos;
                }
            }
        }
        return null;
    }
}
