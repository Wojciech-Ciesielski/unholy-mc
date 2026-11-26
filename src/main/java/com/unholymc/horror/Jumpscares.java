package com.unholymc.horror;

import com.unholymc.attachment.ModAttachments;
import com.unholymc.config.ConfigManager;
import com.unholymc.entity.slop.SlopVariant;
import com.unholymc.item.CrucifixItem;
import com.unholymc.registry.ModDamageTypes;
import com.unholymc.registry.ModSounds;
import com.unholymc.survival.SleepTracker;
import com.unholymc.survival.StatsHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Jumpscares (GDD 5.2/5.3, DECISIONS #5): random screamers rolled every 60 ticks with a chance depending on
 * the sanity stage, unsettling background noises, the deadly Spooky Jumpscare after 10 slept nights.
 * A crucifix in hand blocks all of it.
 */
public final class Jumpscares {
    private static final Map<UUID, Long> SPOOKY_DEATHS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_NOISE = new HashMap<>();

    private Jumpscares() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(Jumpscares::tick);
        SleepTracker.onDeadlyStreak(Jumpscares::spooky);
    }

    public static boolean immune(ServerPlayer player) {
        return !StatsHelper.isActive(player) || CrucifixItem.isHeldForPassive(player);
    }

    /** One-off sanity loss of a jumpscare (not affected by the Unholy Sword multiplier, DECISIONS #10). */
    private static void loseSanity(ServerPlayer player) {
        var s = ModAttachments.stats(player);
        StatsHelper.setSanity(player, s.sanity + ConfigManager.get().jumpscares.sanityLoss);
    }

    /** A full-screen screamer. Returns false if the player is immune. */
    public static boolean screamer(ServerPlayer player) {
        return screamer(player, false);
    }

    /** {@code forced} (test command): plays even in creative/spectator or with a crucifix. */
    public static boolean screamer(ServerPlayer player, boolean forced) {
        if (!forced && immune(player)) return false;
        int variant = player.getRandom().nextInt(SlopVariant.values().length);
        com.unholymc.polymer.ServerFx.screamer(player, SlopVariant.byId(variant));
        loseSanity(player);
        return true;
    }

    /** A neutral slop screaming right next to the player: sanity loss without the screen effect. */
    public static void slopScare(ServerPlayer player) {
        if (immune(player)) return;
        loseSanity(player);
    }

    /** The Spooky Jumpscare: ~5.5 s sequence on the client, the server kills the player afterwards. */
    public static void spooky(ServerPlayer player) {
        com.unholymc.polymer.ServerFx.spooky(player);
        SPOOKY_DEATHS.put(player.getUUID(), player.level().getGameTime() + ConfigManager.get().jumpscares.spookyDeathDelayTicks);
        SleepTracker.setStreak(player, 0);
    }

    private static void tick(MinecraftServer server) {
        var cfg = ConfigManager.get().jumpscares;
        // pending Spooky Jumpscare deaths
        for (Iterator<Map.Entry<UUID, Long>> it = SPOOKY_DEATHS.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) {
                it.remove();
            } else if (p.level().getGameTime() >= e.getValue()) {
                it.remove();
                if (p.isAlive()) p.hurtServer(p.level(), p.level().damageSources().source(ModDamageTypes.SPOOKY_JUMPSCARE), Float.MAX_VALUE);
            }
        }

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isAlive() || immune(p)) continue;
            int stage = ModAttachments.hud(p).sanityStage();
            if (stage == 0) continue;
            long now = p.level().getGameTime();

            if ((p.tickCount + p.getId()) % Math.max(1, cfg.rollIntervalTicks) == 0) {
                float chance = switch (stage) {
                    case 1 -> cfg.unsettledChance;
                    case 2 -> cfg.disturbedChance;
                    default -> cfg.brokenChance;
                };
                if (p.getRandom().nextFloat() < chance) screamer(p);
            }

            if (stage >= 2) {
                long next = NEXT_NOISE.getOrDefault(p.getUUID(), 0L);
                if (now >= next) {
                    playBehind(p, ModSounds.AMBIENT_LOW_SANITY, 1.2f);
                    int span = Math.max(1, cfg.lowSanitySoundMaxTicks - cfg.lowSanitySoundMinTicks);
                    NEXT_NOISE.put(p.getUUID(), now + cfg.lowSanitySoundMinTicks + p.getRandom().nextInt(span));
                }
            }
            if (stage == 3 && now % Math.max(1, cfg.heartbeatIntervalTicks) == 0) {
                playBehind(p, ModSounds.AMBIENT_HEARTBEAT, 0.8f);
            }
        }
    }

    /** A sound only this player hears, from a few blocks behind them. */
    public static void playBehind(ServerPlayer p, Holder<SoundEvent> sound, float volume) {
        Vec3 pos = p.position().subtract(p.getViewVector(1f).scale(3));
        p.connection.send(new ClientboundSoundPacket(sound, SoundSource.AMBIENT, pos.x, pos.y + 1, pos.z, volume, 1f, p.getRandom().nextLong()));
    }
}
