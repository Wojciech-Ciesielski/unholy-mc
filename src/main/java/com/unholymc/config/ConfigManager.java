package com.unholymc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.unholymc.UnholyMC;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(UnholyMC.MOD_ID + ".json");
    private static UnholyConfig config = new UnholyConfig();

    private ConfigManager() {}

    public static UnholyConfig get() {
        return config;
    }

    /** Loads the config (creating it with defaults if absent) and re-saves it so new fields appear. */
    public static boolean load() {
        UnholyConfig loaded = null;
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                loaded = GSON.fromJson(reader, UnholyConfig.class);
            } catch (Exception e) {
                UnholyMC.LOGGER.error("Failed to read {}, keeping previous values", PATH, e);
                return false;
            }
        }
        config = loaded != null ? loaded : new UnholyConfig();
        save();
        return true;
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            UnholyMC.LOGGER.error("Failed to write {}", PATH, e);
        }
    }
}
