package com.unholymc.world;

import com.unholymc.config.ConfigManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WeatherData;

/**
 * DECISIONS #39: rain and storms are frequent and long, sun is rare (vanilla countdowns bent by config multipliers),
 * and at least one rain every few days. Vanilla weather commands (clearWeatherTime) are respected.
 */
public final class WeatherControl {
    private static float rainAcc, thunderAcc;

    private WeatherControl() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(WeatherControl::tick);
    }

    /** One tick of countdown bending; returns the new fractional accumulator. */
    private static float bend(boolean active, float acc, float frequency, float duration,
                              java.util.function.IntSupplier get, java.util.function.IntConsumer set) {
        int time = get.getAsInt();
        if (time <= 1) return 0f;
        if (!active) {
            acc += Math.max(0f, frequency - 1f);
            while (acc >= 1f && time > 1) {
                acc -= 1f;
                time--;
            }
        } else if (duration > 1f) {
            acc += 1f - 1f / duration;
            while (acc >= 1f) {
                acc -= 1f;
                time++;
            }
        } else {
            return 0f;
        }
        if (time != get.getAsInt()) set.accept(time);
        return acc;
    }

    private static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        if (!level.getGameRules().get(GameRules.ADVANCE_WEATHER)) return;
        WeatherData w = level.getWeatherData();
        var cfg = ConfigManager.get().world;
        long now = level.getGameTime();
        WorldState s = BloodMoon.state(level);

        // DECISIONS #39: the vanilla countdowns are bent so that rain/storms are frequent and sun is rare.
        // Counting down faster (extra decrements) shortens the clear spell; slower (re-adding ticks) lengthens the weather.
        rainAcc = bend(w.isRaining(), rainAcc, cfg.rainFrequencyMultiplier, cfg.rainDurationMultiplier, w::getRainTime, w::setRainTime);
        thunderAcc = bend(w.isThundering(), thunderAcc, cfg.thunderFrequencyMultiplier, cfg.thunderDurationMultiplier, w::getThunderTime, w::setThunderTime);

        if (w.isRaining()) {
            if (now - s.lastRainTime() > 1200) level.setAttached(WorldState.TYPE, s.withLastRain(now));
            return;
        }
        if (w.getClearWeatherTime() > 0) return; // /weather clear was used

        if (s.lastRainTime() == 0L) {
            level.setAttached(WorldState.TYPE, s.withLastRain(now)); // start counting from the first tick
        } else if (now - s.lastRainTime() > cfg.guaranteedRainEveryDays * 24000L && w.getRainTime() > 1) {
            w.setRainTime(1); // rain starts on the next weather tick
        }
    }
}
