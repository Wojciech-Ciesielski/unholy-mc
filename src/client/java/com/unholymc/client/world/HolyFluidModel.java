package com.unholymc.client.world;

import com.unholymc.UnholyMC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.AtlasIds;

/** Water model with the design thread's holy water textures (already coloured, so no tint). */
public final class HolyFluidModel {
    private static TextureAtlasSprite lastStill;
    private static FluidModel cached;

    private HolyFluidModel() {}

    public static FluidModel of(FluidModel water) {
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
        TextureAtlasSprite still = atlas.getSprite(UnholyMC.id("block/holy_water_still"));
        if (cached == null || still != lastStill) {
            TextureAtlasSprite flow = atlas.getSprite(UnholyMC.id("block/holy_water_flow"));
            cached = new FluidModel(water.layer(), new Material.Baked(still, true), new Material.Baked(flow, true), water.overlayMaterial(), null);
            lastStill = still;
        }
        return cached;
    }
}
