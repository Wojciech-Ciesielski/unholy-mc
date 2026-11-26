package com.unholymc.polymer;

import com.unholymc.UnholyMC;
import com.unholymc.entity.BillboardEntity;
import com.unholymc.entity.Slenderman;
import com.unholymc.entity.slop.SlopVariant;
import com.unholymc.registry.ModEntities;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;

/**
 * How our mobs look to vanilla clients. Each one is sent as an invisible zombie scaled to the mob's hitbox (so
 * it can be hit and moves smoothly), carrying item displays as passengers that show the real look:
 * a camera-facing PNG billboard for slops and hallucinations, a six-part model for Slenderman.
 */
public final class MobVisuals {
    private static final byte INVISIBLE = 0x20;
    private static final float ZOMBIE_HEIGHT = 1.95f;

    private MobVisuals() {}

    public static void init() {
        overlay(ModEntities.AGGRESSIVE_SLOP, Billboard::new);
        overlay(ModEntities.NEUTRAL_SLOP, Billboard::new);
        overlay(ModEntities.UNHOLY_SLOP, Billboard::new);
        overlay(ModEntities.HALLUCINATION, Billboard::new);
        overlay(ModEntities.SLENDERMAN_HALLUCINATION, Billboard::new);
        overlay(ModEntities.SLENDERMAN, SlenderVisual::new);
        overlay(ModEntities.THROWN_HOLY_WATER, e -> context -> EntityTypes.SNOWBALL);
    }

    private static <T extends Entity> void overlay(EntityType<T> type, java.util.function.Function<T, PolymerEntity> constructor) {
        PolymerEntityUtils.registerOverlay(type, constructor);
    }

    static ItemStack model(Identifier id, int tint, String select) {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.ITEM_MODEL, id);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(),
                select == null ? List.of() : List.of(select), List.of(tint)));
        return stack;
    }

    /** Shared part: an invisible zombie with our hitbox, and a holder of display passengers. */
    private abstract static class Base<T extends LivingEntity> implements PolymerEntity {
        protected final T entity;
        protected final ElementHolder holder = new ElementHolder();

        Base(T entity) {
            this.entity = entity;
        }

        /** Called at the end of the subclass constructor: the passengers must exist before anyone starts tracking. */
        protected void start() {
            build();
            EntityAttachment.ofTicking(holder, entity);
        }

        @Override
        public EntityType<?> getPolymerEntityType(PacketContext context) {
            return EntityTypes.ZOMBIE;
        }

        @Override
        public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data, ServerPlayer player, boolean initial) {
            // only the plain Entity / LivingEntity / Mob values make sense for the zombie; ours would be misread
            byte flags = 0;
            boolean hadFlags = false;
            for (var v : data) {
                if (v.id() == 0 && v.value() instanceof Byte b) {
                    flags = b;
                    hadFlags = true;
                }
            }
            data.removeIf(v -> v.id() > 15 || v.id() == 0);
            if (hadFlags || initial) data.add(new SynchedEntityData.DataValue<>(0, EntityDataSerializers.BYTE, (byte) (flags | INVISIBLE)));
        }

        @Override
        public void modifyRawEntityAttributeData(List<ClientboundUpdateAttributesPacket.AttributeSnapshot> data, ServerPlayer player, boolean initial) {
            data.removeIf(s -> s.attribute().equals(Attributes.SCALE));
            data.add(new ClientboundUpdateAttributesPacket.AttributeSnapshot(Attributes.SCALE, entity.getBbHeight() / ZOMBIE_HEIGHT, List.of()));
        }

        @Override
        public void beforeEntityTrackerTick(Set<ServerPlayerConnection> listeners) {
            update();
        }

        /** Elements are positioned relative to the zombie's passenger seat, i.e. the top of the hitbox. */
        protected float seatY() {
            return entity.getBbHeight();
        }

        protected abstract void build();

        protected abstract void update();
    }

    /** Slops and hallucinations: the PNG on a vertical billboard. */
    private static final class Billboard extends Base<LivingEntity> {
        private final ItemDisplayElement image = new ItemDisplayElement();
        private SlopVariant shownVariant;
        private int tint = -1;

        Billboard(LivingEntity entity) {
            super(entity);
            start();
        }

        private BillboardEntity look() {
            return (BillboardEntity) entity;
        }

        @Override
        protected void build() {
            image.setItemDisplayContext(ItemDisplayContext.NONE);
            image.setBillboardMode(Display.BillboardConstraints.VERTICAL);
            image.setInterpolationDuration(2);
            if (look().isHallucinationLook()) image.setBrightness(new Brightness(15, 15));
            holder.addPassengerElement(image);
        }

        @Override
        protected void update() {
            BillboardEntity look = look();
            SlopVariant v = look.variant();
            int wantTint = look.isSlenderMinion() && (entity.tickCount / 4) % 12 == 0 ? 0xFF5050 : -1;
            if (v != shownVariant || wantTint != tint) {
                shownVariant = v;
                tint = wantTint;
                String name = v.name().toLowerCase(java.util.Locale.ROOT) + (look.isHallucinationLook() ? "_hallucination" : "");
                image.setItem(model(UnholyMC.id("slop/" + name), tint, null));
                image.setScale(new Vector3f(v.billboardSize));
            }
            float jitter = look.isChasing() ? 0.04f : 0f;
            var r = entity.getRandom();
            image.setTranslation(new Vector3f(jitter * (r.nextFloat() * 2 - 1),
                    -seatY() + v.billboardSize / 2f + v.floatOffset, jitter * (r.nextFloat() * 2 - 1)));
            image.startInterpolationIfDirty();
        }
    }

    /** Slenderman: six item display parts animated from the server, static frames while glitching, red when Deadly. */
    private static final class SlenderVisual extends Base<Slenderman> {
        private record Part(String name, float x, float y, float z) {}

        // pivots in blocks from the feet (entity model coordinates / 16)
        private static final Part[] PARTS = {
                new Part("head", 0, 42 / 16f, 0), new Part("body", 0, 42 / 16f, 0),
                new Part("right_arm", -5 / 16f, 41 / 16f, 0), new Part("left_arm", 5 / 16f, 41 / 16f, 0),
                new Part("right_leg", -2 / 16f, 28 / 16f, 0), new Part("left_leg", 2 / 16f, 28 / 16f, 0)};

        private final ItemDisplayElement[] elements = new ItemDisplayElement[PARTS.length];
        private String shownFrame = "";
        private int shownTint;

        SlenderVisual(Slenderman entity) {
            super(entity);
            start();
        }

        @Override
        protected void build() {
            for (int i = 0; i < PARTS.length; i++) {
                var e = new ItemDisplayElement();
                e.setItemDisplayContext(ItemDisplayContext.NONE);
                e.setInterpolationDuration(2);
                e.setViewRange(2f);
                elements[i] = e;
                holder.addPassengerElement(e);
            }
        }

        @Override
        protected void update() {
            int glitch = entity.glitchTicks();
            boolean staticFrame = glitch > 0 && (glitch % 2 == 0 || glitch <= 2);
            String frame = staticFrame ? "static" + (entity.tickCount % 4) : "";
            int tint = entity.isDeadly() ? 0xB02020 : -1;
            if (!frame.equals(shownFrame) || tint != shownTint || elements[0].getItem().isEmpty()) {
                shownFrame = frame;
                shownTint = tint;
                for (int i = 0; i < PARTS.length; i++) {
                    elements[i].setItem(model(UnholyMC.id("slenderman/" + PARTS[i].name), tint, frame.isEmpty() ? null : frame));
                }
            }

            float bodyYaw = Mth.wrapDegrees(entity.yBodyRot);
            float headYaw = Mth.wrapDegrees(entity.yHeadRot - entity.yBodyRot);
            float pos = entity.walkAnimation.position(), speed = Math.min(1f, entity.walkAnimation.speed());
            float swing = Mth.cos(pos * 0.45f) * 0.9f * speed;
            float breath = Mth.sin(entity.tickCount * 0.05f) * 0.03f * (1f - Math.min(1f, speed * 4f));
            double jx = 0, jz = 0;
            if (glitch > 0) {
                jx = (entity.getRandom().nextDouble() * 2 - 1) * 0.1;
                jz = (entity.getRandom().nextDouble() * 2 - 1) * 0.1;
            }
            float s = entity.isDeadly() ? 1.15f : 1f;
            Quaternionf body = new Quaternionf().rotateY(-bodyYaw * Mth.DEG_TO_RAD);
            for (int i = 0; i < PARTS.length; i++) {
                Part part = PARTS[i];
                Quaternionf local = switch (part.name) {
                    case "head" -> new Quaternionf().rotateY(-headYaw * Mth.DEG_TO_RAD).rotateX(entity.getXRot() * Mth.DEG_TO_RAD).rotateZ(0.15f);
                    case "body" -> new Quaternionf().rotateX(breath);
                    case "right_arm" -> new Quaternionf().rotateX(swing * 0.25f).rotateZ(0.04f);
                    case "left_arm" -> new Quaternionf().rotateX(-swing * 0.25f).rotateZ(-0.04f);
                    case "right_leg" -> new Quaternionf().rotateX(-swing);
                    default -> new Quaternionf().rotateX(swing);
                };
                Vector3f pivot = new Vector3f(part.x * s, part.y * s, part.z * s).rotate(body);
                var e = elements[i];
                e.setTranslation(new Vector3f((float) (pivot.x + jx), pivot.y - seatY(), (float) (pivot.z + jz)));
                e.setLeftRotation(new Quaternionf(body).mul(local));
                e.setScale(new Vector3f(2f * s));
                e.startInterpolationIfDirty();
            }
        }
    }
}
