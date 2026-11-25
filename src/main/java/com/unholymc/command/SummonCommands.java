package com.unholymc.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.unholymc.horror.HallucinationSpawner;
import com.unholymc.horror.Jumpscares;
import com.unholymc.registry.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** /thm summon <type> [player] (GDD chapter 7). */
public final class SummonCommands {
    private static final List<String> TYPES = List.of("aggressive", "neutral", "unholy", "jumpscare", "spooky_jumpscare",
            "hallucination", "slenderman_hallucination", "slenderman", "deadly_slenderman");
    private static final SimpleCommandExceptionType UNKNOWN = new SimpleCommandExceptionType(Component.translatable("commands.unholymc.summon.unknown"));
    private static final SimpleCommandExceptionType FAILED = new SimpleCommandExceptionType(Component.translatable("commands.unholymc.summon.failed"));
    private static final SimpleCommandExceptionType NOT_YET = new SimpleCommandExceptionType(Component.translatable("commands.unholymc.summon.not_yet"));

    private SummonCommands() {}

    public static void init() {
        ThmCommand.addSubcommand(root -> root.then(Commands.literal("summon")
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(TYPES, b))
                        .executes(c -> summon(c.getSource(), StringArgumentType.getString(c, "type"), c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> summon(c.getSource(), StringArgumentType.getString(c, "type"), EntityArgument.getPlayer(c, "player")))))));
        ThmCommand.CLEAR_HOOKS.add(HallucinationSpawner::clearFor);
    }

    private static int summon(CommandSourceStack source, String type, ServerPlayer player) throws CommandSyntaxException {
        switch (type) {
            case "aggressive" -> spawnInFront(player, ModEntities.AGGRESSIVE_SLOP);
            case "neutral" -> spawnInFront(player, ModEntities.NEUTRAL_SLOP);
            case "unholy" -> spawnInFront(player, ModEntities.UNHOLY_SLOP);
            case "slenderman_hallucination" -> spawnInFront(player, ModEntities.SLENDERMAN_HALLUCINATION);
            case "hallucination" -> {
                if (HallucinationSpawner.spawnFor(player) == null) throw FAILED.create();
            }
            case "jumpscare" -> {
                if (!Jumpscares.screamer(player)) throw FAILED.create();
            }
            case "spooky_jumpscare" -> Jumpscares.spooky(player);
            case "slenderman", "deadly_slenderman" -> throw NOT_YET.create();
            default -> throw UNKNOWN.create();
        }
        source.sendSuccess(() -> Component.translatable("commands.unholymc.summon.success", type, player.getDisplayName()), true);
        return 1;
    }

    private static <T extends Mob> void spawnInFront(ServerPlayer player, EntityType<T> type) throws CommandSyntaxException {
        ServerLevel level = player.level();
        T mob = type.create(level, EntitySpawnReason.COMMAND);
        if (mob == null) throw FAILED.create();
        Vec3 look = player.getViewVector(1f).multiply(1, 0, 1).normalize();
        Vec3 pos = player.position().add(look.scale(4));
        mob.snapTo(pos.x, player.getY(), pos.z, player.getYRot() + 180f, 0f);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.COMMAND, null);
        level.addFreshEntity(mob);
    }
}
