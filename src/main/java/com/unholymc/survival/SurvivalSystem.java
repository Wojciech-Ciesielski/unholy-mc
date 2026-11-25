package com.unholymc.survival;

import com.unholymc.attachment.HudState;
import com.unholymc.attachment.ModAttachments;
import com.unholymc.attachment.PlayerStats;
import com.unholymc.config.ConfigManager;
import com.unholymc.config.UnholyConfig;
import com.unholymc.registry.ModDamageTypes;
import com.unholymc.registry.ModEffects;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server tick driver for hydration + sanity. Rates are expressed per 2 minutes in the config and
 * applied smoothly every tick (DECISIONS #8). Aura scans and status effects run every scanIntervalTicks.
 */
public final class SurvivalSystem {
    public static final float TICKS_PER_2_MIN = 2400f;

    private static final Map<UUID, AuraBreakdown> LAST_AURAS = new HashMap<>();

    private SurvivalSystem() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(SurvivalSystem::tickServer);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                PlayerStats s = ModAttachments.stats(newPlayer);
                s.sanity = ConfigManager.get().sanity.max;
                s.hydration = ConfigManager.get().hydration.max;
                s.auraImmunityUntil = 0;
            }
            syncHud(newPlayer);
        });
        ServerPlayerEvents.JOIN.register(SurvivalSystem::syncHud);
        ServerPlayerEvents.LEAVE.register(p -> LAST_AURAS.remove(p.getUUID()));
    }

    public static AuraBreakdown lastAuras(ServerPlayer player) {
        return LAST_AURAS.get(player.getUUID());
    }

    private static void tickServer(MinecraftServer server) {
        UnholyConfig cfg = ConfigManager.get();
        int interval = Math.max(1, cfg.general.scanIntervalTicks);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive()) continue;
            if (!StatsHelper.isActive(player)) {
                syncHud(player);
                continue;
            }
            boolean scan = (player.tickCount + player.getId()) % interval == 0;
            if (scan || !LAST_AURAS.containsKey(player.getUUID())) {
                LAST_AURAS.put(player.getUUID(), SanityAuras.compute(player));
                SleepTracker.checkStreak(player);
            }
            tickPlayer(player, cfg, scan);
        }
    }

    private static void tickPlayer(ServerPlayer player, UnholyConfig cfg, boolean scan) {
        PlayerStats s = ModAttachments.stats(player);

        // --- hydration ---
        float drainPer2Min = hydrationDrainPer2Min(player, cfg.hydration);
        s.hydration = Math.max(0f, s.hydration - drainPer2Min / TICKS_PER_2_MIN);

        if (s.hydration <= 0f) {
            if (++s.dehydrationTimer >= cfg.hydration.dehydrationDamageIntervalTicks) {
                s.dehydrationTimer = 0;
                ServerLevel level = player.level();
                player.hurtServer(level, level.damageSources().source(ModDamageTypes.DEHYDRATION), cfg.hydration.dehydrationDamage);
            }
        } else {
            s.dehydrationTimer = 0;
        }

        // --- sanity ---
        AuraBreakdown auras = LAST_AURAS.get(player.getUUID());
        if (auras != null) {
            s.sanity = Math.clamp(s.sanity + auras.total() / TICKS_PER_2_MIN, 0f, cfg.sanity.max);
        }

        HudState hud = syncHud(player);
        if (hud.hydrationStage() >= 2 && player.isSprinting()) {
            player.setSprinting(false);
        }
        if (scan) {
            applyEffects(player, hud, cfg);
        }
    }

    public static float hydrationDrainPer2Min(ServerPlayer player, UnholyConfig.Hydration cfg) {
        boolean exertion = player.isSprinting() || player.isSwimming();
        if (player.level().dimension() == Level.NETHER) {
            return cfg.netherPer2Min + (exertion ? cfg.netherExertionExtraPer2Min : 0f);
        }
        boolean inWater = player.isInWater() && !(player.getVehicle() instanceof AbstractBoat);
        Holder<Biome> biome = player.level().getBiome(player.blockPosition());
        if (inWater || matches(biome, cfg.wetBiomes)) {
            return exertion ? cfg.wetBiomeExertionPer2Min : cfg.wetBiomePer2Min;
        }
        float base = matches(biome, cfg.dryBiomes) ? cfg.dryBiomePer2Min : cfg.basePer2Min;
        return base + (exertion ? cfg.exertionExtraPer2Min : 0f);
    }

    private static final Map<String, Object> MATCHER_CACHE = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static boolean matches(Holder<Biome> biome, List<String> entries) {
        for (String entry : entries) {
            Object m = MATCHER_CACHE.computeIfAbsent(entry, e -> e.startsWith("#")
                    ? TagKey.create(Registries.BIOME, Identifier.parse(e.substring(1)))
                    : ResourceKey.create(Registries.BIOME, Identifier.parse(e)));
            if (m instanceof TagKey<?> tag ? biome.is((TagKey<Biome>) tag) : biome.is((ResourceKey<Biome>) m)) {
                return true;
            }
        }
        return false;
    }

    public static int hydrationStage(float hydration, UnholyConfig.Hydration cfg) {
        int v = StatsHelper.displayValue(hydration);
        if (v <= cfg.severeMax) return 3;
        if (v <= cfg.moderateMax) return 2;
        if (v <= cfg.mildMax) return 1;
        return 0;
    }

    public static int sanityStage(float sanity, UnholyConfig.Sanity cfg) {
        int v = StatsHelper.displayValue(sanity);
        if (v <= cfg.brokenMax) return 3;
        if (v <= cfg.disturbedMax) return 2;
        if (v <= cfg.unsettledMax) return 1;
        return 0;
    }

    /** Builds the HUD snapshot and re-attaches it only if it changed (so it is only synced on change). */
    public static HudState syncHud(ServerPlayer player) {
        UnholyConfig cfg = ConfigManager.get();
        PlayerStats s = ModAttachments.stats(player);
        HudState next = new HudState(StatsHelper.isActive(player),
                StatsHelper.displayValue(s.sanity), StatsHelper.displayValue(s.hydration),
                hydrationStage(s.hydration, cfg.hydration), sanityStage(s.sanity, cfg.sanity));
        if (!next.equals(player.getAttached(ModAttachments.HUD_STATE))) {
            player.setAttached(ModAttachments.HUD_STATE, next);
        }
        return next;
    }

    private static void applyEffects(ServerPlayer player, HudState hud, UnholyConfig cfg) {
        int dur = cfg.general.effectRefreshTicks;
        var h = cfg.hydration;
        switch (hud.hydrationStage()) {
            case 1 -> refresh(player, MobEffects.MINING_FATIGUE, dur, h.miningFatigueAmplifier);
            case 2 -> {
                refresh(player, MobEffects.MINING_FATIGUE, dur, h.miningFatigueAmplifier);
                refresh(player, MobEffects.WEAKNESS, dur, h.weaknessModerateAmplifier);
                if (h.nauseaEnabled) refresh(player, MobEffects.NAUSEA, dur + 40, 0);
            }
            case 3 -> {
                refresh(player, MobEffects.MINING_FATIGUE, dur, h.miningFatigueAmplifier);
                refresh(player, MobEffects.WEAKNESS, dur, h.weaknessSevereAmplifier);
                if (h.nauseaEnabled) refresh(player, MobEffects.NAUSEA, dur + 40, 0);
            }
            default -> {}
        }
        if (hud.sanityStage() == 0 && hud.sanity() >= cfg.sanity.wellbeingMin) {
            refresh(player, ModEffects.WELLBEING, dur, cfg.sanity.wellbeingAmplifier);
        }
    }

    private static void refresh(ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && (current.getAmplifier() > amplifier || current.getDuration() > duration / 2 && current.getAmplifier() == amplifier)) {
            return;
        }
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
    }
}
