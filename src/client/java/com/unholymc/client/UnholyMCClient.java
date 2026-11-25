package com.unholymc.client;

import com.unholymc.client.hud.DehydrationOverlay;
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
        ClientPlayNetworking.registerGlobalReceiver(CrucifixSavePayload.TYPE,
                (payload, context) -> context.client().gameRenderer.displayItemActivation(payload.stack()));
    }
}
