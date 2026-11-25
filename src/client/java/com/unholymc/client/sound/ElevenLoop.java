package com.unholymc.client.sound;

import com.unholymc.config.ConfigManager;
import com.unholymc.registry.ModSounds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * GDD 5.4: the slowed, reversed disc 11 loops in the background all the time (replacing vanilla music),
 * 10% louder at night and 15% louder in caves. Base volume comes from the server config.
 */
public final class ElevenLoop extends AbstractTickableSoundInstance {
    private static ElevenLoop current;
    private static int retryDelay;

    private ElevenLoop() {
        super(ModSounds.MUSIC_ELEVEN_REVERSED.value(), SoundSource.MUSIC, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.volume = target();
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.level == null || mc.player == null) {
                if (current != null) {
                    mc.getSoundManager().stop(current);
                    current = null;
                }
                return;
            }
            if (current == null || current.isStopped() || !mc.getSoundManager().isActive(current)) {
                if (--retryDelay > 0) return;
                retryDelay = 600; // a missing .ogg must not spam the log
                current = new ElevenLoop();
                mc.getSoundManager().play(current);
            }
        });
    }

    public static boolean isPlaying() {
        return current != null && !current.isStopped();
    }

    private static float target() {
        var cfg = ConfigManager.get().ambience;
        Minecraft mc = Minecraft.getInstance();
        float v = cfg.musicVolume;
        LocalPlayer p = mc.player;
        if (p != null && mc.level != null) {
            if (mc.level.isDarkOutside()) v *= cfg.musicNightBoost;
            int surface = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getBlockX(), p.getBlockZ());
            if (p.getY() < surface - 8 && !mc.level.canSeeSky(p.blockPosition())) v *= cfg.musicCaveBoost;
        }
        return Math.min(1f, v);
    }

    @Override
    public void tick() {
        float t = target();
        this.volume += (t - this.volume) * 0.02f; // smooth
    }
}
