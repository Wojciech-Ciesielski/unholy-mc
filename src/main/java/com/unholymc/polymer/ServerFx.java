package com.unholymc.polymer;

import com.unholymc.UnholyMC;
import com.unholymc.attachment.HudState;
import com.unholymc.attachment.ModAttachments;
import com.unholymc.config.ConfigManager;
import com.unholymc.entity.slop.SlopVariant;
import com.unholymc.registry.ModSounds;
import com.unholymc.world.BloodMoon;
import com.unholymc.world.HolyPonds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Everything the client mod used to draw, done from the server for vanilla clients:
 * screamer / Spooky Jumpscare as full-screen title glyphs, dehydration blur and blinks as short vanilla effects,
 * the Blood Moon's red screen tint (a per-player world border warning), holy pond particles, the disc 11 loop
 * and extra cave sounds, and the no-sprint rule while dehydrated.
 */
public final class ServerFx {
    private static final Style JUMPSCARE_FONT = Style.EMPTY.withFont(new FontDescription.Resource(UnholyMC.id("jumpscare"))).withShadowColor(0);
    private static final char SLOP_GLYPH = '', SPOOKY_FACE = '';
    private static final net.minecraft.resources.Identifier NO_SPRINT = UnholyMC.id("dehydrated_no_sprint");

    private static final Map<UUID, Long> SPOOKY_START = new HashMap<>();
    private static final Map<UUID, Long> NEXT_MUSIC = new HashMap<>();
    private static final Set<UUID> RED_TINT = new HashSet<>();

    private ServerFx() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(ServerFx::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> {
            UUID id = h.player.getUUID();
            SPOOKY_START.remove(id);
            NEXT_MUSIC.remove(id);
            RED_TINT.remove(id);
        });
    }

    // --- jumpscares ---

    /** The 14-tick screamer: a slop's face jumps at the screen with a scream. */
    public static void screamer(ServerPlayer p, SlopVariant variant) {
        int t = Math.max(2, ConfigManager.get().jumpscares.screamerTicks);
        p.connection.send(new ClientboundSetTitlesAnimationPacket(0, t / 2, t - t / 2));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(String.valueOf((char) (SLOP_GLYPH + variant.ordinal()))).withStyle(JUMPSCARE_FONT)));
        sound(p, ModSounds.JUMPSCARE_SCREAMER, 1f);
    }

    /** The Spooky Jumpscare (~5.5 s); the death itself is scheduled by Jumpscares. */
    public static void spooky(ServerPlayer p) {
        SPOOKY_START.put(p.getUUID(), p.level().getGameTime());
        p.connection.send(new ClientboundStopSoundPacket(null, null)); // the world goes quiet
        p.connection.send(new ClientboundClearTitlesPacket(true));
    }

    private static void spookyTick(ServerPlayer p, long t) {
        if (t == 20) {
            sound(p, ModSounds.JUMPSCARE_SPOOKY_BUILDUP, 1f);
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 70, 0, true, false, false));
        }
        if (t == 32 || t == 47 || t == 62) { // the red text flashes three times
            p.connection.send(new ClientboundSetTitlesAnimationPacket(0, 6, 0));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("jumpscare.unholymc.spooky_text").withColor(0xC01020)));
        }
        if (t == 80) {
            sound(p, ModSounds.JUMPSCARE_SPOOKY, 1f);
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, true, false, false));
            p.connection.send(new ClientboundSetTitlesAnimationPacket(0, 40, 0));
        }
        if (t >= 80 && t < 110 && t % 2 == 0) { // shaking face
            String pad = t % 4 == 0 ? "" : " ";
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(pad + SPOOKY_FACE).withStyle(JUMPSCARE_FONT)));
        }
    }

    private static void sound(ServerPlayer p, Holder<SoundEvent> sound, float volume) {
        p.connection.send(new ClientboundSoundPacket(sound, SoundSource.MASTER, p.getX(), p.getEyeY(), p.getZ(), volume, 1f, p.getRandom().nextLong()));
    }

    // --- per tick ---

    private static void tick(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            long now = p.level().getGameTime();
            Long start = SPOOKY_START.get(p.getUUID());
            if (start != null) {
                long t = now - start;
                if (t > 160 || !p.isAlive()) SPOOKY_START.remove(p.getUUID());
                else spookyTick(p, t);
            }
            HudState hud = ModAttachments.hud(p);
            dehydration(p, hud, now);
            noSprint(p, hud);
            bloodMoonTint(p);
            if ((now + p.getId()) % 4 == 0) pondParticles(p);
            music(p, now);
            if ((now + p.getId()) % 20 == 0) caveSounds(p);
        }
    }

    /** Dehydration screen effects (DECISIONS #35): below the low threshold the screen blinks and darkens. */
    private static void dehydration(ServerPlayer p, HudState hud, long now) {
        if (!hud.enabled() || hud.hydrationStage() == 0) return;
        long t = now + p.getId();
        if (t % 80 == 0) quiet(p, MobEffects.BLINDNESS, 10);
        if (t % 60 == 0) quiet(p, MobEffects.DARKNESS, 70);
    }

    private static void quiet(ServerPlayer p, Holder<net.minecraft.world.effect.MobEffect> effect, int ticks) {
        p.addEffect(new MobEffectInstance(effect, ticks, 0, true, false, false));
    }

    /** Hydration 9 and below: no sprinting. The client predicts sprint itself, so its speed bonus is cancelled. */
    private static void noSprint(ServerPlayer p, HudState hud) {
        var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        boolean block = hud.enabled() && hud.hydrationStage() >= 1 && p.isSprinting();
        if (block && !speed.hasModifier(NO_SPRINT)) {
            // vanilla sprint: +30% total; 1.3 * (1 - 0.3/1.3) = 1
            speed.addTransientModifier(new AttributeModifier(NO_SPRINT, -0.3 / 1.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!block && speed.hasModifier(NO_SPRINT)) {
            speed.removeModifier(NO_SPRINT);
        }
    }

    /** Blood Moon: the screen edges glow red (the client's world border warning tint, sent only to this player). */
    private static void bloodMoonTint(ServerPlayer p) {
        boolean want = p.level().dimension() == Level.OVERWORLD && BloodMoon.isActive(p.level());
        boolean has = RED_TINT.contains(p.getUUID());
        if (want == has && p.tickCount % 100 != 0) return;
        WorldBorder real = p.level().getWorldBorder();
        WorldBorder fake = new WorldBorder();
        fake.setWarningBlocks(real.getWarningBlocks());
        if (want) {
            fake.setWarningBlocks(Mth.ceil(Math.max(real.getSize(), 1_000_000)));
            RED_TINT.add(p.getUUID());
        } else {
            RED_TINT.remove(p.getUUID());
        }
        p.connection.send(new ClientboundSetBorderWarningDistancePacket(fake));
    }

    /** Holy ponds always show a few particles; on a holy night they glow (client fluid tint is not possible). */
    private static void pondParticles(ServerPlayer p) {
        ServerLevel level = p.level();
        boolean holy = BloodMoon.isHolyNight(level);
        var random = p.getRandom();
        for (HolyPonds.Pond pond : HolyPonds.pondsNear(level, p.blockPosition(), 3)) {
            if (pond.center().distSqr(p.blockPosition()) > 48 * 48) continue;
            int dx, dz;
            if (pond.cells().isEmpty()) {
                dx = random.nextInt(pond.radius() * 2 + 1) - pond.radius();
                dz = random.nextInt(pond.radius() * 2 + 1) - pond.radius();
            } else {
                int cell = pond.cells().get(random.nextInt(pond.cells().size()));
                dx = (cell >> 8) - 128;
                dz = (cell & 0xFF) - 128;
            }
            double x = pond.center().getX() + dx + random.nextDouble();
            double z = pond.center().getZ() + dz + random.nextDouble();
            double y = pond.center().getY() + 1.05;
            if (holy) {
                level.sendParticles(p, random.nextBoolean() ? ParticleTypes.END_ROD : ParticleTypes.GLOW, false, false, x, y, z, 1, 0, 0.1, 0, 0.01);
            } else {
                level.sendParticles(p, ParticleTypes.WHITE_ASH, false, false, x, y + 0.2, z, 1, 0, 0, 0, 0.01);
                if (random.nextInt(4) == 0) level.sendParticles(p, ParticleTypes.BUBBLE_POP, false, false, x, y - 0.1, z, 1, 0, 0, 0, 0.04);
            }
        }
    }

    /** GDD 5.4: the slowed, reversed disc 11 loops all the time (vanilla music is muted by our resource pack). */
    private static void music(ServerPlayer p, long now) {
        var cfg = ConfigManager.get().ambience;
        if (cfg.musicVolume <= 0) return;
        long next = NEXT_MUSIC.getOrDefault(p.getUUID(), now + 40); // a moment after joining
        if (!NEXT_MUSIC.containsKey(p.getUUID())) NEXT_MUSIC.put(p.getUUID(), next);
        if (now < next) return;
        float v = cfg.musicVolume;
        if (p.level().isDarkOutside()) v *= cfg.musicNightBoost;
        if (underground(p)) v *= cfg.musicCaveBoost;
        p.connection.send(new ClientboundSoundEntityPacket(ModSounds.MUSIC_ELEVEN_REVERSED, SoundSource.MUSIC, p, Math.min(1f, v), 1f, p.getRandom().nextLong()));
        NEXT_MUSIC.put(p.getUUID(), now + Math.max(200, cfg.musicLoopTicks));
    }

    /** Cave "mood" sounds come more often than in vanilla (GDD 5.4). */
    private static void caveSounds(ServerPlayer p) {
        float chance = ConfigManager.get().ambience.caveSoundChancePerSecond;
        if (chance <= 0 || !underground(p) || p.getRandom().nextFloat() >= chance) return;
        if (p.level().getMaxLocalRawBrightness(p.blockPosition()) > 7) return;
        var r = p.getRandom();
        BlockPos at = p.blockPosition().offset(r.nextInt(17) - 8, r.nextInt(9) - 4, r.nextInt(17) - 8);
        p.connection.send(new ClientboundSoundPacket(SoundEvents.AMBIENT_CAVE, SoundSource.AMBIENT, at.getX(), at.getY(), at.getZ(), 0.9f, 0.8f + r.nextFloat() * 0.4f, r.nextLong()));
    }

    private static boolean underground(ServerPlayer p) {
        int surface = p.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getBlockX(), p.getBlockZ());
        return p.getY() < surface - 8 && !p.level().canSeeSky(p.blockPosition());
    }
}
