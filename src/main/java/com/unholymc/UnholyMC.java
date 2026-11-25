package com.unholymc;

import com.unholymc.attachment.ModAttachments;
import com.unholymc.command.ThmCommand;
import com.unholymc.config.ConfigManager;
import com.unholymc.network.ModNetworking;
import com.unholymc.registry.ModEffects;
import com.unholymc.registry.ModComponents;
import com.unholymc.registry.ModEntities;
import com.unholymc.registry.ModItems;
import com.unholymc.registry.ModBlocks;
import com.unholymc.world.SlenderForests;
import com.unholymc.world.DarkCorners;
import com.unholymc.item.ItemHooks;
import com.unholymc.light.DynamicLights;
import com.unholymc.command.ItemCommands;
import com.unholymc.command.SummonCommands;
import com.unholymc.command.WorldCommands;
import com.unholymc.world.BloodMoon;
import com.unholymc.world.HolyPonds;
import com.unholymc.world.WeatherControl;
import com.unholymc.world.WorldState;
import com.unholymc.world.feature.ModFeatures;
import com.unholymc.entity.SlopSpawning;
import com.unholymc.horror.HallucinationSpawner;
import com.unholymc.horror.Jumpscares;
import com.unholymc.registry.ModSounds;
import com.unholymc.survival.SurvivalSystem;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnholyMC implements ModInitializer {
    public static final String MOD_ID = "unholymc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ConfigManager.load();
        ModSounds.init();
        ModEffects.init();
        ModComponents.init();
        ModBlocks.init();
        ModItems.init();
        ModEntities.init();
        ModAttachments.init();
        ModNetworking.init();
        SurvivalSystem.init();
        DynamicLights.init();
        ItemHooks.init();
        ItemCommands.init();
        SlopSpawning.init();
        Jumpscares.init();
        HallucinationSpawner.init();
        SummonCommands.init();
        WorldState.init();
        BloodMoon.init();
        WeatherControl.init();
        HolyPonds.init();
        ModFeatures.init();
        WorldCommands.init();
        SlenderForests.init();
        DarkCorners.init();
        ThmCommand.init();
        LOGGER.info("The Unholy Minecraft is waking up...");
    }
}
