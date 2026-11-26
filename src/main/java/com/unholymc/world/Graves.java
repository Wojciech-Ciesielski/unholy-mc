package com.unholymc.world;

import com.mojang.serialization.Codec;
import com.unholymc.UnholyMC;
import com.unholymc.entity.slop.AbstractSlop;
import com.unholymc.horror.HallucinationSpawner;
import com.unholymc.horror.Jumpscares;
import com.unholymc.registry.ModEntities;
import com.unholymc.survival.StatsHelper;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Lonely graves (DECISIONS #42): a mound with a cross and a skull, a barrel with loot buried under it. Digging the
 * barrel up and opening it for the first time wakes something: a jolt of fear, darkness and slops from the dark.
 */
public final class Graves {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, UnholyMC.id("chests/grave"));
    public static final AttachmentType<List<BlockPos>> GRAVES = AttachmentRegistry.create(UnholyMC.id("graves"),
            b -> b.persistent(BlockPos.CODEC.listOf()));

    private Graves() {}

    public static void init() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) return InteractionResult.PASS;
            if (!level.getBlockState(hit.getBlockPos()).is(Blocks.BARREL)) return InteractionResult.PASS;
            if (level.getBlockEntity(hit.getBlockPos()) instanceof RandomizableContainerBlockEntity c && LOOT.equals(c.getLootTable())) {
                wake(sl, sp, hit.getBlockPos());
            }
            return InteractionResult.PASS;
        });
    }

    /** First opening of a grave barrel (its loot table is still unspent). */
    private static void wake(ServerLevel level, ServerPlayer p, BlockPos at) {
        level.sendParticles(ParticleTypes.SOUL, at.getX() + 0.5, at.getY() + 1.2, at.getZ() + 0.5, 30, 0.6, 0.6, 0.6, 0.04);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 20, 0.5, 0.4, 0.5, 0.02);
        if (!StatsHelper.isActive(p)) return;
        Jumpscares.slopScare(p);
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, true, false, false));
        for (int i = 0; i < 2; i++) {
            BlockPos spot = HallucinationSpawner.findSpot(level, p, 8, 16);
            if (spot == null) continue;
            Mob mob = ModEntities.AGGRESSIVE_SLOP.create(level, EntitySpawnReason.EVENT);
            if (mob == null) continue;
            mob.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), EntitySpawnReason.EVENT, null);
            if (mob instanceof AbstractSlop) mob.setTarget(p);
            level.addFreshEntity(mob);
        }
    }

    /** Records a grave in the chunk that holds it (called during world generation). */
    public static void record(ChunkAccess chunk, BlockPos pos) {
        List<BlockPos> list = new ArrayList<>(chunk.getAttachedOrElse(GRAVES, List.of()));
        list.add(pos.immutable());
        chunk.setAttached(GRAVES, List.copyOf(list));
    }

    /** Nearest grave among the loaded chunks around {@code from}. */
    public static @Nullable BlockPos nearest(Level level, BlockPos from, int chunkRadius) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        ChunkPos c = ChunkPos.containing(from);
        for (int x = c.x() - chunkRadius; x <= c.x() + chunkRadius; x++) {
            for (int z = c.z() - chunkRadius; z <= c.z() + chunkRadius; z++) {
                ChunkAccess chunk = level.getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (BlockPos g : chunk.getAttachedOrElse(GRAVES, List.<BlockPos>of())) {
                    double d = g.distSqr(from);
                    if (d < bestD) {
                        bestD = d;
                        best = g;
                    }
                }
            }
        }
        return best;
    }
}
