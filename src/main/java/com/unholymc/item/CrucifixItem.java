package com.unholymc.item;

import com.unholymc.config.ConfigManager;
import com.unholymc.registry.ModEnchantments;
import com.unholymc.registry.ModItems;
import com.unholymc.registry.ModSounds;
import com.unholymc.registry.ModTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Crucifix (GDD 1.2). Active use (2 s cooldown, 1 durability): repel + blind mod creatures in 5 blocks;
 * with Holiness also deals holy damage. Passive effects (light, +sanity, hallucination immunity) are
 * driven by {@link #isHeldForPassive}; the totem function is in LivingEntityMixin (main hand only).
 */
public class CrucifixItem extends Item {
    public CrucifixItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        var cfg = ConfigManager.get().crucifix;
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.PASS;
        if (level instanceof ServerLevel serverLevel) {
            int holiness = Math.min(3, ModEnchantments.level(level, ModEnchantments.HOLINESS, stack));
            HolyEffects.burst(serverLevel, player.position(), cfg.radius, player, player, cfg.blindnessTicks, cfg.repelTicks,
                    target -> damageFor(target, holiness));
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ITEM_CRUCIFIX_USE, SoundSource.PLAYERS, 1f, 1f);
            serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 40, cfg.radius / 2, 0.6, cfg.radius / 2, 0.02);
            stack.hurtAndBreak(1, player, hand);
        }
        player.getCooldowns().addCooldown(stack, cfg.cooldownTicks);
        return InteractionResult.SUCCESS;
    }

    private static double damageFor(LivingEntity target, int holiness) {
        if (holiness <= 0) return 0;
        var cfg = ConfigManager.get().crucifix;
        int i = holiness - 1;
        if (target.is(ModTags.HOLY_VULNERABLE)) {
            double dmg = cfg.modMobDamage[Math.min(i, cfg.modMobDamage.length - 1)];
            return target.is(ModTags.UNHOLY) ? dmg * cfg.unholyMultiplier : dmg;
        }
        if (target instanceof Enemy) {
            return cfg.regularMobDamage[Math.min(i, cfg.regularMobDamage.length - 1)];
        }
        return 0;
    }

    /** Passive effects: main hand, plus the off hand unless disabled in the config. */
    public static boolean isHeldForPassive(LivingEntity entity) {
        return entity.getMainHandItem().is(ModItems.CRUCIFIX)
                || (ConfigManager.get().crucifix.passiveInOffhand && entity.getOffhandItem().is(ModItems.CRUCIFIX));
    }
}
