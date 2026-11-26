package com.unholymc.polymer;

import com.unholymc.UnholyMC;
import com.unholymc.attachment.HudState;
import com.unholymc.attachment.ModAttachments;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sanity (brains) and hydration (drops) bars for vanilla clients (DECISIONS #33), drawn as glyphs of our own
 * font (assets/unholymc/font/hud.json) in the action bar. The text is exactly as wide as the hotbar, so once the
 * client centres it the brains sit right over the hearts and the drops over the hunger bar; the glyphs' negative
 * ascent moves them down from the action bar line to just above the armour row.
 * Other action bar messages pause the bars for a moment (ServerPlayerMixin).
 */
public final class ServerHud {
    private static final Style STYLE = Style.EMPTY.withFont(new FontDescription.Resource(UnholyMC.id("hud"))).withShadowColor(0).withColor(0xFFFFFF);
    private static final char SANITY = '', HYDRATION = '';
    private static final char STEP = '', LAST = '', GAP = '';
    private static final int REFRESH_TICKS = 10, PAUSE_TICKS = 60;

    private record Shown(String text, long at) {}

    private static final Map<UUID, Shown> SHOWN = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SANITY = new HashMap<>();
    private static final Map<UUID, Long> BLINK_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> PAUSED_UNTIL = new HashMap<>();

    private ServerHud() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(ServerHud::tick);
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> {
            UUID id = h.player.getUUID();
            SHOWN.remove(id);
            LAST_SANITY.remove(id);
            BLINK_UNTIL.remove(id);
            PAUSED_UNTIL.remove(id);
        });
    }

    /** Another action bar message is on screen: leave it alone for a while. */
    public static void pause(ServerPlayer player) {
        PAUSED_UNTIL.put(player.getUUID(), player.level().getGameTime() + PAUSE_TICKS);
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            UUID id = p.getUUID();
            long now = p.level().getGameTime();
            HudState hud = ModAttachments.hud(p);
            boolean visible = hud.enabled() && !p.isSpectator();
            Shown shown = SHOWN.get(id);
            if (!visible) {
                if (shown != null && !shown.text.isEmpty()) send(p, "", now);
                LAST_SANITY.remove(id);
                continue;
            }
            Integer last = LAST_SANITY.put(id, hud.sanity());
            if (last != null && hud.sanity() < last) BLINK_UNTIL.put(id, now + 20);
            if (PAUSED_UNTIL.getOrDefault(id, 0L) > now) continue;

            String text = build(p, hud, now);
            if (shown == null || !shown.text.equals(text) || now - shown.at >= REFRESH_TICKS) send(p, text, now);
        }
    }

    private static String build(ServerPlayer p, HudState hud, long now) {
        long blinkUntil = BLINK_UNTIL.getOrDefault(p.getUUID(), 0L);
        boolean blink = blinkUntil > now && (blinkUntil - now) / 3 % 2 == 1;
        boolean low = hud.sanityStage() >= 2;
        StringBuilder sb = new StringBuilder(48);
        for (int i = 0; i < 10; i++) {
            sb.append((char) (SANITY + (blink ? 6 : 0) + (low ? 3 : 0) + fill(i, hud.sanity())));
            sb.append(i < 9 ? STEP : LAST);
        }
        sb.append(GAP);
        // the mount's health replaces the food row: keep the drops out of its way (an empty gap keeps the brains in place)
        boolean mountBar = p.getVehicle() instanceof LivingEntity v && v.showVehicleHealth();
        boolean dry = hud.hydrationStage() >= 2;
        for (int i = 9; i >= 0; i--) {
            if (mountBar) break;
            sb.append((char) (HYDRATION + (dry ? 3 : 0) + fill(i, hud.hydration())));
            sb.append(i > 0 ? STEP : LAST);
        }
        return sb.toString();
    }

    /** 0 = empty, 1 = half, 2 = full for icon {@code i} of a 0..20 bar. */
    private static int fill(int i, int value) {
        int halves = i * 2;
        return halves + 1 < value ? 2 : halves + 1 == value ? 1 : 0;
    }

    private static void send(ServerPlayer p, String text, long now) {
        SHOWN.put(p.getUUID(), new Shown(text, now));
        p.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(text).withStyle(STYLE)));
    }
}
