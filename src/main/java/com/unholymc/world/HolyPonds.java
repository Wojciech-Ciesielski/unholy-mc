package com.unholymc.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.unholymc.UnholyMC;
import com.unholymc.config.ConfigManager;
import com.unholymc.item.ThermosItem;
import com.unholymc.light.DynamicLights;
import com.unholymc.light.LightSource;
import com.unholymc.survival.SanityAuras;
import com.unholymc.survival.StatsHelper;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Small holy-water ponds in deserts and savannas (GDD 1.1, DECISIONS #16). They are ordinary water with a
 * few particles; on a full moon night without a Blood Moon the water is holy: light blue and glowing, holy
 * water can be bottled, there is a +20 sanity / 2 min aura within 10 blocks and wading gives +1 / s.
 * Pond positions live in a chunk attachment that is synced to clients (for the look and the particles).
 */
public final class HolyPonds {
    public record Pond(BlockPos center, int radius) {
        public static final Codec<Pond> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("center").forGetter(Pond::center),
                Codec.INT.fieldOf("radius").forGetter(Pond::radius)
        ).apply(i, Pond::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Pond> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Pond::center, ByteBufCodecs.VAR_INT, Pond::radius, Pond::new);

        /** Horizontal ellipse test plus the 2-block depth of the pond. */
        public boolean contains(BlockPos pos) {
            int dx = pos.getX() - center.getX(), dz = pos.getZ() - center.getZ();
            return dx * dx + dz * dz <= radius * radius + 0.5 && pos.getY() <= center.getY() + 1 && pos.getY() >= center.getY() - 2;
        }
    }

    public static final AttachmentType<List<Pond>> PONDS = AttachmentRegistry.create(UnholyMC.id("holy_ponds"),
            b -> b.persistent(Pond.CODEC.listOf())
                    .syncWith(Pond.STREAM_CODEC.apply(ByteBufCodecs.list()), AttachmentSyncPredicate.all()));

    private HolyPonds() {}

    public static void init() {
        ThermosItem.holyWaterSource = HolyPonds::isHolyWater;
        SanityAuras.register((player, out) -> {
            if (!BloodMoon.isHolyNight(player.level())) return;
            double r = ConfigManager.get().holyPonds.auraRadius;
            for (Pond p : pondsNear(player.level(), player.blockPosition(), 1)) {
                if (p.center().distSqr(player.blockPosition()) <= r * r) {
                    out.add("holy_pond", ConfigManager.get().holyPonds.auraPer2Min);
                    return;
                }
            }
        });
        DynamicLights.register((player, out) -> {
            if (!BloodMoon.isHolyNight(player.level())) return;
            double r = ConfigManager.get().holyPonds.lightRange;
            for (Pond p : pondsNear(player.level(), player.blockPosition(), (int) Math.ceil(r / 16))) {
                if (p.center().distSqr(player.blockPosition()) <= r * r) {
                    LightSource.put(out, p.center(), ConfigManager.get().holyPonds.glowLightLevel);
                }
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            float perTick = ConfigManager.get().holyPonds.wadingPerSecond / 20f;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.isInWater() && StatsHelper.isActive(p) && isHolyWater(p.level(), p.blockPosition())) {
                    StatsHelper.addSanity(p, perTick);
                }
            }
        });
    }

    /** Ponds stored in the chunks within {@code chunkRadius} of {@code pos}; only already-loaded chunks. */
    public static List<Pond> pondsNear(Level level, BlockPos pos, int chunkRadius) {
        List<Pond> out = new ArrayList<>();
        ChunkPos c = ChunkPos.containing(pos);
        for (int x = c.x() - chunkRadius; x <= c.x() + chunkRadius; x++) {
            for (int z = c.z() - chunkRadius; z <= c.z() + chunkRadius; z++) {
                ChunkAccess chunk = level.getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                List<Pond> ponds = chunk.getAttached(PONDS);
                if (ponds != null) out.addAll(ponds);
            }
        }
        return out;
    }

    public static @Nullable Pond pondAt(Level level, BlockPos pos) {
        for (Pond p : pondsNear(level, pos, 1)) {
            if (p.contains(pos)) return p;
        }
        return null;
    }

    public static boolean isHolyWater(Level level, BlockPos pos) {
        return BloodMoon.isHolyNight(level) && level.getFluidState(pos).is(FluidTags.WATER) && pondAt(level, pos) != null;
    }

    /** Records a pond in the chunk that holds its centre (called during world generation). */
    public static void record(ChunkAccess chunk, Pond pond) {
        List<Pond> list = new ArrayList<>(chunk.getAttachedOrElse(PONDS, List.of()));
        list.add(pond);
        chunk.setAttached(PONDS, List.copyOf(list));
    }
}
