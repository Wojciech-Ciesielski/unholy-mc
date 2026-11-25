package com.unholymc.client.hud;

import com.unholymc.UnholyMC;
import com.unholymc.attachment.HudState;
import com.unholymc.attachment.ModAttachments;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * Screen effects of dehydration, drawn below the rest of the HUD:
 * stage 1 (14-10) - periodic blur pulses; stage 3 (4-0) - constant blur and eyelid blinks.
 * Blur uses the vanilla blur post-effect (its strength follows the player's "menu background blur" option).
 */
public final class DehydrationOverlay {
    private DehydrationOverlay() {}

    public static void init() {
        HudElementRegistry.addFirst(UnholyMC.id("dehydration_overlay"), DehydrationOverlay::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null || mc.gui.overlay() != null) return; // screens use the one-per-frame blur themselves
        HudState hud = ModAttachments.hud(mc.player);
        if (!hud.enabled() || hud.hydrationStage() == 0) return;

        float time = mc.player.tickCount + delta.getGameTimeDeltaPartialTick(false);
        boolean blur = switch (hud.hydrationStage()) {
            case 1 -> time % 200f < 30f;              // a short blurry spell every 10 s
            case 2 -> time % 160f < 50f;
            default -> true;
        };
        if (blur) {
            graphics.nextStratum();
            graphics.blurBeforeThisStratum();
        }

        if (hud.hydrationStage() == 3) {
            // eyelids: close for ~0.4 s every 4 s
            float phase = time % 80f;
            if (phase < 8f) {
                float closed = Mth.sin(phase / 8f * Mth.PI);
                int alpha = (int) (closed * 255f) & 0xFF;
                graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24);
            }
        }
    }
}
