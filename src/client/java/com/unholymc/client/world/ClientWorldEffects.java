package com.unholymc.client.world;

import com.unholymc.config.ConfigManager;
import com.unholymc.world.BloodMoon;
import com.unholymc.world.EnvironmentLayers;
import com.unholymc.world.HolyPonds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * Client-side world presentation: smoothed Blood Moon look (200 ticks in/out), the holy pond cache used by
 * the fluid renderer (thread-safe snapshot), re-meshing ponds when they turn holy, and pond particles.
 */
public final class ClientWorldEffects {
    private static float bloodFactor;
    private static volatile List<HolyPonds.Pond> nearbyPonds = List.of();
    private static volatile boolean holyNight;
    private static final RandomSource RANDOM = RandomSource.create();

    private ClientWorldEffects() {}

    public static void init() {
        var serverSide = EnvironmentLayers.bloodMoonFactor;
        EnvironmentLayers.bloodMoonFactor = level -> level.isClientSide() ? bloodFactor : serverSide.applyAsDouble(level);
        ClientTickEvents.END_CLIENT_TICK.register(ClientWorldEffects::tick);
    }

    public static boolean bloodMoonVisible() {
        return bloodFactor > 0.5f;
    }

    /** Used by the fluid renderer on chunk-meshing threads. */
    public static boolean isHolyPondWater(BlockPos pos) {
        if (!holyNight) return false;
        for (HolyPonds.Pond p : nearbyPonds) if (p.contains(pos)) return true;
        return false;
    }

    private static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            bloodFactor = 0;
            nearbyPonds = List.of();
            holyNight = false;
            return;
        }
        float step = 1f / Math.max(1, ConfigManager.get().bloodMoon.transitionTicks);
        float target = BloodMoon.isActive(level) ? 1f : 0f;
        bloodFactor = target > bloodFactor ? Math.min(target, bloodFactor + step) : Math.max(target, bloodFactor - step);

        if (mc.player.tickCount % 20 == 0) {
            nearbyPonds = List.copyOf(HolyPonds.pondsNear(level, mc.player.blockPosition(), 8));
        }
        boolean holy = BloodMoon.isHolyNight(level);
        if (holy != holyNight) {
            holyNight = holy;
            for (HolyPonds.Pond p : nearbyPonds) {
                BlockPos c = p.center();
                level.setSectionDirtyWithNeighbors(SectionPos.blockToSectionCoord(c.getX()), SectionPos.blockToSectionCoord(c.getY()),
                        SectionPos.blockToSectionCoord(c.getZ()));
            }
        }

        if (mc.player.tickCount % 4 == 0) {
            for (HolyPonds.Pond p : nearbyPonds) {
                if (p.center().distSqr(mc.player.blockPosition()) > 48 * 48) continue;
                int dx, dz;
                if (p.cells().isEmpty()) {
                    dx = RANDOM.nextInt(p.radius() * 2 + 1) - p.radius();
                    dz = RANDOM.nextInt(p.radius() * 2 + 1) - p.radius();
                } else {
                    int cell = p.cells().get(RANDOM.nextInt(p.cells().size()));
                    dx = (cell >> 8) - 128;
                    dz = (cell & 0xFF) - 128;
                }
                double x = p.center().getX() + dx + RANDOM.nextDouble();
                double z = p.center().getZ() + dz + RANDOM.nextDouble();
                double y = p.center().getY() + 1.05;
                if (holy) {
                    level.addParticle(RANDOM.nextBoolean() ? ParticleTypes.END_ROD : ParticleTypes.GLOW, x, y, z, 0, 0.02, 0);
                } else {
                    // a small, constant tell that there is a pond here (day and night)
                    level.addParticle(ParticleTypes.WHITE_ASH, x, y + 0.2, z, 0, 0.01, 0);
                    if (RANDOM.nextInt(4) == 0) level.addParticle(ParticleTypes.BUBBLE_POP, x, y - 0.1, z, 0, 0.04, 0);
                }
            }
        }
    }
}
