package com.unholymc.polymer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.unholymc.UnholyMC;
import com.unholymc.attachment.HudState;
import com.unholymc.attachment.ModAttachments;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The HUD for vanilla clients (DECISIONS #34), drawn with the design thread's font {@code unholymc:hud_stats}
 * (docs/ASSETS.md section 4): the four stats as rows at the left edge, the armour as slots at the right edge
 * (left of the effect icons). Vanilla hearts, hunger and armour are hidden by transparent sprites in the pack.
 *
 * Carrier: five transparent boss bars (a spacer and one per row; the pack makes the green bar sprites invisible).
 * A boss bar's name is centred on the screen, so each row is a string of exactly zero total width with negative and
 * positive space glyphs moving the content to the screen edges; that needs the distance from the screen centre to the
 * edge in GUI pixels, which depends on window size and GUI scale. Players set it with {@code /hud edge <px>}
 * (default 240 = a 1080p screen at GUI scale 4).
 */
public final class ServerHud {
    private static final Style STYLE = Style.EMPTY.withFont(new FontDescription.Resource(UnholyMC.id("hud_layout"))).withShadowColor(0).withColor(0xFFFFFF);
    private static final int ROWS = 4;

    // hud_stats glyph advances (bitmap width * height scale + 1) and the space glyphs of hud_layout.json
    private static final int ADV_PANEL = 45, ADV_ICON = 17, ADV_DIGIT = 8, ADV_FRAME = 21, ADV_DURABILITY = 17;
    private static final int ADV_SPRITE = 9; // advance of an item sprite in text, tuned in game
    private static final int MARGIN = 4, EFFECT_COLUMN = 26;

    private static final int WHITE = 0xFFFFFF, YELLOW = 0xFFE070, ORANGE = 0xFF9A30, RED = 0xFF4040, GOLD = 0xF0C020;

    private static final class Bars {
        final ServerBossEvent[] events = new ServerBossEvent[ROWS + 1];
        final String[] shown = new String[ROWS + 1];
        int edge = -1;
    }

    private static final Map<UUID, Bars> BARS = new HashMap<>();

    private ServerHud() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(ServerHud::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            create(handler.player);
            if (!handler.player.hasAttached(ModAttachments.HUD_EDGE)) {
                handler.player.setAttached(ModAttachments.HUD_EDGE, 240);
                handler.player.sendSystemMessage(Component.translatable("message.unholymc.hud_hint"));
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Bars b = BARS.remove(handler.player.getUUID());
            if (b != null) for (ServerBossEvent e : b.events) e.removeAllPlayers();
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("hud")
                        .then(Commands.literal("edge")
                                .then(Commands.argument("pixels", IntegerArgumentType.integer(120, 1200)).executes(c -> {
                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                    int v = IntegerArgumentType.getInteger(c, "pixels");
                                    p.setAttached(ModAttachments.HUD_EDGE, v);
                                    c.getSource().sendSuccess(() -> Component.translatable("commands.unholymc.hud.edge", v), false);
                                    return v;
                                })))
                        .then(Commands.literal("reset").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            p.setAttached(ModAttachments.HUD_EDGE, 240);
                            c.getSource().sendSuccess(() -> Component.translatable("commands.unholymc.hud.edge", 240), false);
                            return 240;
                        }))));
    }

    private static void create(ServerPlayer player) {
        Bars bars = new Bars();
        for (int i = 0; i <= ROWS; i++) {
            ServerBossEvent e = new ServerBossEvent(UUID.randomUUID(), Component.empty(), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
            e.setProgress(0f);
            e.addPlayer(player);
            bars.events[i] = e;
        }
        BARS.put(player.getUUID(), bars);
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Bars bars = BARS.get(p.getUUID());
            if (bars == null) continue;
            int edge = p.getAttachedOrElse(ModAttachments.HUD_EDGE, 240);
            boolean visible = !p.isSpectator() && !p.isCreative();
            for (int i = 0; i < ROWS; i++) {
                String key;
                Component text;
                if (!visible) {
                    key = "";
                    text = Component.empty();
                } else {
                    Row row = new Row(edge);
                    stat(row, p, i);
                    armour(row, p, i);
                    key = row.key.toString();
                    text = row.root;
                }
                if (edge != bars.edge || !key.equals(bars.shown[i + 1])) {
                    bars.shown[i + 1] = key;
                    bars.events[i + 1].setName(visible ? text : Component.empty());
                }
            }
            bars.edge = edge;
        }
    }

    // --- one boss bar line ---

    /** Builds the text of one line, tracking the text cursor so the line ends exactly at zero. */
    private static final class Row {
        final MutableComponent root = Component.empty().withStyle(STYLE);
        final StringBuilder key = new StringBuilder();
        final int edge;
        int x; // cursor relative to the centre of the screen

        Row(int edge) {
            this.edge = edge;
        }

        /** Moves the cursor to {@code target} with space glyphs. */
        void moveTo(int target) {
            int d = target - x;
            if (d == 0) return;
            StringBuilder sb = new StringBuilder();
            int rest = Math.abs(d);
            for (int bit = 7; bit >= 0; bit--) {
                int v = 1 << bit;
                while (rest >= v) {
                    sb.append((char) ((d > 0 ? 0xE500 : 0xE510) + bit));
                    rest -= v;
                }
            }
            root.append(Component.literal(sb.toString()));
            key.append('[').append(d).append(']');
            x = target;
        }

        void glyph(int code, int color, int advance) {
            root.append(Component.literal(String.valueOf((char) code)).withColor(color));
            key.append(Integer.toHexString(code)).append(':').append(Integer.toHexString(color));
            x += advance;
        }

        void number(int value, int color) {
            for (char c : Integer.toString(Math.max(0, value)).toCharArray()) glyph(0xE480 + (c - '0'), color, ADV_DIGIT);
        }

        void finish() {
            moveTo(0);
        }
    }

    // --- stats (left) ---

    private static int stateColor(int points) {
        return points >= 15 ? WHITE : points >= 10 ? YELLOW : points >= 5 ? ORANGE : RED;
    }

    /** 0 full, 1 medium, 2 low, 3 critical. */
    private static int state(int points) {
        return points >= 15 ? 0 : points >= 10 ? 1 : points >= 5 ? 2 : 3;
    }

    private static void stat(Row row, ServerPlayer p, int index) {
        HudState hud = ModAttachments.hud(p);
        long t = p.level().getGameTime();
        int base, points, icon, extra = 0;
        switch (index) {
            case 0 -> { // health
                points = (int) Math.ceil(p.getHealth());
                extra = (int) Math.ceil(p.getAbsorptionAmount());
                base = 0xE400;
                int st = state(points);
                icon = p.hasEffect(MobEffects.WITHER) ? 6 : p.hasEffect(MobEffects.POISON) ? 5 : p.isFullyFrozen() ? 7
                        : extra > 0 && extra >= points ? 8 : st == 3 ? (t / 10 % 2 == 0 ? 3 : 4) : st;
            }
            case 1 -> { // hunger
                points = p.getFoodData().getFoodLevel();
                base = 0xE410;
                int st = state(points);
                icon = p.hasEffect(MobEffects.HUNGER) ? 5 : st == 3 ? (t / 10 % 2 == 0 ? 3 : 4) : st;
            }
            case 2 -> { // hydration
                if (!hud.enabled()) return;
                points = hud.hydration();
                base = 0xE420;
                int st = state(points);
                icon = st == 3 ? (t / 10 % 2 == 0 ? 3 : 4) : st;
            }
            default -> { // sanity
                if (!hud.enabled()) return;
                points = hud.sanity();
                base = 0xE430;
                int st = state(points);
                icon = p.hasEffect(com.unholymc.registry.ModEffects.WELLBEING) ? 5
                        : st == 3 ? (t / 5 % 3 == 0 ? 4 : 3) : st; // critical: the icon "trembles" now and then
            }
        }
        row.moveTo(-(row.edge - MARGIN));
        row.glyph(0xE490, WHITE, ADV_PANEL);
        row.glyph(0xE4F6, WHITE, -45);
        row.glyph(0xE4F7, WHITE, 2);
        row.glyph(base + icon, WHITE, ADV_ICON);
        row.glyph(0xE4F7, WHITE, 2);
        row.number(points, stateColor(points));
        if (extra > 0) {
            row.glyph(0xE48A, GOLD, ADV_DIGIT);
            row.number(extra, GOLD);
        }
    }

    // --- armour (right) ---

    private static final EquipmentSlot[] ARMOUR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] EMPTY_SLOT = {"helmet", "chestplate", "leggings", "boots"};

    private static void armour(Row row, ServerPlayer p, int index) {
        ItemStack stack = p.getItemBySlot(ARMOUR[index]);
        int frameX = row.edge - MARGIN - EFFECT_COLUMN - 20;
        row.moveTo(frameX);
        row.glyph(0xE4A0, WHITE, ADV_FRAME);
        if (!stack.isEmpty() && stack.isDamageableItem()) {
            float left = 1f - (float) stack.getDamageValue() / stack.getMaxDamage();
            row.moveTo(frameX + 2);
            row.glyph(0xE4B0 + Math.round(left * 8f), WHITE, ADV_DURABILITY);
        }
        row.moveTo(frameX + 6);
        AtlasSprite sprite;
        if (stack.isEmpty()) {
            sprite = new AtlasSprite(Identifier.withDefaultNamespace("gui"), Identifier.withDefaultNamespace("container/slot/" + EMPTY_SLOT[index]));
        } else {
            Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            sprite = new AtlasSprite(Identifier.withDefaultNamespace("items"), id.withPrefix("item/"));
        }
        row.root.append(Component.object(sprite).withColor(WHITE));
        row.key.append("s:").append(sprite.sprite());
        row.x += ADV_SPRITE;
        row.finish();
    }
}
