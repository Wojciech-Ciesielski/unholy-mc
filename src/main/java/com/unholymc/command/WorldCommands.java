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
        ThmCommand.addSubcommand(root -> root.then(Commands.literal("tp")
                .then(Commands.literal("dark_corner").executes(c -> tpTo(c.getSource(), "dark_corner")))
                .then(Commands.literal("slender_forest").executes(c -> tpTo(c.getSource(), "slender_forest")))
                .then(Commands.literal("holy_pond").executes(c -> tpTo(c.getSource(), "holy_pond")))
                .then(Commands.literal("grave").executes(c -> tpTo(c.getSource(), "grave")))));
        ThmCommand.addSubcommand(root -> root.then(Commands.literal("dark_corner")
                .then(Commands.literal("spawn").executes(c -> spawnCorner(c.getSource())))
                .then(Commands.literal("toggle_day_cycle").executes(c -> toggleCorner(c.getSource())))
                .then(Commands.literal("locate").executes(c -> locate(c.getSource())))));
    }

    private static int set(CommandSourceStack source, boolean active) {
        ServerLevel overworld = source.getServer().overworld();
        if (active && !com.unholymc.world.Night.isNight(overworld)) {
            // a Blood Moon only lives at night: "start" by day jumps straight to the evening
            source.getServer().getCommands().performPrefixedCommand(source.withSuppressedOutput(), "time set night");
        }
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

    private static int spawnCorner(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var p = source.getPlayerOrException();
        com.unholymc.world.DarkCorners.addManual(source.getServer().overworld(), p.blockPosition());
        source.sendSuccess(() -> Component.translatable("commands.unholymc.dark_corner.spawned", p.getBlockX(), p.getBlockZ()), true);
        return 1;
    }

    private static int toggleCorner(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var p = source.getPlayerOrException();
        ServerLevel ow = source.getServer().overworld();
        var near = com.unholymc.world.DarkCorners.cornersNear(ow, p.blockPosition(), 64);
        if (near.isEmpty()) {
            source.sendFailure(Component.translatable("commands.unholymc.dark_corner.none_near"));
            return 0;
        }
        boolean active = com.unholymc.world.DarkCorners.toggle(ow, near.getFirst());
        source.sendSuccess(() -> Component.translatable(active ? "commands.unholymc.dark_corner.night" : "commands.unholymc.dark_corner.day"), true);
        return 1;
    }

    private static int locate(CommandSourceStack source) {
        ServerLevel ow = source.getServer().overworld();
        net.minecraft.core.BlockPos from = net.minecraft.core.BlockPos.containing(source.getPosition());
        var corner = com.unholymc.world.DarkCorners.locate(ow, from, 6);
        var forest = com.unholymc.world.SlenderForests.locate(ow, from, 3000);
        source.sendSuccess(() -> Component.translatable("commands.unholymc.dark_corner.located",
                corner == null ? Component.translatable("commands.unholymc.dark_corner.not_found") : Component.literal(corner.getX() + ", " + corner.getZ()),
                forest == null ? Component.translatable("commands.unholymc.dark_corner.not_found") : Component.literal(forest.getX() + ", " + forest.getZ())), false);
        return 1;
    }

    /** /thm tp <structure>: teleports the player to the nearest generated structure of that kind (DECISIONS #44). */
    private static int tpTo(CommandSourceStack source, String what) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var p = source.getPlayerOrException();
        ServerLevel ow = source.getServer().overworld();
        net.minecraft.core.BlockPos from = p.level() == ow ? p.blockPosition() : ow.getRespawnData().pos();
        net.minecraft.core.BlockPos target = switch (what) {
            case "dark_corner" -> com.unholymc.world.DarkCorners.locate(ow, from, 6);
            case "slender_forest" -> com.unholymc.world.SlenderForests.locate(ow, from, 3000);
            case "grave" -> com.unholymc.world.Graves.nearest(ow, from, 20);
            default -> nearestPond(ow, from);
        };
        if (target == null) {
            source.sendFailure(Component.translatable("commands.unholymc.tp.not_found", Component.translatable("commands.unholymc.tp." + what)));
            return 0;
        }
        ow.getChunk(target.getX() >> 4, target.getZ() >> 4); // load it
        int y = ow.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
        p.teleportTo(ow, target.getX() + 0.5, y, target.getZ() + 0.5, java.util.Set.of(), p.getYRot(), p.getXRot(), true);
        source.sendSuccess(() -> Component.translatable("commands.unholymc.tp.done", Component.translatable("commands.unholymc.tp." + what),
                target.getX(), y, target.getZ()), true);
        return 1;
    }

    /** Nearest pond among the chunks that are currently loaded (ponds live in chunk data). */
    private static net.minecraft.core.@org.jspecify.annotations.Nullable BlockPos nearestPond(ServerLevel level, net.minecraft.core.BlockPos from) {
        net.minecraft.core.BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (var pond : com.unholymc.world.HolyPonds.pondsNear(level, from, 20)) {
            double d = pond.center().distSqr(from);
            if (d < bestD) {
                bestD = d;
                best = pond.center();
            }
        }
        return best;
    }
}
