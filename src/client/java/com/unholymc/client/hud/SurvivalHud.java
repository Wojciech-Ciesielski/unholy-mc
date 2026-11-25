package com.unholymc.client.hud;

import com.unholymc.UnholyMC;
import com.unholymc.attachment.HudState;
import com.unholymc.attachment.ModAttachments;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Sanity bar (brains) above the hearts, hydration bar (drops) above the hunger bar (DECISIONS #33).
 * Vertical placement goes through Fabric's status bar height registry, so armor / air bubbles
 * are pushed up automatically.
 */
public final class SurvivalHud {
    public static final Identifier SANITY_ID = UnholyMC.id("sanity_bar");
    public static final Identifier HYDRATION_ID = UnholyMC.id("hydration_bar");

    private static final Identifier SANITY_CONTAINER = UnholyMC.id("hud/sanity/container");
    private static final Identifier SANITY_CONTAINER_BLINKING = UnholyMC.id("hud/sanity/container_blinking");
    private static final Identifier SANITY_FULL = UnholyMC.id("hud/sanity/full");
    private static final Identifier SANITY_HALF = UnholyMC.id("hud/sanity/half");
    private static final Identifier SANITY_FULL_LOW = UnholyMC.id("hud/sanity/full_low");
    private static final Identifier SANITY_HALF_LOW = UnholyMC.id("hud/sanity/half_low");
    private static final Identifier HYDRATION_CONTAINER = UnholyMC.id("hud/hydration/container");
    private static final Identifier HYDRATION_FULL = UnholyMC.id("hud/hydration/full");
    private static final Identifier HYDRATION_HALF = UnholyMC.id("hud/hydration/half");
    private static final Identifier HYDRATION_FULL_DEHYDRATED = UnholyMC.id("hud/hydration/full_dehydrated");
    private static final Identifier HYDRATION_HALF_DEHYDRATED = UnholyMC.id("hud/hydration/half_dehydrated");

    private static final RandomSource RANDOM = RandomSource.create();
    private static int lastSanity = -1;
    private static int sanityBlinkUntil;

    private SurvivalHud() {}

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HEALTH_BAR, SANITY_ID, SurvivalHud::renderSanity);
        HudStatusBarHeightRegistry.addLeft(SANITY_ID, p -> sanityVisible(p) ? 10 : 0);
        HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, HYDRATION_ID, SurvivalHud::renderHydration);
        HudStatusBarHeightRegistry.addRight(HYDRATION_ID, p -> hydrationVisible(p) ? 10 : 0);
    }

    private static boolean sanityVisible(Player player) {
        Minecraft mc = Minecraft.getInstance();
        return player != null && mc.gameMode != null && mc.gameMode.canHurtPlayer() && ModAttachments.hud(player).enabled();
    }

    private static boolean hydrationVisible(Player player) {
        if (!sanityVisible(player)) return false;
        // The mount health bar replaces the food row; keep hydration out of its way too.
        return !(player.getVehicle() instanceof LivingEntity vehicle && vehicle.showVehicleHealth());
    }

    private static Player cameraPlayer() {
        return Minecraft.getInstance().getCameraEntity() instanceof Player p ? p : null;
    }

    private static void renderSanity(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Player player = cameraPlayer();
        if (!sanityVisible(player)) return;
        HudState hud = ModAttachments.hud(player);
        int ticks = player.tickCount;
        if (lastSanity >= 0 && hud.sanity() < lastSanity) {
            sanityBlinkUntil = ticks + 20;
        }
        lastSanity = hud.sanity();
        boolean blink = sanityBlinkUntil > ticks && (sanityBlinkUntil - ticks) / 3 % 2 == 1;
        boolean low = hud.sanityStage() >= 2;

        int x0 = graphics.guiWidth() / 2 - 91;
        int y = graphics.guiHeight() - HudStatusBarHeightRegistry.getHeight(SANITY_ID);
        RANDOM.setSeed(ticks * 312871L);
        for (int i = 0; i < 10; i++) {
            int x = x0 + i * 8;
            int yo = y;
            if (hud.sanityStage() == 3) yo += RANDOM.nextInt(2); // trembling when broken
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, blink ? SANITY_CONTAINER_BLINKING : SANITY_CONTAINER, x, yo, 9, 9);
            int halves = i * 2;
            if (halves + 1 < hud.sanity()) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, low ? SANITY_FULL_LOW : SANITY_FULL, x, yo, 9, 9);
            } else if (halves + 1 == hud.sanity()) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, low ? SANITY_HALF_LOW : SANITY_HALF, x, yo, 9, 9);
            }
        }
    }

    private static void renderHydration(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Player player = cameraPlayer();
        if (!hydrationVisible(player)) return;
        HudState hud = ModAttachments.hud(player);
        boolean dehydrated = hud.hydrationStage() >= 2;
        int xRight = graphics.guiWidth() / 2 + 91;
        int y = graphics.guiHeight() - HudStatusBarHeightRegistry.getHeight(HYDRATION_ID);
        int ticks = player.tickCount;
        RANDOM.setSeed(ticks * 1234567L);
        for (int i = 0; i < 10; i++) {
            int x = xRight - i * 8 - 9;
            int yo = y;
            if (hud.hydration() <= 4 && ticks % (hud.hydration() * 3 + 1) == 0) yo += RANDOM.nextInt(3) - 1;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HYDRATION_CONTAINER, x, yo, 9, 9);
            int halves = i * 2;
            if (halves + 1 < hud.hydration()) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, dehydrated ? HYDRATION_FULL_DEHYDRATED : HYDRATION_FULL, x, yo, 9, 9);
            } else if (halves + 1 == hud.hydration()) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, dehydrated ? HYDRATION_HALF_DEHYDRATED : HYDRATION_HALF, x, yo, 9, 9);
            }
        }
    }
}
