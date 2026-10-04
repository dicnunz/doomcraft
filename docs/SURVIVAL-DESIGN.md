# Survival integration decisions

- Minecraft's normal title, world-creation screen, world list, saves, generation, day/night cycle and game rules remain in charge. Survival uses a separate `run-survival` directory. Older `run` worlds and the optional arena are preserved.
- World-owned `PersistentState`, keyed by the player UUID, stores reserve bullets/shells and implicitly whether the starter kit has been issued. Every ammo change marks the state dirty; normal world saving writes it. The development profile is fixed to `DoomCraftPlayer` so launch-to-launch UUIDs are stable. This is a local Fabric development profile, not account authentication.
- Death follows Minecraft's normal inventory-drop and respawn rules. Ammo is a persistent reserve and is retained on death; no free replacement guns or extra ammo are issued. Weapons can be recovered from drops or crafted. This policy prevents resetting the game to farm a starter kit.
- The original C weapon state machines own cadence, spread, ammo expenditure and damage rolls. Minecraft owns entity health, terrain raycasts, mining, building, crafting, inventory, hunger, armor, air, XP, effects and mounts. One full-width, bottom-anchored Doom console replaces the vanilla status bars, hotbar, XP bar/level, held-item name, crosshair and effect HUD. Its ARMS-style 3×3 equipment matrix represents the actual nine hotbar slots, including item counts and durability. A central Freedoom portrait and red numeric vitals remain the visual hierarchy. Food, air, absorption, XP, effects, offhand, attack readiness and mount health/jump are integrated into the same metal panel. Transient messages and chat move above its top edge. Inventory/crafting menus remain Minecraft screens with genuine slot logic.
- Right-click with a weapon fires when normal Minecraft block interaction does not consume the click. Workbenches/chests retain priority; stow the gun for offhand-item use. Ammo packs use right-click and consume one pack only when reserve space exists.
- Pistol/shotgun recipes and renewable ammo recipes appear in the recipe book. Ammo packs can partially fill the capped reserve; overflow is discarded. Ammo caps remain 200 bullets / 50 shells.
- The armored marine Minecraft skin is original artwork inspired by the requested role. The weapon sprites, sounds and status portrait remain licensed Freedoom assets. None is claimed to be the original commercial Doomguy artwork.

Primary API references checked against the installed 1.21.1 mappings:
- [PersistentState and writeNbt](https://maven.fabricmc.net/docs/yarn-1.21.1+build.3/net/minecraft/world/PersistentState.html)
- [SkinTextures](https://maven.fabricmc.net/docs/yarn-1.21.1+build.3/net/minecraft/client/util/SkinTextures.html)

These APIs establish the storage/rendering mechanisms; HUD layout, ammo economy and death policy are project design choices. Singleplayer is the supported scope. Third-party modpacks, dedicated servers and multiplayer are not verified.

## 0.3 packaging and verification

The default mod entry activates survival; the arena/night launchers explicitly retain their own modes. The JAR embeds the Apple Silicon native library; `NativeBridge` extracts it to a temporary file when no development override is supplied. This packages the same C engine without relying on relative workspace paths.

The new panel is 640×72 design pixels, scaled to the full viewport width. It is independent of Minecraft GUI scale, so inventory GUI scaling does not shrink the survival vitals. Live visual checks cover 1280×720 and 960×540 window sizes (Retina framebuffer), GUI scales 2/3/4, stacked items, damaged tools, armor, absorption, XP, effects, depleted air, and a ridden horse. Explicit flushes separate batched rectangle/text rendering from immediate texture patches; real screenshots caught and corrected an overlapping fill issue.


API target: [Fabric Yarn 1.21.1 InGameHud](https://maven.fabricmc.net/docs/yarn-1.21.1+build.3/net/minecraft/client/gui/hud/InGameHud.html), also checked against the installed named JAR with javap.
