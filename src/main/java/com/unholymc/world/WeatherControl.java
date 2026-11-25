package com.unholymc.world;

import com.unholymc.config.ConfigManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WeatherData;

/**
 * GDD 5.4: rain and storms +50% more often (the vanilla countdown to the next rain/storm runs 1.5x as fast),
 * and at least one rain every 3 days. Vanilla weather commands (clearWeatherTime) are respected.
 */
public final class WeatherControl {
    private static float extraRain, extraThunder;

    private WeatherControl() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(WeatherControl::tick);
    }

    private static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        if (!level.getGameRules().get(GameRules.ADVANCE_WEATHER)) return;
        WeatherData w = level.getWeatherData();
        var cfg = ConfigManager.get().world;
        long now = level.getGameTime();
        WorldState s = BloodMoon.state(level);

        if (w.isRaining()) {
            if (now - s.lastRainTime() > 1200) level.setAttached(WorldState.TYPE, s.withLastRain(now));
            return;
        }
        if (w.getClearWeatherTime() > 0) return; // /weather clear was used

        float extra = Math.max(0f, cfg.rainFrequencyMultiplier - 1f);
        extraRain += extra;
        while (extraRain >= 1f) {
            extraRain -= 1f;
            if (w.getRainTime() > 1) w.setRainTime(w.getRainTime() - 1);
        }
        if (!w.isThundering()) {
            extraThunder += extra;
            while (extraThunder >= 1f) {
                extraThunder -= 1f;
                if (w.getThunderTime() > 1) w.setThunderTime(w.getThunderTime() - 1);
            }
        }

        if (s.lastRainTime() == 0L) {
            level.setAttached(WorldState.TYPE, s.withLastRain(now)); // start counting from the first tick
        } else if (now - s.lastRainTime() > cfg.guaranteedRainEveryDays * 24000L && w.getRainTime() > 1) {
            w.setRainTime(1); // rain starts on the next weather tick
        }
    }
}
