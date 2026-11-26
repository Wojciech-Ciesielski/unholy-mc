# unholy-mc
The unholy mc repository

Survival-horror mod for Minecraft 26.2 (Fabric), multiplayer-first. **Server-side only**: players join with a plain vanilla client.

- `docs/GDD_original.md` – original design document (PL)
- `docs/DECISIONS.md` – clarifications that override the GDD
- `assets_src/slop_png_raw/` – raw source images for PNG slops

## Building

- Minecraft **26.2** (confirmed target version), Fabric Loader 0.20.1, Fabric API 0.161.0+26.2, Loom 1.19.4.
- Requires **JDK 25** (`JAVA_HOME` pointing at it); Gradle 9.9.2 via the wrapper.
- `./gradlew build` → `build/libs/unholymc-<version>.jar`.

## Installing (server only)

1. Fabric server for Minecraft 26.2, Java 25.
2. Put `unholymc-<version>.jar` and Fabric API into the server's `mods/`. Nothing else: Polymer is bundled inside our jar.
3. Players connect with a **vanilla** Minecraft 26.2 client (no Fabric, no mods) and accept the server resource pack.
   The pack is generated and served automatically on the game port (Polymer AutoHost; `config/polymer/auto-host.json`,
   created with `enabled: true` on the first start).

The client-visible parts are built with Polymer: items and blocks use vanilla stand-ins with our models,
mobs are invisible vanilla mobs carrying display entities (PNG billboards, Slenderman's six-part model),
the sanity / hydration bars are font glyphs in the action bar, jumpscares are full-screen title glyphs,
the Blood Moon tints the screen through a per-player world border warning.
**HUD:** the four stats (health, hunger, hydration, sanity) are rows at the left screen edge and the armour slots at the right edge, drawn as boss-bar text with the design thread's font. Vanilla hearts, hunger and armour are hidden by the resource pack. The distance from the screen centre to the edge depends on the window size and GUI scale, so each player can calibrate it with `/hud edge <px>` (default 240, half the width of the screen in GUI pixels; `/hud reset`). The layout needs a screen at least ~270 GUI pixels high (e.g. 1080p at GUI scale 4 or smaller).
Known limits for vanilla clients: holy water in ponds is not tinted (particles and light only), no custom sky colours.
- `./gradlew runClient` / `./gradlew runServer` for a dev instance (run dir: `run/`).
- Server config: `config/unholymc.json` (created on first start, every gameplay number lives there; `/thm reload` re-reads it).

Assets (textures, sounds) are specified in `docs/ASSETS.md`; missing `.ogg` files only log a warning.
