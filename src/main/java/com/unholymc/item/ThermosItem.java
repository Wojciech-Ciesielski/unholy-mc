package com.unholymc.item;

import com.unholymc.config.ConfigManager;
import com.unholymc.registry.ModComponents;
import com.unholymc.registry.ModComponents.ThermosContents;
import com.unholymc.registry.ModSounds;
import com.unholymc.survival.StatsHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Thermos (GDD 1.5, DECISIONS #26): unbreakable, one fluid at a time, filled by right-clicking the source
 * (water source, full beehive for honey, a cow for milk, a holy pond for holy water - stage 4).
 * Each sip has the same stats as the single drink.
 */
public class ThermosItem extends Item {
    public static final String WATER = "water", HOLY_WATER = "holy_water", HONEY = "honey", MILK = "milk";
    public static final List<String> FLUIDS = List.of(WATER, HOLY_WATER, HONEY, MILK);

    /** Hook for the holy pond (stage 4): is this block position a holy water source right now? */
    public static BiPredicate<Level, BlockPos> holyWaterSource = (level, pos) -> false;

    public ThermosItem(Properties properties) {
        super(properties);
    }

    public static ThermosContents contents(ItemStack stack) {
        return stack.getOrDefault(ModComponents.THERMOS, ThermosContents.EMPTY);
    }

    public static void setContents(ItemStack stack, ThermosContents c) {
        if (c.portions() <= 0) c = ThermosContents.EMPTY;
        stack.set(ModComponents.THERMOS, c);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), c.isEmpty() ? List.of() : List.of(c.fluid()), List.of()));
    }

    public static int capacity(String fluid) {
        var cfg = ConfigManager.get().thermos;
        return switch (fluid) {
            case WATER -> cfg.waterPortions;
            case HOLY_WATER -> cfg.holyWaterPortions;
            case HONEY -> cfg.honeyPortions;
            case MILK -> cfg.milkPortions;
            default -> 0;
        };
    }

    /** Fills to capacity if empty or already holding the same fluid. */
    public static boolean fill(ItemStack stack, String fluid) {
        ThermosContents c = contents(stack);
        if (!c.isEmpty() && !c.fluid().equals(fluid)) return false;
        int cap = capacity(fluid);
        if (!c.isEmpty() && c.portions() >= cap) return false;
        setContents(stack, new ThermosContents(fluid, cap));
        return true;
    }

    private static void playFill(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ITEM_THERMOS_FILL, SoundSource.PLAYERS, 1f, 1f);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = level.clip(new ClipContext(player.getEyePosition(),
                player.getEyePosition().add(player.getViewVector(1f).scale(player.blockInteractionRange())),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            String fluid = holyWaterSource.test(level, pos) ? HOLY_WATER
                    : level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos).isSource() ? WATER : null;
            if (fluid != null && fill(stack, fluid)) {
                if (!level.isClientSide()) playFill(level, player);
                return InteractionResult.SUCCESS;
            }
            // full (or holding another fluid): fall through and drink
        }
        if (contents(stack).isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide()) level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ITEM_THERMOS_OPEN, SoundSource.PLAYERS, 0.8f, 1f);
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockState state = level.getBlockState(context.getClickedPos());
        if (state.getBlock() instanceof BeehiveBlock && state.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS) {
            if (fill(context.getItemInHand(), HONEY)) {
                if (!level.isClientSide() && context.getPlayer() != null) {
                    level.setBlock(context.getClickedPos(), state.setValue(BeehiveBlock.HONEY_LEVEL, 0), 3);
                    playFill(level, context.getPlayer());
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof AbstractCow cow && !cow.isBaby()) {
            ItemStack held = player.getItemInHand(hand); // the real stack (the passed one may be a copy in creative)
            if (fill(held, MILK)) {
                if (!player.level().isClientSide()) {
                    player.level().playSound(null, cow.getX(), cow.getY(), cow.getZ(), net.minecraft.sounds.SoundEvents.COW_MILK, SoundSource.PLAYERS, 1f, 1f);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return contents(stack).isEmpty() ? 0 : ConfigManager.get().thermos.drinkTicks;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ThermosContents c = contents(stack);
        if (c.isEmpty()) return stack;
        if (entity instanceof ServerPlayer player) {
            var cfg = ConfigManager.get().thermos;
            switch (c.fluid()) {
                case WATER -> StatsHelper.addHydration(player, cfg.waterHydration);
                case HOLY_WATER -> {
                    StatsHelper.addHydration(player, cfg.holyWaterHydration);
                    StatsHelper.addSanity(player, cfg.holyWaterSanity);
                }
                case HONEY -> {
                    StatsHelper.addHydration(player, cfg.honeyHydration);
                    player.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
                }
                case MILK -> {
                    StatsHelper.addHydration(player, cfg.milkHydration);
                    player.removeAllEffects();
                }
                default -> {}
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ITEM_THERMOS_DRINK, SoundSource.PLAYERS, 0.8f, 1f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ITEM_THERMOS_CLOSE, SoundSource.PLAYERS, 0.8f, 1f);
            if (!player.hasInfiniteMaterials()) setContents(stack, new ThermosContents(c.fluid(), c.portions() - 1));
        }
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        ThermosContents c = contents(stack);
        if (c.isEmpty()) return Component.translatable("item.unholymc.thermos.empty");
        return Component.translatable("item.unholymc.thermos.filled", Component.translatable("fluid.unholymc." + c.fluid()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        ThermosContents c = contents(stack);
        if (!c.isEmpty()) {
            builder.accept(Component.translatable("item.unholymc.thermos.portions", c.portions(), capacity(c.fluid())).withStyle(ChatFormatting.GRAY));
        }
    }
}
