package com.unholymc.entity.slop;

import com.unholymc.UnholyMC;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

import java.util.Arrays;
import java.util.List;

/** Billboard variants (docs/ASSETS.md §1): texture, billboard size and hitbox. */
public enum SlopVariant {
    SHADOW_WATCHER(SlopKind.AGGRESSIVE, 2.2f, 0.7f, 2.0f, 0f, false),
    LOVE_NOTE(SlopKind.AGGRESSIVE, 1.8f, 0.9f, 1.6f, 0f, false),
    WIG_GUY(SlopKind.AGGRESSIVE, 1.9f, 0.9f, 1.8f, 0f, false),
    SHEET_GHOST(SlopKind.NEUTRAL, 1.4f, 0.7f, 1.2f, 0.3f, true),
    CIGAR_CAT(SlopKind.NEUTRAL, 1.6f, 0.9f, 1.4f, 0f, false),
    HOLLOW_SCREAMER(SlopKind.UNHOLY, 2.6f, 0.8f, 2.4f, 0f, false),
    LOCUST(SlopKind.UNHOLY, 2.8f, 0.7f, 2.6f, 0f, false);

    public final SlopKind kind;
    public final float billboardSize, width, height, floatOffset;
    public final boolean softAlpha;
    public final Identifier texture, hallucinationTexture;

    SlopVariant(SlopKind kind, float billboardSize, float width, float height, float floatOffset, boolean softAlpha) {
        this.kind = kind;
        this.billboardSize = billboardSize;
        this.width = width;
        this.height = height;
        this.floatOffset = floatOffset;
        this.softAlpha = softAlpha;
        String id = name().toLowerCase(java.util.Locale.ROOT);
        this.texture = UnholyMC.id("textures/entity/slop/" + id + ".png");
        this.hallucinationTexture = UnholyMC.id("textures/entity/slop/" + id + "_hallucination.png");
    }

    /** Billboard / hitbox sizes as configured (slops.sizeMultiplier, DECISIONS: slops are a bit bigger than the design spec). */
    public float scaledBillboard() {
        return billboardSize * com.unholymc.config.ConfigManager.get().slops.sizeMultiplier;
    }

    public float scaledWidth() {
        return width * com.unholymc.config.ConfigManager.get().slops.sizeMultiplier;
    }

    public float scaledHeight() {
        return height * com.unholymc.config.ConfigManager.get().slops.sizeMultiplier;
    }

    public static SlopVariant byId(int id) {
        SlopVariant[] v = values();
        return v[Math.floorMod(id, v.length)];
    }

    public static List<SlopVariant> ofKind(SlopKind kind) {
        return Arrays.stream(values()).filter(v -> v.kind == kind).toList();
    }

    public static SlopVariant random(SlopKind kind, RandomSource random) {
        List<SlopVariant> list = ofKind(kind);
        return list.get(random.nextInt(list.size()));
    }
}
