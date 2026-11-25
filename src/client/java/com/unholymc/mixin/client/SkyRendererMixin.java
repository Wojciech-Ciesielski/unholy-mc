package com.unholymc.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.unholymc.UnholyMC;
import com.unholymc.client.world.ClientWorldEffects;
import com.unholymc.config.ConfigManager;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.MoonPhase;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blood Moon look (ASSETS 5a): an extra 9th quad with unholymc:blood_moon is appended to the moon buffer,
 * and while the Blood Moon is visible the moon uses it and is drawn x1.6 larger.
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    @Inject(method = "buildMoonPhases", at = @At("HEAD"), cancellable = true)
    private static void unholymc$buildMoonPhases(TextureAtlas atlas, CallbackInfoReturnable<GpuBuffer> cir) {
        MoonPhase[] phases = MoonPhase.values();
        VertexFormat format = DefaultVertexFormat.POSITION_TEX;
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized((phases.length + 1) * 4 * format.getVertexSize())) {
            BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, format);
            for (MoonPhase phase : phases) {
                quad(builder, atlas.getSprite(Identifier.withDefaultNamespace("moon/" + phase.getSerializedName())));
            }
            quad(builder, atlas.getSprite(UnholyMC.id("blood_moon")));
            try (MeshData mesh = builder.buildOrThrow()) {
                cir.setReturnValue(RenderSystem.getDevice().createBuffer(() -> "Moon phases (+blood moon)", 32, mesh.vertexBuffer()));
            }
        }
    }

    private static void quad(BufferBuilder b, TextureAtlasSprite s) {
        b.addVertex(-1.0F, 0.0F, -1.0F).setUv(s.getU1(), s.getV1());
        b.addVertex(1.0F, 0.0F, -1.0F).setUv(s.getU0(), s.getV1());
        b.addVertex(1.0F, 0.0F, 1.0F).setUv(s.getU0(), s.getV0());
        b.addVertex(-1.0F, 0.0F, 1.0F).setUv(s.getU1(), s.getV0());
    }

    @ModifyVariable(method = "renderMoon", at = @At("STORE"), ordinal = 0)
    private int unholymc$bloodMoonQuad(int baseVertex) {
        return ClientWorldEffects.bloodMoonVisible() ? MoonPhase.COUNT * 4 : baseVertex;
    }

    @WrapOperation(method = "renderMoon", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;scale(FFF)Lorg/joml/Matrix4f;"))
    private Matrix4f unholymc$bloodMoonSize(Matrix4fStack stack, float x, float y, float z, Operation<Matrix4f> original) {
        if (ClientWorldEffects.bloodMoonVisible()) {
            float s = ConfigManager.get().bloodMoon.moonScale;
            return original.call(stack, x * s, y, z * s);
        }
        return original.call(stack, x, y, z);
    }
}
