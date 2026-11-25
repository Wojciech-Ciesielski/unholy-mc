package com.unholymc.client;

import com.unholymc.client.hud.DehydrationOverlay;
import com.unholymc.client.hud.JumpscareOverlay;
import com.unholymc.client.render.BillboardRenderer;
import com.unholymc.network.JumpscarePayload;
import com.unholymc.network.ClientSettingsPayload;
import com.unholymc.client.world.ClientWorldEffects;
import com.unholymc.client.sound.ElevenLoop;
import com.unholymc.client.hud.SurvivalHud;
import com.unholymc.network.CrucifixSavePayload;
import com.unholymc.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class UnholyMCClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SurvivalHud.init();
        DehydrationOverlay.init();
        EntityRenderers.register(ModEntities.THROWN_HOLY_WATER, ThrownItemRenderer::new);
        EntityRenderers.register(ModEntities.AGGRESSIVE_SLOP, ctx -> new BillboardRenderer<>(ctx, 0.4f));
        EntityRenderers.register(ModEntities.NEUTRAL_SLOP, ctx -> new BillboardRenderer<>(ctx, 0.35f));
        EntityRenderers.register(ModEntities.UNHOLY_SLOP, ctx -> new BillboardRenderer<>(ctx, 0.4f));
        EntityRenderers.register(ModEntities.HALLUCINATION, ctx -> new BillboardRenderer<>(ctx, 0f));
        EntityRenderers.register(ModEntities.SLENDERMAN_HALLUCINATION, ctx -> new BillboardRenderer<>(ctx, 0f));
        JumpscareOverlay.init();
        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
                com.unholymc.client.render.SlendermanModel.LAYER, com.unholymc.client.render.SlendermanModel::createLayer);
        EntityRenderers.register(ModEntities.SLENDERMAN, com.unholymc.client.render.SlendermanRenderer::new);
        ClientWorldEffects.init();
        ElevenLoop.init();
        ClientPlayNetworking.registerGlobalReceiver(ClientSettingsPayload.TYPE, (payload, context) -> payload.apply());
        ClientPlayNetworking.registerGlobalReceiver(JumpscarePayload.TYPE, (payload, context) -> JumpscareOverlay.start(payload));
        ClientPlayNetworking.registerGlobalReceiver(CrucifixSavePayload.TYPE,
                (payload, context) -> context.client().gameRenderer.displayItemActivation(payload.stack()));
    }
}
