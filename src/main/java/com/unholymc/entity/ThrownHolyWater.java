package com.unholymc.entity;

import com.unholymc.config.ConfigManager;
import com.unholymc.item.HolyEffects;
import com.unholymc.registry.ModEntities;
import com.unholymc.registry.ModItems;
import com.unholymc.registry.ModTags;
import com.unholymc.survival.StatsHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Thrown splash holy water (GDD 1.1): repels and blinds mod creatures, burns Unholy Slops, refreshes players under it. */
public class ThrownHolyWater extends ThrowableItemProjectile {
    public static final int COLOR = 0x9CE4FF;

    public ThrownHolyWater(EntityType<? extends ThrownHolyWater> type, Level level) {
        super(type, level);
    }

    public ThrownHolyWater(ServerLevel level, LivingEntity owner, ItemStack stack) {
        super(ModEntities.THROWN_HOLY_WATER, owner, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SPLASH_HOLY_WATER;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!(this.level() instanceof ServerLevel level)) return;
        var cfg = ConfigManager.get().holyWater;
        Vec3 center = this.position();

        HolyEffects.burst(level, center, cfg.splashRadius, this, this.getOwner(), cfg.blindnessTicks, cfg.repelTicks,
                target -> target.is(ModTags.UNHOLY) ? cfg.unholySlopDamage : 0);

        // "thrown under yourself": refresh players standing in the splash
        AABB self = new AABB(center, center).inflate(cfg.selfSplashRadius);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, self, p -> p.distanceToSqr(center) <= cfg.selfSplashRadius * cfg.selfSplashRadius)) {
            StatsHelper.addHydration(player, cfg.selfSplashHydration);
            StatsHelper.addSanity(player, cfg.selfSplashSanity);
        }

        level.levelEvent(2002, this.blockPosition(), COLOR);
        this.discard();
    }
}
