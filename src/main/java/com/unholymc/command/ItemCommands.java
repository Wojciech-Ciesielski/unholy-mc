package com.unholymc.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.unholymc.item.ThermosItem;
import com.unholymc.registry.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** /thm give and /thm thermos fill (GDD chapter 7). */
public final class ItemCommands {
    private static final SimpleCommandExceptionType NOT_A_THERMOS = new SimpleCommandExceptionType(Component.translatable("commands.unholymc.thermos.not_held"));
    private static final SimpleCommandExceptionType UNKNOWN_ITEM = new SimpleCommandExceptionType(Component.translatable("commands.unholymc.give.unknown"));
    private static final SimpleCommandExceptionType UNKNOWN_FLUID = new SimpleCommandExceptionType(Component.translatable("commands.unholymc.thermos.unknown"));

    private static final Map<String, Supplier<Item>> ITEMS = new LinkedHashMap<>();

    static {
        ITEMS.put("holy_water", () -> ModItems.HOLY_WATER);
        ITEMS.put("splash_holy_water", () -> ModItems.SPLASH_HOLY_WATER);
        ITEMS.put("crucifix", () -> ModItems.CRUCIFIX);
        ITEMS.put("flashlight", () -> ModItems.FLASHLIGHT);
        ITEMS.put("unholy_sword", () -> ModItems.UNHOLY_SWORD);
        ITEMS.put("thermos", () -> ModItems.THERMOS);
        ITEMS.put("nightmare_gem", () -> ModItems.NIGHTMARE_GEM);
    }

    private ItemCommands() {}

    public static void init() {
        ThmCommand.addSubcommand(root -> {
            root.then(Commands.literal("give")
                    .then(Commands.argument("targets", EntityArgument.players())
                            .then(Commands.argument("item", StringArgumentType.word())
                                    .suggests((c, b) -> SharedSuggestionProvider.suggest(ITEMS.keySet(), b))
                                    .executes(c -> give(c.getSource(), EntityArgument.getPlayers(c, "targets"), StringArgumentType.getString(c, "item"), 1))
                                    .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                            .executes(c -> give(c.getSource(), EntityArgument.getPlayers(c, "targets"),
                                                    StringArgumentType.getString(c, "item"), IntegerArgumentType.getInteger(c, "count")))))));
            root.then(Commands.literal("thermos")
                    .then(Commands.literal("fill")
                            .then(Commands.argument("fluid", StringArgumentType.word())
                                    .suggests((c, b) -> SharedSuggestionProvider.suggest(ThermosItem.FLUIDS, b))
                                    .executes(c -> fillThermos(c.getSource(), StringArgumentType.getString(c, "fluid"))))));
        });
    }

    private static int give(CommandSourceStack source, java.util.Collection<ServerPlayer> targets, String name, int count) throws CommandSyntaxException {
        Supplier<Item> item = ITEMS.get(name);
        if (item == null) throw UNKNOWN_ITEM.create();
        for (ServerPlayer p : targets) {
            int left = count;
            while (left > 0) {
                ItemStack stack = new ItemStack(item.get(), Math.min(left, item.get().getDefaultMaxStackSize()));
                left -= stack.getCount();
                if (!p.getInventory().add(stack)) p.drop(stack, false, false);
            }
        }
        source.sendSuccess(() -> Component.translatable("commands.unholymc.give.success", count,
                Component.translatable(item.get().getDescriptionId()), targets.size()), true);
        return targets.size();
    }

    private static int fillThermos(CommandSourceStack source, String fluid) throws CommandSyntaxException {
        if (!ThermosItem.FLUIDS.contains(fluid)) throw UNKNOWN_FLUID.create();
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModItems.THERMOS)) throw NOT_A_THERMOS.create();
        ThermosItem.setContents(stack, new com.unholymc.registry.ModComponents.ThermosContents(fluid, ThermosItem.capacity(fluid)));
        source.sendSuccess(() -> Component.translatable("commands.unholymc.thermos.filled", Component.translatable("fluid.unholymc." + fluid)), true);
        return 1;
    }
}
