package com.unholymc.network;

import com.google.gson.Gson;
import com.unholymc.UnholyMC;
import com.unholymc.config.ConfigManager;
import com.unholymc.config.UnholyConfig;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server -> client: the client-side parts of the server config (ambience, Blood Moon look), so that the
 * server stays the single source of truth for every number.
 */
public record ClientSettingsPayload(String json) implements CustomPacketPayload {
    private static final Gson GSON = new Gson();
    public static final Type<ClientSettingsPayload> TYPE = new Type<>(UnholyMC.id("client_settings"));
    public static final StreamCodec<ByteBuf, ClientSettingsPayload> CODEC = ByteBufCodecs.STRING_UTF8.map(ClientSettingsPayload::new, ClientSettingsPayload::json);

    private record Settings(UnholyConfig.Ambience ambience, UnholyConfig.BloodMoon bloodMoon) {}

    public static ClientSettingsPayload fromServerConfig() {
        UnholyConfig c = ConfigManager.get();
        return new ClientSettingsPayload(GSON.toJson(new Settings(c.ambience, c.bloodMoon)));
    }

    /** Client side: adopt the server's values. */
    public void apply() {
        Settings s = GSON.fromJson(json, Settings.class);
        if (s == null) return;
        if (s.ambience() != null) ConfigManager.get().ambience = s.ambience();
        if (s.bloodMoon() != null) ConfigManager.get().bloodMoon = s.bloodMoon();
    }

    public static void send(ServerPlayer player) {
        ServerPlayNetworking.send(player, fromServerConfig());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
