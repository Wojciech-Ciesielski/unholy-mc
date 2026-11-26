package com.unholymc.mixin;

import com.unholymc.config.ConfigManager;
import com.unholymc.registry.ModItems;
import com.unholymc.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DeathProtection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /**
     * Hit particles for our mobs: the vanilla client only sees an invisible stand-in zombie, so a hit would show
     * nothing. Damage indicators and crits spawn at the mob's body.
     */
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void unholymc$hitParticles(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!cir.getReturnValueZ() || amount <= 0f) return;
        if (!(self instanceof com.unholymc.entity.BillboardEntity || self instanceof com.unholymc.entity.Slenderman)) return;
        double x = self.getX(), y = self.getY() + self.getBbHeight() * 0.6, z = self.getZ();
        int n = Math.min(12, 2 + Math.round(amount));
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR, x, y, z, Math.max(1, n / 3), 0.2, 0.3, 0.2, 0.1);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, x, y, z, n, self.getBbWidth() * 0.4, self.getBbHeight() * 0.25, self.getBbWidth() * 0.4, 0.25);
    }

    /**
     * Crucifix totem (GDD 1.2, DECISIONS #24): only in the main hand, only if no real totem saved the player
     * (vanilla runs first, so totems always win), and the crucifix is destroyed regardless of enchantments.
     */
    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"), cancellable = true)
    private void unholymc$crucifixTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)) return;
        ItemStack held = player.getMainHandItem();
        if (!held.is(ModItems.CRUCIFIX)) return;

        ItemStack used = held.copy();
        held.shrink(held.getCount());
        player.awardStat(Stats.ITEM_USED.get(ModItems.CRUCIFIX));
        player.setHealth(ConfigManager.get().crucifix.totemHealth);
        DeathProtection.TOTEM_OF_UNDYING.applyEffects(used, player);
        player.level().broadcastEntityEvent(player, (byte) 35);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ITEM_CRUCIFIX_SAVE, SoundSource.PLAYERS, 1f, 1f);
        cir.setReturnValue(true);
    }
}
