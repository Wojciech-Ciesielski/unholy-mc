package com.unholymc.client.render;

import com.unholymc.UnholyMC;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Slenderman's 3D model, geometry from docs/ASSETS.md §1.3 (64x64 box UV, ~3.25 blocks tall). */
public class SlendermanModel extends EntityModel<SlendermanRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(UnholyMC.id("slenderman"), "main");

    private final ModelPart head, body, rightArm, leftArm, rightLeg, leftLeg;

    public SlendermanModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -10, -4, 8, 10, 8), PartPose.offset(0, -18, 0));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4, 0, -2, 8, 14, 4), PartPose.offset(0, -18, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 18).addBox(-1, -1, -1, 2, 30, 2), PartPose.offset(-5, -17, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(8, 18).addBox(-1, -1, -1, 2, 30, 2), PartPose.offset(5, -17, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 18).addBox(-1, 0, -1, 2, 28, 2), PartPose.offset(-2, -4, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 18).addBox(-1, 0, -1, 2, 28, 2), PartPose.offset(2, -4, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(SlendermanRenderState state) {
        super.setupAnim(state);
        float pos = state.walkAnimationPos, speed = state.walkAnimationSpeed;
        // slow walk, barely swinging arms
        float swing = Mth.cos(pos * 0.45f) * 0.9f * speed;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        rightArm.xRot = -swing * 0.25f;
        leftArm.xRot = swing * 0.25f;
        rightArm.zRot = 0.04f;
        leftArm.zRot = -0.04f;
        // tilted head looking at you
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        head.zRot = 0.15f;
        // slow "breathing" of the torso when standing
        float breath = Mth.sin(state.ageInTicks * 0.05f) * 0.03f * (1f - Math.min(1f, speed * 4f));
        body.xRot = breath;
    }
}
