package com.unholymc.network;

import com.unholymc.survival.ConsumptionHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
    private ModNetworking() {}

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(DrinkFromSourcePayload.TYPE, DrinkFromSourcePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CrucifixSavePayload.TYPE, CrucifixSavePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DrinkFromSourcePayload.TYPE,
                (payload, context) -> ConsumptionHandler.tryDrinkFromSource(context.player()));
    }
}
