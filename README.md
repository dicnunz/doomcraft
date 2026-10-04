# DoomCraft 0.3 — Doom weapons in Minecraft survival

Minecraft 1.21.1 survival, with original Doom C pistol/shotgun simulation running at 35 Hz through JNI. Minecraft owns the generated world, mobs, terrain collision, damage, inventory, crafting, food, death and saves. The bottom of the screen is one Doom-style console, including the equipment slots and survival information.

## Play on Apple Silicon macOS

Double-click **Play.command**, or run `./Play.command`. Choose **Singleplayer**, then create a Survival world or load a saved one normally. Install a JDK 21+ (tested with JDK 22), Gradle 8.8, Python 3, and Apple command-line developer tools (`xcode-select --install`). The launcher uses those tools. Your survival saves are in `run-survival/saves`; older arena/night worlds remain in `run/saves`.

- WASD/mouse to move and aim; Space to jump; Shift to sneak.
- **1–9 or scroll** select the nine slots shown in the console's 3×3 equipment matrix, left to right, top to bottom.
- **Left mouse** mines/attacks normally. **Right mouse** fires a held gun; a workbench, chest or other usable block takes priority.
- **E** opens the real inventory and crafting screen. **F5** shows the armored marine skin. **F** swaps offhand; stow the gun to use offhand items.
- Right-click an ammo pack to fill its reserve. There is no magazine reload. No restock commands are enabled in normal Survival.
- Escape → Save and Quit saves the ordinary world. The same player profile and reserve ammo are restored on the next launch.

Each player receives one pistol, one shotgun, 60 bullets and 12 shells on first joining each world. Death drops inventory normally. Reserve ammo stays with the player; respawn does **not** issue another kit. Recover dropped guns or craft replacements.

## Read the console

The large red values are current ammo, health, armor and food. Health/armor/food use the classic 0–100 scale (Minecraft points ×5); absorption appears beside health. The portrait responds to health and damage. The equipment matrix shows selected slot, real item icons, stack counts and durability. The right side shows bullet/shell reserves, XP level/progress, offhand and active effect icons with remaining seconds. Air is always readable. The readiness meter shows melee cooldown, or mount jump charge with mount health while riding. Open inventory for complete effect names/details when more than four are active.

## Crafting

Shapeless recipes appear in the recipe book:

| Item | Ingredients | Result |
|---|---|---|
| Pistol | 2 iron ingots + flint | 1 pistol |
| Shotgun | 3 iron ingots + oak planks | 1 shotgun |
| Bullets | iron nugget + gunpowder | 20-bullet pack |
| Shells | paper + iron nugget + gunpowder | 8-shell pack |

Reserve limits: 200 bullets, 50 shells. A pack used near the limit fills the reserve and discards overflow; a full reserve does not consume it.

## Build and install

Requires Apple Silicon macOS, a JDK 21+, Gradle 8.8, Python 3 and Clang. A licensed Minecraft Java installation is required for ordinary launcher play.

```sh
./tools/test-native.sh
./tools/gradle.sh build
./Play.command
```

The current mod is `build/libs/doomcraft-0.3.0.jar`. It includes the Apple Silicon native engine. For a normal licensed Minecraft Java installation, use a Fabric 1.21.1 profile, Fabric Loader 0.16.5 or newer and Fabric API for 1.21.1; place this JAR in that profile's mods folder. Survival is enabled by default. The launchers use Fabric's development client with the fixed `DoomCraftPlayer` profile; this is not Minecraft account authentication. Installation in a separate retail launcher has not been verified here. Other operating systems need their own native build.

**Arena.command** preserves the original native-demon arena. **NightLab.command** preserves the staged, fixed-night combat lab. Their shortcuts and mechanics are separate from normal Survival.

## Verification and recording

See `docs/VERIFICATION.md` for test results and recording disclosures. The opt-in harness creates a fresh QA world using the actual creation screen:

```sh
./tools/gradle.sh runClient -Psurvival -PsurvivalDemo
# Resume the exact world named in the generated docs/final-survival/checkpoint.properties:
./tools/gradle.sh runClient -Psurvival -PsurvivalDemo '-PsurvivalResume=DoomCraft Survival <timestamp>'
```

It stages a real zombie and finds a natural tree for reproducible interaction tests; normal Play does not stage mobs, teleport the player, change time, or run the harness. Captures are actual game frames. Cuts and test staging are documented with the video.

## Integration and assets

Original upstream C weapon state tables, cadence, spread, damage rolls and ammo use execute at 35 Hz. JNI exports pellet events; Minecraft raycasts its real terrain and living entities and applies damage through its normal rules. Normal Minecraft mining/building continues independently. This is an embedded Doom simulation, not two full game processes or a Doom campaign port. The optional arena also runs native Doom demon AI.

Weapon sprites, portrait and sound are licensed **Freedoom 0.13.0**, not commercial Doom artwork. The armored marine skin and console panel are original project art. See `docs/PROVENANCE.md`, `docs/SURVIVAL-DESIGN.md` and the included licenses. Singleplayer on Apple Silicon is the tested scope; multiplayer and third-party modpacks are not verified.
