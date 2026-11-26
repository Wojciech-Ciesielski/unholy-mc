package com.unholymc.survival;

import com.unholymc.attachment.ModAttachments;
import com.unholymc.config.ConfigManager;
import com.unholymc.entity.AuraFilter;
import com.unholymc.registry.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Registry of aura providers + the built-in vanilla-world auras of the GDD (5.2). */
public final class SanityAuras {
    private static final List<AuraProvider> PROVIDERS = new ArrayList<>();

    /** Hook for the Blood Moon (stage 4). */
    public static Predicate<ServerLevel> bloodMoonActive = level -> false;

    private SanityAuras() {}

    public static void register(AuraProvider provider) {
        PROVIDERS.add(provider);
    }

    public static AuraBreakdown compute(ServerPlayer player) {
        AuraBreakdown out = new AuraBreakdown();
        builtIn(player, out);
        for (AuraProvider p : PROVIDERS) {
            p.collect(player, out);
        }
        var stats = ModAttachments.stats(player);
        out.setNegativeImmune(player.level().getGameTime() < stats.auraImmunityUntil);
        return out;
    }

    private static void builtIn(ServerPlayer player, AuraBreakdown out) {
        var cfg = ConfigManager.get().auras;
        ServerLevel level = player.level();

        // Night (no passive drain during the day). Blood Moon triples it.
        if (com.unholymc.world.Night.isNight(level)) {
            float night = cfg.nightPer2Min;
            if (bloodMoonActive.test(level)) {
                night *= cfg.bloodMoonNightMultiplier;
                out.add("blood_moon_night", night);
            } else {
                out.add("night", night);
            }
        }

        // Darkness: light below N as shown by F3 (combined sky+block, with current sky darkening).
        if (level.getMaxLocalRawBrightness(player.blockPosition()) < cfg.darknessLightBelow
                && !LightOverrides.isLit(player, cfg.darknessLightBelow)) {
            out.add("darkness", cfg.darknessPer2Min);
        }

        double maxR = Math.max(Math.max(Math.max(cfg.monstersRadius, cfg.slopsRadius), cfg.bossesRadius),
                Math.max(cfg.friendlyAnimalsRadius, Math.max(cfg.tamedRadius, cfg.playersRadius)));
        AABB box = player.getBoundingBox().inflate(maxR);
        boolean monsters = false, bosses = false, animals = false, tamed = false, slops = false;
        int players = 0;
        for (Entity e : level.getEntities(player, box, Entity::isAlive)) {
            double d2 = e.distanceToSqr(player);
            if (e instanceof ServerPlayer other) {
                if (!other.isSpectator() && d2 <= cfg.playersRadius * cfg.playersRadius) players++;
                continue;
            }
            if (e.is(ModTags.SLOPS)) {
                if (d2 <= cfg.slopsRadius * cfg.slopsRadius && (!(e instanceof AuraFilter f) || f.affects(player))) slops = true;
            } else if (e.is(ModTags.BOSSES)) {
                if (d2 <= cfg.bossesRadius * cfg.bossesRadius) bosses = true;
            } else if (e instanceof Enemy) {
                if (d2 <= cfg.monstersRadius * cfg.monstersRadius) monsters = true;
            } else if (isTamed(e)) {
                if (d2 <= cfg.tamedRadius * cfg.tamedRadius) tamed = true;
            } else if (e.is(ModTags.COMFORTING_ANIMALS)) {
                if (d2 <= cfg.friendlyAnimalsRadius * cfg.friendlyAnimalsRadius) animals = true;
            }
        }
        if (slops) out.add("slops", cfg.slopsPer2Min);
        if (monsters) out.add("monsters", cfg.monstersPer2Min);
        if (bosses) out.add("bosses", cfg.bossesPer2Min);
        if (animals) out.add("friendly_animals", cfg.friendlyAnimalsPer2Min);
        if (tamed) out.add("tamed", cfg.tamedPer2Min);
        if (players > 0) out.add("players", Math.min(players * cfg.playersPer2MinEach, cfg.playersPer2MinMax));
    }

    private static boolean isTamed(Entity e) {
        if (e instanceof TamableAnimal t) return t.isTame();
        return e instanceof OwnableEntity o && o.getOwnerReference() != null;
    }
}
