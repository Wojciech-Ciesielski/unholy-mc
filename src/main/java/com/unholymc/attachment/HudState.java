package com.unholymc.attachment;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Client-visible snapshot of the survival stats. Synced (owner only) by the Fabric attachment API,
 * and only re-set on the server when one of the values actually changes.
 *
 * @param enabled         false in creative/spectator/peaceful: HUD hidden, no effects
 * @param sanity          0..20 (rounded up)
 * @param hydration       0..20 (rounded up)
 * @param hydrationStage  0 = ok, 1 = mild, 2 = moderate, 3 = severe
 * @param sanityStage     0 = wellbeing, 1 = unsettled, 2 = disturbed, 3 = broken
 */
public record HudState(boolean enabled, int sanity, int hydration, int hydrationStage, int sanityStage) {
    public static final HudState DEFAULT = new HudState(false, 20, 20, 0, 0);

    public static final StreamCodec<RegistryFriendlyByteBuf, HudState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, HudState::enabled,
            ByteBufCodecs.VAR_INT, HudState::sanity,
            ByteBufCodecs.VAR_INT, HudState::hydration,
            ByteBufCodecs.VAR_INT, HudState::hydrationStage,
            ByteBufCodecs.VAR_INT, HudState::sanityStage,
            HudState::new);

    public boolean canSprint() {
        return !enabled || hydrationStage < 2;
    }
}
