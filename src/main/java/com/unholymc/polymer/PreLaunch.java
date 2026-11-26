package com.unholymc.polymer;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plug and play: on the first start, turn on Polymer's resource pack auto-hosting (off by default outside a dev
 * environment). The pack is then served on the game port itself and sent to every joining player as required.
 * An existing config/polymer/auto-host.json is never touched, so server owners keep full control.
 */
public final class PreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("polymer").resolve("auto-host.json");
        if (Files.exists(file)) return;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, """
                    {
                      "enabled": true,
                      "required": true,
                      "type": "polymer:automatic"
                    }
                    """);
        } catch (IOException e) {
            System.err.println("[unholymc] Could not write " + file + ": " + e);
        }
    }
}
