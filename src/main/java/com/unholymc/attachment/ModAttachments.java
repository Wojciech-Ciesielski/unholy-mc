package com.unholymc.attachment;

import com.unholymc.UnholyMC;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;

public final class ModAttachments {
    /** Server-authoritative stats; persisted with the player and kept across death (values reset on respawn). */
    public static final AttachmentType<PlayerStats> PLAYER_STATS = AttachmentRegistry.create(UnholyMC.id("player_stats"),
            b -> b.persistent(PlayerStats.CODEC).initializer(PlayerStats::createDefault).copyOnDeath());

    /** Compact HUD snapshot, synced to the owning client only. */
    public static final AttachmentType<HudState> HUD_STATE = AttachmentRegistry.create(UnholyMC.id("hud_state"),
            b -> b.initializer(() -> HudState.DEFAULT));

    /** Distance (GUI pixels) from the screen centre to the left/right screen edge, set by the player with /hud edge. */
    public static final AttachmentType<Integer> HUD_EDGE = AttachmentRegistry.create(UnholyMC.id("hud_edge"),
            b -> b.persistent(com.mojang.serialization.Codec.INT).initializer(() -> 240).copyOnDeath());

    private ModAttachments() {}

    public static void init() {}

    public static PlayerStats stats(Player player) {
        return player.getAttachedOrCreate(PLAYER_STATS);
    }

    public static HudState hud(Player player) {
        HudState state = player.getAttached(HUD_STATE);
        return state != null ? state : HudState.DEFAULT;
    }
}
