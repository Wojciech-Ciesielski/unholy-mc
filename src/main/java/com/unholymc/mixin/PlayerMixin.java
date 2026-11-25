package com.unholymc.mixin;

import com.unholymc.attachment.ModAttachments;
import com.unholymc.survival.SleepTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Shadow
    public abstract boolean isSleepingLongEnough();

    /**
     * A normal (non-forceful) wake-up after a full sleep counts as a slept night - but only once the night is over
     * (skipped or slept until dawn). "Leave Bed" in the middle of the night is also a non-forceful wake-up, so it must
     * not count, or the reward could be farmed on servers where the night is never skipped.
     */
    @Inject(method = "stopSleepInBed", at = @At("HEAD"))
    private void unholymc$onWakeUp(boolean forcefulWakeUp, boolean updateLevelList, CallbackInfo ci) {
        if (!forcefulWakeUp && (Object) this instanceof ServerPlayer player && isSleepingLongEnough() && !player.level().isDarkOutside()) {
            SleepTracker.onWokeUp(player);
        }
    }

    /** Hydration 9-5 and lower: no sprinting (checked on both sides; the client reads the synced HUD state). */
    @Inject(method = "hasEnoughFoodToDoExhaustiveManoeuvres", at = @At("RETURN"), cancellable = true)
    private void unholymc$noSprintWhenDehydrated(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && !ModAttachments.hud((Player) (Object) this).canSprint()) {
            cir.setReturnValue(false);
        }
    }
}
