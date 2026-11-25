package com.unholymc.item;

import com.unholymc.config.ConfigManager;
import com.unholymc.light.LightSource;
import com.unholymc.registry.ModComponents;
import com.unholymc.registry.ModComponents.FlashlightData;
import com.unholymc.registry.ModItems;
import com.unholymc.registry.ModSounds;
import com.unholymc.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Flashlight (GDD 1.3, DECISIONS #2/#3): toggled with right click, works only while held, real light in a cone
 * in front of the player (Nether Star: all around). Cores are inserted/removed in the inventory like a bundle.
 * A Nightmare Gem core shines "dark light": no visible light, but aggressive slops in the cone turn neutral.
 */
public class FlashlightItem extends Item {
    public FlashlightItem(Properties properties) {
        super(properties);
    }

    public static FlashlightData data(ItemStack stack) {
        return stack.getOrDefault(ModComponents.FLASHLIGHT, FlashlightData.EMPTY);
    }

    public static void setData(ItemStack stack, FlashlightData data) {
        stack.set(ModComponents.FLASHLIGHT, data);
        String core = data.core().map(h -> h.value() == ModItems.NIGHTMARE_GEM ? "nightmare_gem" : coreName(h.value())).orElse("");
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(data.on()),
                core.isEmpty() ? List.of() : List.of(core), List.of()));
    }

    private static String coreName(Item item) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    // ---- use: toggle ----

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            FlashlightData d = data(stack);
            boolean on = !d.on();
            setData(stack, new FlashlightData(on, d.core()));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    on ? ModSounds.ITEM_FLASHLIGHT_TOGGLE_ON : ModSounds.ITEM_FLASHLIGHT_TOGGLE_OFF, SoundSource.PLAYERS, 0.8f, 1f);
        }
        return InteractionResult.SUCCESS;
    }

    // ---- cores, bundle-like ----

    /** Flashlight on the cursor clicked onto a slot. */
    @Override
    public boolean overrideStackedOnOther(ItemStack self, Slot slot, ClickAction action, Player player) {
        FlashlightData d = data(self);
        ItemStack other = slot.getItem();
        if (action == ClickAction.PRIMARY && d.core().isEmpty() && other.is(ModTags.FLASHLIGHT_CORES)) {
            ItemStack core = slot.safeTake(1, 1, player);
            if (core.isEmpty()) return false;
            setData(self, new FlashlightData(d.on(), Optional.of(core.typeHolder())));
            playCoreSound(player, true);
            return true;
        }
        if (action == ClickAction.SECONDARY && other.isEmpty() && d.core().isPresent()) {
            ItemStack remainder = slot.safeInsert(new ItemStack(d.core().get()));
            if (remainder.isEmpty()) {
                setData(self, new FlashlightData(d.on(), Optional.empty()));
                playCoreSound(player, false);
            }
            return true;
        }
        return false;
    }

    /** An item on the cursor clicked onto the flashlight in a slot. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess carried) {
        if (!slot.allowModification(player)) return false;
        FlashlightData d = data(self);
        if (action == ClickAction.PRIMARY && d.core().isEmpty() && other.is(ModTags.FLASHLIGHT_CORES)) {
            setData(self, new FlashlightData(d.on(), Optional.of(other.typeHolder())));
            other.shrink(1);
            playCoreSound(player, true);
            return true;
        }
        if (action == ClickAction.SECONDARY && other.isEmpty() && d.core().isPresent()) {
            carried.set(new ItemStack(d.core().get()));
            setData(self, new FlashlightData(d.on(), Optional.empty()));
            playCoreSound(player, false);
            return true;
        }
        return false;
    }

    private static void playCoreSound(Player player, boolean insert) {
        player.playSound(insert ? ModSounds.ITEM_FLASHLIGHT_INSERT_CORE.value() : ModSounds.ITEM_FLASHLIGHT_REMOVE_CORE.value(), 0.8f, 1f);
        if (player.containerMenu != null) player.containerMenu.slotsChanged(player.getInventory());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        FlashlightData d = data(stack);
        builder.accept(Component.translatable(d.on() ? "item.unholymc.flashlight.on" : "item.unholymc.flashlight.off").withStyle(d.on() ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        Component core = d.core().map(h -> (Component) Component.translatable(h.value().getDescriptionId()))
                .orElse(Component.translatable("item.unholymc.flashlight.no_core"));
        builder.accept(Component.translatable("item.unholymc.flashlight.core", core).withStyle(ChatFormatting.GRAY));
        Item coreItem = d.core().map(h -> h.value()).orElse(null);
        String range = String.format(Locale.ROOT, "%.1f", range(coreItem));
        builder.accept(Component.translatable(coreItem == Items.NETHER_STAR ? "item.unholymc.flashlight.radius" : "item.unholymc.flashlight.range", range)
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    // ---- logic shared with lights / slops ----

    public static double range(Item core) {
        var cfg = ConfigManager.get().flashlight;
        if (core == null) return cfg.rangeNone;
        if (core == Items.NETHER_STAR) return cfg.radiusNetherStar;
        if (core == ModItems.NIGHTMARE_GEM) return cfg.rangeNightmareGem;
        if (core == Items.DIAMOND || core == Items.NETHERITE_INGOT) return cfg.rangeDiamondNetherite;
        return cfg.rangeLapisRedstoneEmerald;
    }

    /** The lit flashlight held in either hand, or empty. */
    public static ItemStack activeFlashlight(LivingEntity entity) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = entity.getItemInHand(hand);
            if (s.is(ModItems.FLASHLIGHT) && data(s).on()) return s;
        }
        return ItemStack.EMPTY;
    }

    public static boolean hasDarkCore(ItemStack flashlight) {
        return data(flashlight).core().map(h -> h.value() == ModItems.NIGHTMARE_GEM).orElse(false);
    }

    /** True if {@code target} is inside the dark (Nightmare Gem) cone of the player's lit flashlight. */
    public static boolean isInDarkCone(Player player, Entity target) {
        ItemStack fl = activeFlashlight(player);
        if (fl.isEmpty() || !hasDarkCore(fl)) return false;
        return isInCone(player, target.getBoundingBox().getCenter(), range(ModItems.NIGHTMARE_GEM));
    }

    public static boolean isInCone(Player player, Vec3 point, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 to = point.subtract(eye);
        double dist = to.length();
        if (dist > range || dist < 1.0e-3) return dist < 1.0e-3;
        double cos = to.normalize().dot(player.getViewVector(1f));
        return cos >= Math.cos(Math.toRadians(ConfigManager.get().flashlight.coneHalfAngle));
    }

    /** Light blocks for a lit flashlight: a few points along the view ray, or one bright point for the Nether Star. */
    public static final LightSource LIGHTS = (ServerPlayer player, Map<BlockPos, Integer> out) -> {
        ItemStack fl = activeFlashlight(player);
        if (fl.isEmpty() || hasDarkCore(fl)) return;
        var cfg = ConfigManager.get().flashlight;
        Item core = data(fl).core().map(h -> h.value()).orElse(null);
        Vec3 eye = player.getEyePosition();
        if (core == Items.NETHER_STAR) {
            LightSource.put(out, BlockPos.containing(eye), 15);
            return;
        }
        double range = range(core);
        Vec3 look = player.getViewVector(1f);
        HitResult hit = player.level().clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double max = hit.getType() == HitResult.Type.MISS ? range : Math.max(0.5, hit.getLocation().distanceTo(eye) - 0.6);
        LightSource.put(out, BlockPos.containing(eye.add(look.scale(Math.min(1.5, max)))), cfg.nearLightLevel);
        for (double d = 4.5; d < max; d += 3.5) {
            LightSource.put(out, BlockPos.containing(eye.add(look.scale(d))), cfg.beamLightLevel);
        }
        if (max > 1.5) LightSource.put(out, BlockPos.containing(eye.add(look.scale(max))), cfg.beamLightLevel);
    };
}
