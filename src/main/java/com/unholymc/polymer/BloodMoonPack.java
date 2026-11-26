package com.unholymc.polymer;

import com.unholymc.UnholyMC;
import com.unholymc.world.BloodMoon;
import eu.pb4.polymer.autohost.api.AutoHostUtils;
import eu.pb4.polymer.autohost.api.ResourcePackDataProvider;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Blood Moon look for vanilla clients (docs/ASSETS.md 5a): a small overlay resource pack (blood moon texture, red sky,
 * stars, clouds, fog and colour maps) that is pushed on top of the main pack when the event starts and popped at dawn,
 * so the sky really changes without a client mod. The overlay is zipped from the mod's own resources at server start and
 * served by Polymer's auto-host next to the main pack. Each push or pop reloads the client's resources once (~1-3 s).
 */
public final class BloodMoonPack {
    private static final UUID PACK_ID = UUID.nameUUIDFromBytes("unholymc:blood_moon_overlay".getBytes(StandardCharsets.UTF_8));
    private static final Identifier HOST_ID = UnholyMC.id("blood_moon");
    private static final String META = "{\"pack\":{\"description\":\"The Unholy Minecraft: Blood Moon\",\"min_format\":88,\"max_format\":88}}";

    private static String hash;
    private static final Set<UUID> PUSHED = new HashSet<>();

    private BloodMoonPack() {}

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(BloodMoonPack::build);
        ServerTickEvents.END_SERVER_TICK.register(BloodMoonPack::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PUSHED.remove(handler.player.getUUID()));
    }

    /** Zips the overlay from the mod jar and registers it with the auto-host. */
    private static void build(MinecraftServer server) {
        PUSHED.clear();
        hash = null;
        Path out = FabricLoader.getInstance().getGameDir().resolve("polymer").resolve("unholymc_blood_moon.zip");
        var container = FabricLoader.getInstance().getModContainer(UnholyMC.MOD_ID);
        if (container.isEmpty()) return;
        Path root = container.get().findPath("resourcepacks/blood_moon").orElse(null);
        if (root == null) return;
        try {
            Files.createDirectories(out.getParent());
            try (OutputStream os = Files.newOutputStream(out); ZipOutputStream zip = new ZipOutputStream(os); Stream<Path> files = Files.walk(root)) {
                zip.putNextEntry(new ZipEntry("pack.mcmeta"));
                zip.write(META.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                for (Path f : (Iterable<Path>) files.filter(Files::isRegularFile).sorted()::iterator) {
                    String name = root.relativize(f).toString().replace('\\', '/');
                    if (name.equals("pack.mcmeta")) continue;
                    zip.putNextEntry(new ZipEntry(name));
                    Files.copy(f, zip);
                    zip.closeEntry();
                }
            }
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            StringBuilder hex = new StringBuilder();
            for (byte b : sha1.digest(Files.readAllBytes(out))) hex.append(String.format("%02x", b));
            hash = hex.toString();
            AutoHostUtils.registerHostedFile(HOST_ID, out);
            UnholyMC.LOGGER.info("Blood Moon overlay pack ready ({})", hash);
        } catch (IOException | java.security.NoSuchAlgorithmException e) {
            UnholyMC.LOGGER.warn("Could not build the Blood Moon overlay pack: {}", e.toString());
        }
    }

    private static void tick(MinecraftServer server) {
        if (hash == null) return;
        ResourcePackDataProvider provider = ResourcePackDataProvider.getActive();
        if (provider == null) return;
        boolean active = BloodMoon.isActive(server.overworld());
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if ((p.tickCount + p.getId()) % 20 != 0) continue;
            boolean want = active && p.level().dimension() == Level.OVERWORLD;
            boolean has = PUSHED.contains(p.getUUID());
            if (want && !has) {
                // only after the client has the main pack, so the two reloads do not collide
                if (!PolymerResourcePackUtils.hasMainPack(p.connection)) continue;
                String url = provider.getFilePath(p.connection.getPacketContext(), HOST_ID, hash);
                p.connection.send(new ClientboundResourcePackPushPacket(PACK_ID, url, hash, true, Optional.empty()));
                PUSHED.add(p.getUUID());
            } else if (!want && has) {
                p.connection.send(new ClientboundResourcePackPopPacket(Optional.of(PACK_ID)));
                PUSHED.remove(p.getUUID());
            }
        }
    }
}
