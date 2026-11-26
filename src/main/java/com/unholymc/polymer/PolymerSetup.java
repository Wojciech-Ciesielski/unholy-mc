package com.unholymc.polymer;

import com.unholymc.UnholyMC;
import com.unholymc.registry.ModComponents;
import com.unholymc.registry.ModEffects;
import com.unholymc.registry.ModItems;
import com.unholymc.registry.ModSounds;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import eu.pb4.polymer.core.api.other.PolymerMobEffect;
import eu.pb4.polymer.core.api.other.PolymerSoundEvent;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;

/**
 * Makes the mod playable with a vanilla client (Polymer): every entry we add to a synchronized registry is either
 * shown to the client as a vanilla stand-in (items, entities, blocks) or kept server-side (effect, components),
 * and our textures, models, sounds, fonts and translations reach players through the server resource pack.
 */
public final class PolymerSetup {
    private PolymerSetup() {}

    /** Items are shown as a stick (or a netherite sword for the sword) wearing our own item model from the pack. */
    private record Overlay(Item base) implements PolymerItem {
        @Override
        public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
            return base;
        }
    }

    /**
     * The thermos: a stick for vanilla clients, but with a drinking animation. The client plays the use animation
     * from the item it sees, so a filled thermos is shown with a consumable component (the server finishes the use).
     */
    private record ThermosOverlay() implements PolymerItem {
        @Override
        public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
            return Items.STICK;
        }

        @Override
        public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context, net.minecraft.core.HolderLookup.Provider lookup) {
            if (com.unholymc.item.ThermosItem.contents(stack).isEmpty()) return;
            float seconds = com.unholymc.config.ConfigManager.get().thermos.drinkTicks / 20f;
            out.set(net.minecraft.core.component.DataComponents.CONSUMABLE, net.minecraft.world.item.component.Consumable.builder().consumeSeconds(seconds)
                            .animation(net.minecraft.world.item.ItemUseAnimation.DRINK)
                            .sound(net.minecraft.sounds.SoundEvents.GENERIC_DRINK).hasConsumeParticles(false).build());
        }
    }

    public static void init() {
        PolymerResourcePackUtils.addModAssets(UnholyMC.MOD_ID);
        PolymerResourcePackUtils.markAsRequired();
        PolymerBlocks.init();
        MobVisuals.init();
        ServerHud.init();
        ServerFx.init();
        BloodMoonPack.init();

        for (Item item : new Item[]{ModItems.NIGHTMARE_GEM, ModItems.HOLY_WATER, ModItems.SPLASH_HOLY_WATER, ModItems.CRUCIFIX,
                ModItems.FLASHLIGHT, ModItems.GRAVE_CROSS}) {
            PolymerItem.registerOverlay(item, new Overlay(Items.STICK));
        }
        PolymerItem.registerOverlay(ModItems.THERMOS, new ThermosOverlay());
        PolymerItem.registerOverlay(ModItems.UNHOLY_SWORD, new Overlay(Items.NETHERITE_SWORD));

        for (var sound : BuiltInRegistries.SOUND_EVENT.entrySet()) {
            if (sound.getKey().identifier().getNamespace().equals(UnholyMC.MOD_ID)) PolymerSoundEvent.registerOverlay(sound.getValue());
        }
        PolymerMobEffect.registerOverlay(ModEffects.WELLBEING.value());
        PolymerComponent.registerDataComponent(ModComponents.FLASHLIGHT, ModComponents.THERMOS, ModComponents.DISCHARGED);
    }
}
