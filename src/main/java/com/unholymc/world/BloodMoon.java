package com.unholymc.world;

import com.unholymc.config.ConfigManager;
import com.unholymc.registry.ModSounds;
import com.unholymc.survival.SanityAuras;
import com.unholymc.survival.SleepTracker;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;

/**
 * Blood Moon (GDD 5.4, DECISIONS #17/#18): on every full moon (every 8 days, vanilla phases) there is a 25%
 * chance at nightfall. Lasts until dawn: no sleeping, triple night sanity drain, more spawns, ponds stay
 * unholy, Slenderman turns deadly (stage 5). Vanilla day/night length.
 */
public final class BloodMoon {
    /** Server-side flag for code without a level at hand (mob caps). */
    private static volatile boolean activeOnServer;

    private BloodMoon() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(BloodMoon::tick);
        SanityAuras.bloodMoonActive = BloodMoon::isActive;
    }

    public static WorldState state(Level level) {
        WorldState s = level.getAttached(WorldState.TYPE);
        return s != null ? s : WorldState.DEFAULT;
    }

    private static Level overworldOf(Level level) {
        if (level instanceof ServerLevel sl) return sl.getServer().overworld();
        return level;
    }

    /** Active Blood Moon; only meaningful in the Overworld. */
    public static boolean isActive(Level level) {
        return level.dimension() == Level.OVERWORLD && state(level).bloodMoon();
    }

    /** Set while the Overworld runs natural spawning (server thread only, see ServerChunkCacheMixin). */
    public static volatile boolean spawningInOverworld;

    public static boolean activeOnServer() {
        return activeOnServer;
    }

    public static boolean isFullMoon(Level level) {
        return level.environmentAttributes().getDimensionValue(EnvironmentAttributes.MOON_PHASE) == MoonPhase.FULL_MOON;
    }

    /** Full moon night without a Blood Moon: holy ponds are holy (DECISIONS #16). */
    public static boolean isHolyNight(Level level) {
        return level.dimension() == Level.OVERWORLD && isFullMoon(level) && Night.isNight(level) && !isActive(level);
    }

    public static int daysUntilFullMoon(Level level) {
        int phase = level.environmentAttributes().getDimensionValue(EnvironmentAttributes.MOON_PHASE).index();
        return (MoonPhase.COUNT - phase) % MoonPhase.COUNT;
    }

    private static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        WorldState s = state(level);
        boolean dark = Night.isNight(level);
        if (s.bloodMoon() && !dark) {
            set(level, false); // dawn
        } else if (dark && isFullMoon(level)) {
            long night = nightKey(level);
            if (s.rolledNight() != night) {
                level.setAttached(WorldState.TYPE, s.withRolledNight(night));
                if (level.getRandom().nextFloat() < ConfigManager.get().bloodMoon.chance) set(level, true);
            }
        }
        activeOnServer = state(level).bloodMoon();
    }

    private static long nightKey(ServerLevel level) {
        return Math.floorDiv(level.getOverworldClockTime() - SleepTracker.DAY_TICKS / 2, SleepTracker.DAY_TICKS);
    }

    /** Start or stop; used by the natural roll and by /thm blood_moon. */
    public static void set(ServerLevel overworld, boolean active) {
        WorldState s = state(overworld);
        if (s.bloodMoon() == active) return;
        overworld.setAttached(WorldState.TYPE, s.withBloodMoon(active).withRolledNight(nightKey(overworld)));
        activeOnServer = active;
        if (active) {
            for (ServerPlayer p : overworld.players()) {
                p.connection.send(new ClientboundSetTitlesAnimationPacket(20, 80, 30));
                p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.unholymc.blood_moon").withColor(0xC8281C)));
                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("title.unholymc.blood_moon.sub").withColor(0xC8281C)));
                p.connection.send(new ClientboundSoundPacket(ModSounds.AMBIENT_BLOOD_MOON_RISE, SoundSource.AMBIENT,
                        p.getX(), p.getY(), p.getZ(), 1f, 1f, p.getRandom().nextLong()));
                if (p.isSleeping()) p.stopSleepInBed(true, true);
            }
        }
    }

    public static ServerLevel overworld(ServerLevel any) {
        return any.getServer().overworld();
    }
}
