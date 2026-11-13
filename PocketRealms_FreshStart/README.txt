POCKET REALMS
Minecraft 1.21.1 / NeoForge 21.1.265

A lightweight portable-base mod built around ONE shared pocket dimension.
It does not create a separate dimension for every anchor.

FEATURES
- One shared custom void dimension: pocketrealms:pockets.
- Up to FIVE personal pocket homes per player.
- Each home has a 30 x 30 usable build floor plus a one-block perimeter wall (32 x 32 total footprint).
- Plot centres are separated by 300 blocks along X inside the same dimension.
- Every Pocket Anchor is bound to one owner's pocket slot.
- Break an anchor in Survival to carry it; its bound ID is preserved on the dropped item.
- Place a carried anchor again to return to the same home. It does not create another plot.
- Every new plot includes a free Pocket Gateway.
- Right-click a gateway to cycle between your owned pockets for free.
- Sneak + right-click a gateway to return to the dimension and position you came from.
- Other players cannot use your anchor, break/place blocks in your plot, or interact with blocks there.
- If an anchor is lost, `/pockets recover <slot>` gives you a replacement bound to that same home.
- `/pockets` lists your homes; `/pockets visit <slot>` visits one directly; `/pockets return` returns to your saved entry point.
- Pocket plots are created only when first entered, so unused pockets do not generate chunks.
- Falling below the platform triggers a void-safety rescue to your nearest owned home instead of a void death.
- No Nether Star, Ender Chest, Nether materials, or boss drops are required.

RECIPES
Pocket Anchor:
P C P
C R C
P C P
P = Oak Planks, C = Copper Ingot, R = Redstone Dust

Pocket Gateway:
S I S
C R C
S I S
S = Stone Bricks, I = Iron Nugget, C = Copper Ingot, R = Redstone Dust

BUILD THE MOD JAR (WINDOWS)
1. Install a 64-bit Java 21 JDK. The Java bundled with some launchers may be runtime-only and may not include javac.
2. Extract this project folder.
3. Double-click BUILD_MOD.bat.
4. On first run it downloads Gradle 9.2.1 and the NeoForge development libraries. Internet access is required.
5. After a successful build, copy build\libs\pocketrealms-0.1.0-alpha.jar into your Minecraft instance's mods folder.
6. Launch Minecraft 1.21.1 with NeoForge 21.1.265 or a compatible 21.1.x build.

IMPORTANT
- Back up your world before testing the mod. It adds a custom dimension and saved player data.
- The pocket dimension is shared to keep resource usage low; each personal plot is spaced 300 blocks from the next.
- Plot protection covers player block breaking, placing, and right-click interactions. Do not treat it as a complete anti-grief system against every possible modded contraption, fluid, explosion, or server plugin. It is intended primarily for single-player and small private servers.
- This project source has NOT been compiled or run in this environment because outbound access to download Gradle/NeoForge build dependencies is unavailable. The ZIP is source code, not a ready-to-install JAR. BUILD_MOD.bat performs the actual compilation on Windows with Java 21 and internet access.
- If compilation fails, send the complete output from BUILD_MOD.bat. Do not rename the source ZIP to .jar or place it in the mods folder.
