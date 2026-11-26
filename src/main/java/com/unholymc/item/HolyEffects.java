package com.unholymc.item;

import com.unholymc.entity.HolyRepellable;
import com.unholymc.registry.ModDamageTypes;
import com.unholymc.registry.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.ToDoubleFunction;

/** Shared area effect of holy items: repel + blind mod creatures, optional holy damage. Never affects Slenderman. */
public final class HolyEffects {
    private HolyEffects() {}

    /**
     * @param damage per-target damage in HP (0 = none); called for every living non-player target in range
     * @return number of entities affected
     */
    public static int burst(ServerLevel level, Vec3 center, double radius, @Nullable Entity direct, @Nullable Entity owner,
                            int blindTicks, int repelTicks, ToDoubleFunction<LivingEntity> damage) {
        int affected = 0;
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !(e instanceof Player))) {
            if (target.distanceToSqr(center) > radius * radius || target.is(ModTags.HOLY_IMMUNE)) continue;
            boolean modCreature = target.is(ModTags.HOLY_VULNERABLE);
            if (modCreature) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blindTicks, 0), owner);
                if (target instanceof HolyRepellable repellable) repellable.repelFrom(center, repelTicks);
            }
            double dmg = damage.applyAsDouble(target);
            if (dmg > 0) {
                target.hurtServer(level, level.damageSources().source(ModDamageTypes.HOLY, direct, owner), (float) dmg);
                if (direct instanceof Player) { // the crucifix: its blow sounds where it lands
                    level.playSound(null, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                            com.unholymc.registry.ModSounds.ITEM_CRUCIFIX_HIT, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1f);
                }
            }
            if (modCreature || dmg > 0) affected++;
        }
        return affected;
    }
}
