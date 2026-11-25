package com.unholymc.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.unholymc.client.world.ClientWorldEffects;
import com.unholymc.client.world.HolyFluidModel;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Holy ponds on a full moon night render with the holy water textures (DECISIONS #16). */
@Mixin(FluidRenderer.class)
public abstract class FluidRendererMixin {
    @WrapOperation(method = "tesselate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/FluidStateModelSet;get(Lnet/minecraft/world/level/material/FluidState;)Lnet/minecraft/client/renderer/block/FluidModel;"))
    private FluidModel unholymc$holyWater(FluidStateModelSet set, FluidState state, Operation<FluidModel> original, @Local(argsOnly = true) BlockPos pos) {
        FluidModel model = original.call(set, state);
        if (state.is(FluidTags.WATER) && ClientWorldEffects.isHolyPondWater(pos)) return HolyFluidModel.of(model);
        return model;
    }
}
