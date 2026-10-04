# Verified scope — 2026-10-04

- Clean publication tree: Gradle build passed; packaged Apple Silicon native library included.
- Native tests: 22 assertions passed, including original firing cadence, spread, reload/state transitions, safe ammo exhaustion and saved-ammo restore/clamping.
- Latest fresh-world client run: 33 assertions passed. Both weapons consumed persistent reserves and damaged real vanilla mobs; six mobs were killed. Natural logs were mined and picked up; real inventory crafting produced planks and a workbench; three crafted blocks were placed; workbench interaction took priority over firing; an ammo pack and food were consumed; save/quit/reload preserved ammo, inventory and placed blocks.
- Earlier separate client restarts: 14 assertions passed for retained state, craftable ammunition, underwater air, mount health, death/respawn and absence of duplicated starter weapons; another restart passed four post-death persistence checks.
- HUD visually checked at 1280×720 and 960×540, GUI scales 2/3/4, with durability, stacks, armor, absorption, XP, effects, offhand, air and mount state.

## Recording disclosure

The 54-second showcase is an edited recording of the actual Java Minecraft client and its synchronized game audio. There is no replacement soundtrack, fabricated HUD, added gunfire, or browser imitation. Short cuts remove waiting and staging; the action itself plays at normal speed.

The opt-in harness creates a new seed-1234 survival world through the normal menu. It places real vanilla mobs, teleports between locations, sets scene time, restores health between combat scenes, supplies ammo/food for interaction checks and stages armor/effects for HUD checks. The combat uses the actual native weapon simulation, Minecraft terrain raycasts, mob AI and damage. Mining, crafting and building use actual game inputs/slot interactions. The demo is an automated showcase, not an uninterrupted unaided survival run.

Normal `Play.command` does not enable staging or automation. Existing user worlds are excluded from the repository. The separate arena/night launchers remain available.

Supported and tested: singleplayer, Minecraft 1.21.1 / Fabric, Apple Silicon macOS. Separate retail launcher installation, multiplayer, Windows/Linux and third-party modpacks have not been verified. No claim of feature parity with the entire Doom campaign or other game-combination projects is made.
