package com.unholymc;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnholyMC implements ModInitializer {
    public static final String MOD_ID = "unholymc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("The Unholy Minecraft is waking up...");
    }
}
