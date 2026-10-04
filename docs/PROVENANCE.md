# Source and asset provenance

DoomCraft embeds the GPL-2.0 Doom C weapon simulation through the doomgeneric project. The vendored C source and its license are included; `tools/prepare_native.py` applies the host bridge adaptations reproducibly before compilation. Minecraft and Fabric dependencies are resolved through Gradle from their publishers and are not redistributed in this repository.

- doomgeneric upstream: https://github.com/ozkl/doomgeneric
- Upstream revision reported at acquisition: `dcb7a8dbc7a16ce3dda29382ac9aae9d77d21284`
- Exact downloaded source archive SHA-256: `52f599b5f2113fe09a84501c17f87f63a48e0a15fe4273ceb00beb58220225fc`
- Freedoom 0.13.0: https://github.com/freedoom/freedoom/releases/tag/v0.13.0
- Freedoom ZIP SHA-256: `3f9b264f3e3ce503b4fb7f6bdcb1f419d93c7b546f4df3e874dd878db9688f59`

The source archive was fetched from upstream master and the revision API was queried during the same acquisition; the archive digest is the exact downloaded identity. The checked-in source is authoritative for rebuilding this release.

Weapon, demon and portrait sprites, HUD digits, and gun sounds were extracted from licensed Freedoom 0.13.0 data. Attribution and redistribution terms are retained in `src/main/resources/licenses/Freedoom-COPYING.txt` and `Freedoom-CREDITS.txt`. Sound lumps were converted to Ogg Vorbis using ffmpeg. The armored marine skin and console panel are original project artwork. No commercial Doom WAD, Doomguy artwork, or Minecraft game binaries are included.

The checked-in assets are sufficient to build. To regenerate them, obtain the matching Freedoom release, unpack it at `vendor/freedoom-0.13.0`, and use the extraction scripts. These optional scripts require Python, Pillow and ffmpeg. Generated panel/skin assets use the corresponding asset scripts.

The native host uses a synthetic sector and upstream state tables; it does not require a commercial Doom WAD. Pistol/shotgun timing, spread, ammo and damage rolls run in C at 35 Hz. Minecraft owns terrain raycasts and entity damage. Native sound calls are disabled in the headless host; Freedoom gun sounds play through Minecraft's sound engine.
