package com.unholymc.survival;

import com.unholymc.attachment.ModAttachments;
import com.unholymc.attachment.PlayerStats;
import com.unholymc.config.ConfigManager;
import com.unholymc.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** One-off sanity / hydration changes from eating and drinking (GDD 5.1, 5.2). */
public final class ConsumptionHandler {
    private ConsumptionHandler() {}

    /** Called (server side) right before a consumable item takes effect. */
    public static void onConsumed(ServerPlayer player, ItemStack stack) {
        if (!StatsHelper.isActive(player)) return;
        var cfg = ConfigManager.get().consumption;

        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            StatsHelper.setSanity(player, cfg.enchantedGoldenAppleSanity);
            PlayerStats s = ModAttachments.stats(player);
            s.auraImmunityUntil = player.level().getGameTime() + cfg.enchantedGoldenAppleImmunityTicks;
            return;
        }

        if (stack.is(Items.POTION)) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.is(Potions.WATER)) {
                StatsHelper.addHydration(player, cfg.waterBottleHydration);
            }
        } else if (stack.is(Items.HONEY_BOTTLE)) {
            StatsHelper.addHydration(player, cfg.honeyBottleHydration);
        }

        if (stack.is(ModTags.SWEET_FOOD)) {
            StatsHelper.addSanity(player, cfg.sweetFoodSanity);
        } else if (stack.is(ModTags.COOKED_FOOD)) {
            StatsHelper.addSanity(player, cfg.cookedFoodSanity);
        } else if (stack.is(ModTags.RAW_FOOD)) {
            StatsHelper.addSanity(player, cfg.rawFoodSanity); // ignored during golden apple immunity
        }
    }

    public static void onCakeSlice(ServerPlayer player) {
        StatsHelper.addSanity(player, ConfigManager.get().consumption.cakeSliceSanity);
    }

    /** DECISIONS #27: right-click a water source with an empty hand to drink (like a water bottle). */
    public static void tryDrinkFromSource(ServerPlayer player) {
        if (!StatsHelper.isActive(player) || !player.getMainHandItem().isEmpty()) return;
        var cfg = ConfigManager.get();
        PlayerStats s = ModAttachments.stats(player);
        long now = player.level().getGameTime();
        if (now - s.lastSourceSip < cfg.consumption.sourceSipCooldownTicks) return;
        if (s.hydration >= cfg.hydration.max) return;

        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getViewVector(1f).scale(cfg.consumption.sourceSipReach));
        BlockHitResult hit = player.level().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        FluidState fluid = player.level().getFluidState(pos);
        if (!fluid.is(FluidTags.WATER) || !fluid.isSource()) return;

        s.lastSourceSip = now;
        StatsHelper.addHydration(player, cfg.consumption.sourceSipHydration);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6f, 0.9f + player.getRandom().nextFloat() * 0.2f);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
    }
}
