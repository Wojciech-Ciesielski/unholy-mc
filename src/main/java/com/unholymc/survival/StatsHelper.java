package com.unholymc.survival;

import com.unholymc.attachment.ModAttachments;
import com.unholymc.attachment.PlayerStats;
import com.unholymc.config.ConfigManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;

/** Central place that mutates sanity / hydration so clamping and HUD sync stay consistent. */
public final class StatsHelper {
    private StatsHelper() {}

    /** DECISIONS #31: Peaceful / Creative / Spectator disable sanity and hydration. */
    public static boolean isActive(ServerPlayer player) {
        return !player.isCreative() && !player.isSpectator()
                && player.level().getDifficulty() != Difficulty.PEACEFUL;
    }

    public static void addSanity(ServerPlayer player, float delta) {
        if (!isActive(player)) return;
        PlayerStats s = ModAttachments.stats(player);
        if (delta < 0 && player.level().getGameTime() < s.auraImmunityUntil) return;
        setSanity(player, s.sanity + delta);
    }

    public static void setSanity(ServerPlayer player, float value) {
        PlayerStats s = ModAttachments.stats(player);
        s.sanity = Mth.clamp(value, 0f, ConfigManager.get().sanity.max);
        SurvivalSystem.syncHud(player);
    }

    public static void addHydration(ServerPlayer player, float delta) {
        if (!isActive(player)) return;
        PlayerStats s = ModAttachments.stats(player);
        setHydration(player, s.hydration + delta);
    }

    public static void setHydration(ServerPlayer player, float value) {
        PlayerStats s = ModAttachments.stats(player);
        s.hydration = Mth.clamp(value, 0f, ConfigManager.get().hydration.max);
        SurvivalSystem.syncHud(player);
    }

    /** Display value: points rounded up, so the bar never shows empty while > 0. */
    public static int displayValue(float value) {
        return Mth.ceil(value - 1.0E-4f);
    }
}
