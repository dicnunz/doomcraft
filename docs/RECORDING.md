# Record the opt-in showcase

On macOS 15+ with screen-recording permission, compile `tools/RecordGame.swift` using `swiftc -parse-as-library tools/RecordGame.swift -o /tmp/doomcraft-record`. The recorder selects only the Minecraft Java window, records its application audio, and disables microphone capture. It never records the whole desktop.

Remove a stale `/tmp/doomcraft-recording-ready`, start `./tools/gradle.sh runClient -Psurvival -PsurvivalDemo -Pshowcase`, then run `/tmp/doomcraft-record /absolute/path/take.mp4 230`. The recording gate lets the real creation menu run after capture has started. This command makes a fresh test world and stages documented combat scenes. Phase timestamps printed in the game log can guide editing. See VERIFICATION.md for staging disclosures.

The recorder is a development utility for a 1280×720 game content area. Inspect framing and audio before publishing. The default duration includes waiting; trim using matching video/audio timestamps, and do not add gameplay sound effects in post.
