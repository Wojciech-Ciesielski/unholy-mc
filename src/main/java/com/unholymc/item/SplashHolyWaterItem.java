package com.unholymc.item;

import com.unholymc.entity.ThrownHolyWater;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Throwable holy water (brewed from holy water + gunpowder). */
public class SplashHolyWaterItem extends HolyWaterItem {
    public SplashHolyWaterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS,
                0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
        if (level instanceof ServerLevel serverLevel) {
            Projectile.spawnProjectileFromRotation((l, owner, s) -> new ThrownHolyWater(l, owner, s), serverLevel, stack, player, -20f, 0.5f, 1f);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
