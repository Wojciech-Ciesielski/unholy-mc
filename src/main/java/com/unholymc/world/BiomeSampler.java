package com.unholymc.world;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/** Biome lookups straight from the biome source: no chunk is loaded or generated. */
public final class BiomeSampler {
    private BiomeSampler() {}

    public static Holder<Biome> biome(ServerLevel level, int x, int y, int z) {
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        return source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), level.getChunkSource().randomState().sampler());
    }

    public static boolean is(ServerLevel level, int x, int z, TagKey<Biome> tag) {
        return biome(level, x, level.getSeaLevel() + 1, z).is(tag);
    }
}
