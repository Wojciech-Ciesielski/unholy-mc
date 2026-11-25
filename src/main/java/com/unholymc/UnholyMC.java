package com.unholymc;

import com.unholymc.attachment.ModAttachments;
import com.unholymc.command.ThmCommand;
import com.unholymc.config.ConfigManager;
import com.unholymc.network.ModNetworking;
import com.unholymc.registry.ModEffects;
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
        ModAttachments.init();
        ModNetworking.init();
        SurvivalSystem.init();
        ThmCommand.init();
        LOGGER.info("The Unholy Minecraft is waking up...");
    }
}
