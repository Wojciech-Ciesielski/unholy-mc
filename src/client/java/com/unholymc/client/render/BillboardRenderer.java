package com.unholymc.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.unholymc.entity.BillboardEntity;
import com.unholymc.entity.slop.SlopVariant;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Camera-facing PNG billboard (docs/ASSETS.md §1.1): a square quad of the variant's size, bottom edge on the
 * entity's feet, rotated only around Y. Slops use world lighting (dark in the dark); hallucinations are
 * black translucent silhouettes, fullbright, without a shadow. Chasing slops jitter a little.
 */
public class BillboardRenderer<T extends LivingEntity & BillboardEntity> extends EntityRenderer<T, BillboardRenderState> {
    private static final int FULL_BRIGHT = 0xF000F0;

    public BillboardRenderer(EntityRendererProvider.Context context, float shadowRadius) {
        super(context);
        this.shadowRadius = shadowRadius;
    }

    @Override
    public BillboardRenderState createRenderState() {
        return new BillboardRenderState();
    }

    @Override
    public void extractRenderState(T entity, BillboardRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        SlopVariant v = entity.variant();
        boolean halluc = entity.isHallucinationLook();
        state.texture = halluc ? v.hallucinationTexture : v.texture;
        state.size = v.billboardSize;
        state.floatOffset = v.floatOffset;
        state.translucent = halluc || v.softAlpha;
        // the hallucination PNGs already carry 60% alpha; Slenderman's minions are drawn more opaque (x1.3)
        state.alpha = halluc && !entity.isSlenderMinion() ? 196 : 255;
        boolean redFlash = entity.isSlenderMinion() && (entity.tickCount % 40) < 2;
        state.hurt = entity.hurtTime > 0 || redFlash;
        state.deathProgress = entity.deathTime > 0 ? Math.min(1f, (entity.deathTime + partialTicks) / 20f) : 0f;
        if (halluc) state.lightCoords = FULL_BRIGHT;
        if (entity.isChasing()) {
            long seed = entity.getId() * 31L + (entity.tickCount / 3);
            state.jitterX = Math.floorMod(seed * 1103515245L + 12345L, 1000L) / 1000f * 0.04f - 0.02f;
            state.jitterZ = Math.floorMod(seed * 22695477L + 1L, 1000L) / 1000f * 0.04f - 0.02f;
        } else {
            state.jitterX = state.jitterZ = 0f;
        }
    }

    @Override
    public void submit(BillboardRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        RenderType type = state.translucent ? RenderTypes.entityTranslucent(state.texture) : RenderTypes.entityCutout(state.texture);
        poseStack.pushPose();
        poseStack.translate(state.jitterX, state.floatOffset, state.jitterZ);
        double dx = camera.pos.x - state.x;
        double dz = camera.pos.z - state.z;
        poseStack.mulPose(Axis.YP.rotation((float) Mth.atan2(dx, dz)));
        float s = state.size;
        float sy = s * (1f - state.deathProgress);
        int color = (state.alpha << 24) | 0xFFFFFF;
        int overlay = OverlayTexture.pack(0f, state.hurt);
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            vertex(buffer, pose, -s / 2, 0, 0f, 1f, color, overlay, light);
            vertex(buffer, pose, s / 2, 0, 1f, 1f, color, overlay, light);
            vertex(buffer, pose, s / 2, sy, 1f, 0f, color, overlay, light);
            vertex(buffer, pose, -s / 2, sy, 0f, 0f, color, overlay, light);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v, int color, int overlay, int light) {
        buffer.addVertex(pose, x, y, 0f).setColor(color).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 1f, 0f);
    }
}
