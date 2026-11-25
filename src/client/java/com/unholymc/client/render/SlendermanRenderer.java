package com.unholymc.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.unholymc.UnholyMC;
import com.unholymc.entity.Slenderman;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Slenderman renderer: normal texture, or TV static frames (tick % 4) with positional jitter while he
 * glitch-teleports; Deadly Slenderman is tinted red and 15% larger (docs/ASSETS.md §1.3).
 */
public class SlendermanRenderer extends MobRenderer<Slenderman, SlendermanRenderState, SlendermanModel> {
    private static final Identifier TEXTURE = UnholyMC.id("textures/entity/slenderman/slenderman.png");
    private static final Identifier[] STATIC = {
            UnholyMC.id("textures/entity/slenderman/slenderman_static_0.png"),
            UnholyMC.id("textures/entity/slenderman/slenderman_static_1.png"),
            UnholyMC.id("textures/entity/slenderman/slenderman_static_2.png"),
            UnholyMC.id("textures/entity/slenderman/slenderman_static_3.png")};

    public SlendermanRenderer(EntityRendererProvider.Context context) {
        super(context, new SlendermanModel(context.bakeLayer(SlendermanModel.LAYER)), 0.6f);
    }

    @Override
    public SlendermanRenderState createRenderState() {
        return new SlendermanRenderState();
    }

    @Override
    public void extractRenderState(Slenderman entity, SlendermanRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.glitch = entity.glitchTicks();
        state.deadly = entity.isDeadly();
        state.staticFrame = entity.tickCount % 4;
    }

    /** While glitching, alternate between static and normal every tick. */
    private static boolean showStatic(SlendermanRenderState state) {
        return state.glitch > 0 && (state.glitch % 2 == 0 || state.glitch <= 2);
    }

    @Override
    public Identifier getTextureLocation(SlendermanRenderState state) {
        return showStatic(state) ? STATIC[state.staticFrame] : TEXTURE;
    }

    @Override
    protected int getModelTint(SlendermanRenderState state) {
        return state.deadly ? 0xFFB02020 : -1;
    }

    @Override
    protected void scale(SlendermanRenderState state, PoseStack poseStack) {
        if (state.deadly) poseStack.scale(1.15f, 1.15f, 1.15f);
    }

    @Override
    public Vec3 getRenderOffset(SlendermanRenderState state) {
        if (state.glitch <= 0) return super.getRenderOffset(state);
        double j = 0.1;
        return new Vec3((Math.random() * 2 - 1) * j, 0, (Math.random() * 2 - 1) * j);
    }
}
