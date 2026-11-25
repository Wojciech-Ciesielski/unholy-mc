package com.unholymc.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.unholymc.attachment.ModAttachments;
import com.unholymc.attachment.PlayerStats;
import com.unholymc.config.ConfigManager;
import com.unholymc.registry.ModEffects;
import com.unholymc.survival.AuraBreakdown;
import com.unholymc.survival.SanityAuras;
import com.unholymc.survival.SleepTracker;
import com.unholymc.survival.StatsHelper;
import com.unholymc.survival.SurvivalSystem;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * /thm - operator test commands (GDD chapter 7, permission level 2+).
 * Subcommands for later stages (summon, give, blood_moon, dark_corner, thermos) are added by those
 * stages through {@link #addSubcommand}.
 */
public final class ThmCommand {
    private static final List<Consumer<LiteralArgumentBuilder<CommandSourceStack>>> EXTENSIONS = new ArrayList<>();
    /** Hooks run by /thm aura clear (e.g. removing hallucinations, stage 3). */
    public static final List<Consumer<ServerPlayer>> CLEAR_HOOKS = new ArrayList<>();

    private ThmCommand() {}

    public static void addSubcommand(Consumer<LiteralArgumentBuilder<CommandSourceStack>> extension) {
        EXTENSIONS.add(extension);
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("thm")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

        // /thm set <stat> <value> [players]
        var set = Commands.literal("set");
        for (Stat stat : Stat.values()) {
            set.then(Commands.literal(stat.name)
                    .then(Commands.argument("value", IntegerArgumentType.integer(0, 20))
                            .executes(c -> setStat(c, stat, List.of(c.getSource().getPlayerOrException())))
                            .then(Commands.argument("targets", EntityArgument.players())
                                    .executes(c -> setStat(c, stat, EntityArgument.getPlayers(c, "targets"))))));
        }
        root.then(set);

        // /thm sleep_streak <get|set|reset>
        root.then(Commands.literal("sleep_streak")
                .then(Commands.literal("get")
                        .executes(c -> getStreak(c.getSource(), c.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(c -> getStreak(c.getSource(), EntityArgument.getPlayer(c, "target")))))
                .then(Commands.literal("set")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(c -> setStreak(c.getSource(), List.of(c.getSource().getPlayerOrException()), IntegerArgumentType.getInteger(c, "amount")))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(c -> setStreak(c.getSource(), EntityArgument.getPlayers(c, "targets"), IntegerArgumentType.getInteger(c, "amount"))))))
                .then(Commands.literal("reset")
                        .executes(c -> setStreak(c.getSource(), List.of(c.getSource().getPlayerOrException()), 0))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(c -> setStreak(c.getSource(), EntityArgument.getPlayers(c, "targets"), 0)))));

        // /thm aura <inspect|clear>
        root.then(Commands.literal("aura")
                .then(Commands.literal("inspect")
                        .executes(c -> inspect(c.getSource(), c.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(c -> inspect(c.getSource(), EntityArgument.getPlayer(c, "target")))))
                .then(Commands.literal("clear")
                        .executes(c -> clear(c.getSource(), List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(c -> clear(c.getSource(), EntityArgument.getPlayers(c, "targets"))))));

        // /thm reload
        root.then(Commands.literal("reload").executes(c -> {
            boolean ok = ConfigManager.load();
            c.getSource().sendSuccess(() -> Component.translatable(ok ? "commands.unholymc.reload.success" : "commands.unholymc.reload.failed"), true);
            return ok ? 1 : 0;
        }));

        for (var ext : EXTENSIONS) ext.accept(root);
        dispatcher.register(root);
    }

    private enum Stat {
        SANITY("sanity"), HUNGER("hunger"), IRRIGATION("irrigation"), HYDRATION("hydration");
        final String name;
        Stat(String name) { this.name = name; }
    }

    private static int setStat(CommandContext<CommandSourceStack> c, Stat stat, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        int value = IntegerArgumentType.getInteger(c, "value");
        for (ServerPlayer p : targets) {
            switch (stat) {
                case SANITY -> StatsHelper.setSanity(p, value);
                case HUNGER -> p.getFoodData().setFoodLevel(value);
                case IRRIGATION, HYDRATION -> StatsHelper.setHydration(p, value);
            }
        }
        c.getSource().sendSuccess(() -> Component.translatable("commands.unholymc.set.success",
                Component.translatable("stat.unholymc." + (stat == Stat.IRRIGATION ? "hydration" : stat.name)), value, targets.size()), true);
        return targets.size();
    }

    private static int getStreak(CommandSourceStack source, ServerPlayer player) {
        SleepTracker.checkStreak(player);
        int streak = ModAttachments.stats(player).sleepStreak;
        source.sendSuccess(() -> Component.translatable("commands.unholymc.sleep_streak.get", player.getDisplayName(), streak,
                ConfigManager.get().sleep.deadlyStreak), false);
        return streak;
    }

    private static int setStreak(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer p : targets) SleepTracker.setStreak(p, amount);
        source.sendSuccess(() -> Component.translatable("commands.unholymc.sleep_streak.set", amount, targets.size()), true);
        return targets.size();
    }

    private static int inspect(CommandSourceStack source, ServerPlayer player) {
        PlayerStats s = ModAttachments.stats(player);
        AuraBreakdown b = SanityAuras.compute(player);
        source.sendSuccess(() -> Component.translatable("commands.unholymc.aura.header", player.getDisplayName(),
                fmt(s.sanity), fmt(s.hydration)).withStyle(ChatFormatting.GOLD), false);
        if (!StatsHelper.isActive(player)) {
            source.sendSuccess(() -> Component.translatable("commands.unholymc.aura.inactive").withStyle(ChatFormatting.GRAY), false);
        }
        if (b.entries().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.unholymc.aura.none").withStyle(ChatFormatting.GRAY), false);
        }
        for (AuraBreakdown.Entry e : b.entries()) {
            ChatFormatting color = e.per2Min() > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
            source.sendSuccess(() -> Component.literal(" ")
                    .append(Component.translatable("aura.unholymc." + e.key()))
                    .append(Component.literal(": " + signed(e.per2Min()) + " / 2 min").withStyle(color)), false);
        }
        source.sendSuccess(() -> Component.translatable("commands.unholymc.aura.multiplier", fmt(b.negativeMultiplier())), false);
        if (b.negativeImmune()) {
            long left = s.auraImmunityUntil - player.level().getGameTime();
            source.sendSuccess(() -> Component.translatable("commands.unholymc.aura.immune", left / 20).withStyle(ChatFormatting.AQUA), false);
        }
        source.sendSuccess(() -> Component.translatable("commands.unholymc.aura.total",
                signed(b.positiveTotal()), signed(b.negativeTotal()), signed(b.total())).withStyle(ChatFormatting.YELLOW), false);
        float drain = SurvivalSystem.hydrationDrainPer2Min(player, ConfigManager.get().hydration);
        source.sendSuccess(() -> Component.translatable("commands.unholymc.aura.hydration", signed(-drain)).withStyle(ChatFormatting.AQUA), false);
        return Math.round(b.total());
    }

    private static int clear(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer p : targets) {
            p.removeEffect(ModEffects.WELLBEING);
            p.removeEffect(MobEffects.NAUSEA);
            p.removeEffect(MobEffects.WEAKNESS);
            p.removeEffect(MobEffects.MINING_FATIGUE);
            p.removeEffect(MobEffects.DARKNESS);
            p.removeEffect(MobEffects.BLINDNESS);
            for (var hook : CLEAR_HOOKS) hook.accept(p);
        }
        source.sendSuccess(() -> Component.translatable("commands.unholymc.aura.cleared", targets.size()), true);
        return targets.size();
    }

    private static String fmt(float v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String signed(float v) {
        return (v > 0 ? "+" : "") + String.format(Locale.ROOT, "%.2f", v);
    }
}
