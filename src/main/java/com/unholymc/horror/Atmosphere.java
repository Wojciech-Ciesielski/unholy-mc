package com.unholymc.horror;

import com.unholymc.attachment.ModAttachments;
import com.unholymc.config.ConfigManager;
import com.unholymc.entity.SlopSpawning;
import com.unholymc.entity.slop.AbstractSlop;
import com.unholymc.registry.ModSounds;
import com.unholymc.world.Night;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The sound atmosphere of the world (docs/ASSETS.md 6.1): forest whispers, knocks and steps at night, birds by day
 * (and their sudden silence when a slop is near), wind, creaking trees, and at low Sanity (14 and below) fake steps,
 * screams from behind a wall, breathing and a fake creeper. Everything is played to one player only, from a spot
 * around them, with a per-player timer for every kind of sound; intervals are in config ({@code ambience.*}).
 */
public final class Atmosphere {
    private enum Kind {
        WHISPER, DISTANT_STEPS, KNOCK, BRANCH_SNAP, BREATH, TREE_CREAK, BIRDS, WIND, FAKE_STEPS, WALL_SCREAM, FAKE_CREEPER
    }

    private static final Map<UUID, long[]> NEXT = new HashMap<>();
    private static final Map<UUID, Long> BIRDS_QUIET_UNTIL = new HashMap<>();
    private record FakeSteps(long start, float yaw) {}
    private static final Map<UUID, FakeSteps> FAKE_STEPS = new HashMap<>();

    private Atmosphere() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(Atmosphere::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> {
            UUID id = h.player.getUUID();
            NEXT.remove(id);
            BIRDS_QUIET_UNTIL.remove(id);
            FAKE_STEPS.remove(id);
        });
    }

    private static void tick(MinecraftServer server) {
        if (!ConfigManager.get().ambience.atmosphere) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator() || !p.isAlive()) continue;
            FakeSteps fs = FAKE_STEPS.get(p.getUUID());
            if (fs != null) {
                long age = p.level().getGameTime() - fs.start;
                if (age > 120) FAKE_STEPS.remove(p.getUUID());
                else if (Math.abs(Mth.wrapDegrees(p.getYRot() - fs.yaw)) > 90f) { // the player turned round: the steps stop
                    p.connection.send(new ClientboundStopSoundPacket(ModSounds.AMBIENT_SANITY_FAKE_STEPS.key().identifier(), SoundSource.AMBIENT));
                    FAKE_STEPS.remove(p.getUUID());
                }
            }
            if ((p.tickCount + p.getId() * 3) % 20 != 0) continue;
            if (p.level().dimension() != Level.OVERWORLD) continue;
            scan(p);
        }
    }

    private static void scan(ServerPlayer p) {
        ServerLevel level = p.level();
        long now = level.getGameTime();
        long[] next = NEXT.computeIfAbsent(p.getUUID(), k -> new long[Kind.values().length]);
        RandomSource r = p.getRandom();
        boolean night = Night.isNight(level);
        boolean surface = level.canSeeSky(p.blockPosition()) || level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getBlockX(), p.getBlockZ()) <= p.getBlockY() + 3;
        boolean forest = surface && level.getBiome(p.blockPosition()).is(SlopSpawning.FOREST);
        boolean raining = level.isRaining();
        float k = ConfigManager.get().ambience.atmosphereRate;

        if (night && forest) {
            due(next, Kind.WHISPER, now, r, 40, 120, k, () -> at(p, ModSounds.AMBIENT_FOREST_WHISPER, around(p, 3, 6), 0.6f));
            due(next, Kind.DISTANT_STEPS, now, r, 60, 150, k, () -> at(p, ModSounds.AMBIENT_FOREST_DISTANT_STEPS, around(p, 12, 24), 0.8f));
            due(next, Kind.KNOCK, now, r, 120, 300, k, () -> at(p, ModSounds.AMBIENT_FOREST_KNOCK, around(p, 16, 32), 1.0f));
            int snapMin = nearSlop(p, 24) ? 15 : 30;
            due(next, Kind.BRANCH_SNAP, now, r, snapMin, snapMin * 3, k, () -> at(p, ModSounds.AMBIENT_FOREST_BRANCH_SNAP, behind(p, 8, 16, 60), 0.9f));
        }
        if (forest) {
            int mult = raining ? 2 : 1;
            due(next, Kind.TREE_CREAK, now, r, 20 / mult, 60 / mult, k, () -> {
                BlockPos log = nearestLog(p, 4, 12);
                if (log != null) at(p, ModSounds.AMBIENT_FOREST_TREE_CREAK, Vec3.atCenterOf(log), 0.8f);
            });
        }
        if (!night && forest && !raining) {
            if (nearSlop(p, 32) && BIRDS_QUIET_UNTIL.getOrDefault(p.getUUID(), 0L) < now) {
                // the birds fall silent when something is about
                p.connection.send(new ClientboundStopSoundPacket(ModSounds.AMBIENT_FOREST_BIRDS.key().identifier(), SoundSource.AMBIENT));
                at(p, ModSounds.AMBIENT_FOREST_BIRDS_SILENCE, p.position(), 0.7f);
                BIRDS_QUIET_UNTIL.put(p.getUUID(), now + 1200);
            }
            if (BIRDS_QUIET_UNTIL.getOrDefault(p.getUUID(), 0L) < now) {
                due(next, Kind.BIRDS, now, r, 8, 20, k, () -> {
                    Vec3 pos = around(p, 6, 16);
                    at(p, ModSounds.AMBIENT_FOREST_BIRDS, pos.add(0, 4 + r.nextInt(5), 0), 0.5f);
                });
            }
        }
        if (surface) {
            boolean storm = level.isThundering();
            int min = raining ? 15 : 60, max = raining ? 40 : 120;
            due(next, Kind.WIND, now, r, min, max, k, () -> {
                double a = r.nextDouble() * Math.PI * 2;
                at(p, ModSounds.AMBIENT_WIND, p.getEyePosition().add(Math.cos(a) * 10, 0, Math.sin(a) * 10), storm ? 1.0f : 0.7f);
            });
        }
        sanitySounds(p, next, now, r, night, k);
    }

    private static void sanitySounds(ServerPlayer p, long[] next, long now, RandomSource r, boolean night, float k) {
        var hud = ModAttachments.hud(p);
        if (!hud.enabled() || Jumpscares.immune(p)) return;
        int low = ConfigManager.get().sanity.unsettledMax; // 14
        if (hud.sanity() > low) return;
        boolean deep = hud.sanity() <= ConfigManager.get().sanity.disturbedMax; // 9
        due(next, Kind.FAKE_STEPS, now, r, deep ? 30 : 60, deep ? 90 : 180, k, () -> {
            at(p, ModSounds.AMBIENT_SANITY_FAKE_STEPS, behind(p, 2, 4, 20), 0.9f);
            FAKE_STEPS.put(p.getUUID(), new FakeSteps(now, p.getYRot()));
        });
        due(next, Kind.WALL_SCREAM, now, r, deep ? 90 : 180, deep ? 180 : 360, k, () -> at(p, ModSounds.AMBIENT_SANITY_WALL_SCREAM, wallSpot(p, r), 1.0f));
        due(next, Kind.FAKE_CREEPER, now, r, deep ? 120 : 240, deep ? 240 : 480, k, () -> {
            Vec3 pos = behind(p, 1.5, 2.5, 20);
            p.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.CREEPER_PRIMED), SoundSource.HOSTILE,
                    pos.x, pos.y, pos.z, 1f, 1f, r.nextLong()));
        });
        if (night) due(next, Kind.BREATH, now, r, 180, 360, k, () -> at(p, ModSounds.AMBIENT_FOREST_BREATH, behind(p, 2, 3, 20), 0.5f));
    }

    /** Runs {@code action} when the timer of {@code kind} is due and re-arms it with a random interval (seconds / rate). */
    private static void due(long[] next, Kind kind, long now, RandomSource r, int minSec, int maxSec, float rate, Runnable action) {
        int i = kind.ordinal();
        if (next[i] == 0) { // first time: wait a random part of the interval so nothing fires the moment you join
            next[i] = now + (long) (r.nextInt(Math.max(1, maxSec - minSec + 1)) + minSec) * 20L;
            return;
        }
        if (now < next[i]) return;
        action.run();
        next[i] = now + (long) ((minSec + r.nextInt(Math.max(1, maxSec - minSec + 1))) / Math.max(0.1f, rate)) * 20L;
    }

    // --- positions ---

    private static Vec3 around(ServerPlayer p, double min, double max) {
        RandomSource r = p.getRandom();
        double a = r.nextDouble() * Math.PI * 2, d = min + r.nextDouble() * (max - min);
        return p.position().add(Math.cos(a) * d, 0, Math.sin(a) * d);
    }

    /** Behind the player's back: {@code spreadDeg} either side of directly behind (and 120-240 degrees from the view for 60). */
    private static Vec3 behind(ServerPlayer p, double min, double max, double spreadDeg) {
        RandomSource r = p.getRandom();
        double yaw = Math.toRadians(p.getYRot() + 180 + (r.nextDouble() * 2 - 1) * spreadDeg);
        double d = min + r.nextDouble() * (max - min);
        return p.position().add(-Math.sin(yaw) * d, 0, Math.cos(yaw) * d);
    }

    /** The nearest solid block 3-8 blocks away in a random direction (so the sound comes from behind a wall), else 10 down. */
    private static Vec3 wallSpot(ServerPlayer p, RandomSource r) {
        Vec3 eye = p.getEyePosition();
        for (int i = 0; i < 12; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            Vec3 to = eye.add(Math.cos(a) * 8, (r.nextDouble() - 0.4) * 3, Math.sin(a) * 8);
            var hit = p.level().clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            if (hit.getType() == HitResult.Type.BLOCK && hit.getLocation().distanceTo(eye) >= 3) {
                return hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()).scale(-0.6));
            }
        }
        return p.position().add(0, -10, 0);
    }

    private static BlockPos nearestLog(ServerPlayer p, int min, int max) {
        ServerLevel level = p.level();
        BlockPos c = p.blockPosition();
        RandomSource r = p.getRandom();
        for (int i = 0; i < 24; i++) {
            BlockPos q = c.offset(r.nextInt(max * 2 + 1) - max, r.nextInt(5) - 1, r.nextInt(max * 2 + 1) - max);
            if (q.distSqr(c) < min * min || q.distSqr(c) > max * max) continue;
            if (level.getBlockState(q).is(BlockTags.LOGS)) return q;
        }
        return null;
    }

    private static boolean nearSlop(ServerPlayer p, double range) {
        return !p.level().getEntitiesOfClass(AbstractSlop.class, p.getBoundingBox().inflate(range)).isEmpty();
    }

    private static void at(ServerPlayer p, Holder<SoundEvent> sound, Vec3 pos, float volume) {
        p.connection.send(new ClientboundSoundPacket(sound, SoundSource.AMBIENT, pos.x, pos.y, pos.z, volume, 0.9f + p.getRandom().nextFloat() * 0.2f, p.getRandom().nextLong()));
    }
}
