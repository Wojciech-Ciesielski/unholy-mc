package com.unholymc.client.hud;

import com.unholymc.UnholyMC;
import com.unholymc.entity.slop.SlopVariant;
import com.unholymc.network.JumpscarePayload;
import com.unholymc.registry.ModSounds;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Jumpscare sequences (docs/ASSETS.md §5): the 14-tick screamer and the ~5.5 s Spooky Jumpscare.
 * Purely visual/audio; the server applies the sanity loss and the death.
 */
public final class JumpscareOverlay {
    private static final Identifier BLOOD = UnholyMC.id("textures/gui/jumpscare/blood_vignette.png");
    private static final Identifier SPOOKY_FACE = UnholyMC.id("textures/gui/jumpscare/spooky_face.png");
    private static final Identifier STATIC = UnholyMC.id("jumpscare/static");
    private static final RandomSource RANDOM = RandomSource.create();

    private static int kind = -1;
    private static long start;
    private static SlopVariant variant = SlopVariant.SHADOW_WATCHER;
    private static boolean buildupPlayed, spookyPlayed;

    private JumpscareOverlay() {}

    public static void init() {
        HudElementRegistry.addLast(UnholyMC.id("jumpscare"), JumpscareOverlay::render);
    }

    public static void start(JumpscarePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        kind = payload.kind();
        start = mc.level.getGameTime();
        variant = SlopVariant.byId(payload.variant());
        buildupPlayed = spookyPlayed = false;
        if (kind == JumpscarePayload.SCREAMER) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.JUMPSCARE_SCREAMER.value(), 1f, 1f));
        } else {
            mc.getSoundManager().stop(); // the world goes quiet
        }
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (kind < 0 || mc.level == null) return;
        float t = mc.level.getGameTime() - start + delta.getGameTimeDeltaPartialTick(false);
        if (kind == JumpscarePayload.SCREAMER) {
            if (t > 14) kind = -1; else screamer(g, t);
        } else {
            if (t > 160 || (mc.player != null && mc.player.isDeadOrDying() && t > 20)) kind = -1; else spooky(g, mc, t);
        }
    }

    private static void screamer(GuiGraphicsExtractor g, float t) {
        int w = g.guiWidth(), h = g.guiHeight();
        float zoom = t < 6 ? Mth.lerp(t / 6f, 1f, 1.4f) : 1.4f;
        float fade = t < 6 ? 1f : Math.max(0f, 1f - (t - 6f) / 8f);
        int size = (int) (h * 0.6f * zoom);
        int shakeX = t < 6 ? RANDOM.nextInt(25) - 12 : 0;
        int shakeY = t < 6 ? RANDOM.nextInt(25) - 12 : 0;
        int x = (w - size) / 2 + shakeX, y = (h - size) / 2 + shakeY;
        if (t >= 6) {
            g.blit(RenderPipelines.GUI_TEXTURED, BLOOD, 0, 0, 0, 0, w, h, 256, 256, 256, 256, argb(fade, 0xFFFFFF));
        }
        int off = 3 + RANDOM.nextInt(4);
        // chromatic aberration: three tinted copies, then the image itself
        g.blit(RenderPipelines.GUI_TEXTURED, variant.texture, x - off, y, 0, 0, size, size, 512, 512, 512, 512, argb(fade * 0.6f, 0xFF0000));
        g.blit(RenderPipelines.GUI_TEXTURED, variant.texture, x + off, y, 0, 0, size, size, 512, 512, 512, 512, argb(fade * 0.6f, 0x00FF00));
        g.blit(RenderPipelines.GUI_TEXTURED, variant.texture, x, y + off, 0, 0, size, size, 512, 512, 512, 512, argb(fade * 0.6f, 0x0000FF));
        g.blit(RenderPipelines.GUI_TEXTURED, variant.texture, x, y, 0, 0, size, size, 512, 512, 512, 512, argb(fade, 0xFFFFFF));
        staticNoise(g, 0.5f * fade);
    }

    private static void spooky(GuiGraphicsExtractor g, Minecraft mc, float t) {
        int w = g.guiWidth(), h = g.guiHeight();
        if (t >= 20 && !buildupPlayed) {
            buildupPlayed = true;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.JUMPSCARE_SPOOKY_BUILDUP.value(), 1f, 1f));
        }
        if (t < 20) return;
        if (t < 80) {
            float dark = Math.min(0.85f, (t - 20f) / 30f * 0.85f);
            g.fill(0, 0, w, h, argb(dark, 0x000000));
            // the red text flashes three times
            if ((t >= 32 && t < 38) || (t >= 47 && t < 53) || (t >= 62 && t < 68)) {
                Component text = Component.translatable("jumpscare.unholymc.spooky_text");
                g.pose().pushMatrix();
                g.pose().translate(w / 2f, h / 2f);
                g.pose().scale(2.5f, 2.5f);
                g.centeredText(mc.font, text, 0, -4, 0xFFC01020);
                g.pose().popMatrix();
            }
            return;
        }
        if (!spookyPlayed) {
            spookyPlayed = true;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.JUMPSCARE_SPOOKY.value(), 1f, 1f));
        }
        g.fill(0, 0, w, h, 0xFF000000);
        if (t < 110) {
            float zoom = Mth.lerp((t - 80f) / 30f, 1f, 1.2f);
            int size = (int) (Math.max(w, h) * zoom); // cover, no margins
            int x = (w - size) / 2 + RANDOM.nextInt(41) - 20, y = (h - size) / 2 + RANDOM.nextInt(41) - 20;
            g.blit(RenderPipelines.GUI_TEXTURED, SPOOKY_FACE, x, y, 0, 0, size, size, 512, 512, 512, 512, 0xFFFFFFFF);
            staticNoise(g, 0.8f);
            if (((int) t) % 2 == 0) g.fill(0, 0, w, h, argb(0.55f, 0xFFFFFF)); // harsh flicker in place of a colour inversion
        }
    }

    /** The animated static sprite tiled over the screen. */
    private static void staticNoise(GuiGraphicsExtractor g, float alpha) {
        if (alpha <= 0.01f) return;
        int tile = 64;
        int color = argb(alpha, 0xFFFFFF);
        for (int x = 0; x < g.guiWidth(); x += tile) {
            for (int y = 0; y < g.guiHeight(); y += tile) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, STATIC, x, y, tile, tile, color);
            }
        }
    }

    private static int argb(float alpha, int rgb) {
        return ((int) (Mth.clamp(alpha, 0f, 1f) * 255f) << 24) | (rgb & 0xFFFFFF);
    }
}
