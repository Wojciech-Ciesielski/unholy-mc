package com.unholymc.client;

import com.unholymc.client.hud.DehydrationOverlay;
import com.unholymc.client.hud.SurvivalHud;
import net.fabricmc.api.ClientModInitializer;

public class UnholyMCClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SurvivalHud.init();
        DehydrationOverlay.init();
    }
}
