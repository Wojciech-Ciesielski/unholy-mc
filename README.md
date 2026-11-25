# unholy-mc
The unholy mc repository

Survival-horror mod for Minecraft 26.2.1 (Fabric), multiplayer-first.

- `docs/GDD_original.md` – original design document (PL)
- `docs/DECISIONS.md` – clarifications that override the GDD
- `assets_src/slop_png_raw/` – raw source images for PNG slops

## Building

- Minecraft **26.2** (Fabric has no 26.2.1 release – to be confirmed), Fabric Loader 0.20.1, Fabric API 0.161.0+26.2, Loom 1.19.4.
- Requires **JDK 25** (`JAVA_HOME` pointing at it); Gradle 9.9.2 via the wrapper.
- `./gradlew build` → `build/libs/unholymc-<version>.jar` (put it with Fabric API into `mods/` on server and clients).
- `./gradlew runClient` / `./gradlew runServer` for a dev instance (run dir: `run/`).
- Server config: `config/unholymc.json` (created on first start, every gameplay number lives there; `/thm reload` re-reads it).

Assets (textures, sounds) are specified in `docs/ASSETS.md`; missing `.ogg` files only log a warning.
