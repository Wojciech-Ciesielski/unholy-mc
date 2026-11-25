package com.unholymc.command;

import com.unholymc.world.BloodMoon;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** /thm blood_moon <start|stop|check> (GDD chapter 7). */
public final class WorldCommands {
    private WorldCommands() {}

    public static void init() {
        ThmCommand.addSubcommand(root -> root.then(Commands.literal("blood_moon")
                .then(Commands.literal("start").executes(c -> set(c.getSource(), true)))
                .then(Commands.literal("stop").executes(c -> set(c.getSource(), false)))
                .then(Commands.literal("check").executes(c -> check(c.getSource())))));
    }

    private static int set(CommandSourceStack source, boolean active) {
        ServerLevel overworld = source.getServer().overworld();
        BloodMoon.set(overworld, active);
        source.sendSuccess(() -> Component.translatable(active ? "commands.unholymc.blood_moon.started" : "commands.unholymc.blood_moon.stopped"), true);
        return 1;
    }

    private static int check(CommandSourceStack source) {
        ServerLevel overworld = source.getServer().overworld();
        boolean active = BloodMoon.isActive(overworld);
        int days = BloodMoon.daysUntilFullMoon(overworld);
        source.sendSuccess(() -> Component.translatable("commands.unholymc.blood_moon.check",
                Component.translatable(active ? "commands.unholymc.blood_moon.active" : "commands.unholymc.blood_moon.inactive"),
                days, BloodMoon.isHolyNight(overworld) ? Component.translatable("commands.unholymc.blood_moon.holy") : Component.empty()), false);
        return days;
    }
}
