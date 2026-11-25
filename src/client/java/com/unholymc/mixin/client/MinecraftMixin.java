package com.unholymc.mixin.client;

import com.unholymc.attachment.ModAttachments;
import com.unholymc.network.DrinkFromSourcePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    public @Nullable LocalPlayer player;

    /** Nothing else handled the right click: an empty hand aimed at a water source drinks from it. */
    @Inject(method = "startUseItem", at = @At("TAIL"))
    private void unholymc$drinkFromSource(CallbackInfo ci) {
        LocalPlayer p = this.player;
        if (p == null || !p.getMainHandItem().isEmpty() || !ModAttachments.hud(p).enabled()) return;
        if (ModAttachments.hud(p).hydration() >= 20) return;
        Vec3 from = p.getEyePosition();
        Vec3 to = from.add(p.getViewVector(1f).scale(p.blockInteractionRange()));
        BlockHitResult hit = p.level().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, p));
        if (hit.getType() == HitResult.Type.BLOCK && p.level().getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
            ClientPlayNetworking.send(new DrinkFromSourcePayload());
            p.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }
}
