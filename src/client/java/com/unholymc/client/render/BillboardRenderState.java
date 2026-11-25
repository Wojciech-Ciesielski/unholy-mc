package com.unholymc.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

public class BillboardRenderState extends EntityRenderState {
    public Identifier texture;
    public float size = 1f;
    public float floatOffset;
    public boolean translucent;
    public int alpha = 255;
    public boolean hurt;
    public float deathProgress;
    public float jitterX, jitterZ;
}
