# Pocket Realms (alpha)

Minecraft 1.21.1 / NeoForge 21.1.265 / Java 21.

## Features planned in this alpha

- Five personal pocket plots per player.
- One shared pocket dimension with plots laid out 300 blocks apart.
- A 30 x 30 build area for each plot.
- Portable Pocket Anchors that keep their assigned pocket ID.
- Pocket Gateways for travel among a player's pockets and return travel.
- Owner protection, void rescue, and pocket recovery commands.

## Build on GitHub

The included GitHub Actions workflow compiles the mod with Java 21 and Gradle 9.2.1. It checks that the Java sources exist and that the resulting JAR contains the Pocket Realms main class before uploading the artifact.

After uploading the **contents** of this folder to the root of a GitHub repository, open **Actions** and run **Build Pocket Realms**. Download the `PocketRealms-0.1.0-alpha` artifact only after the workflow finishes successfully and its verification step passes.

This is an alpha build. Back up worlds before testing. The mod has not yet been verified in a live Minecraft instance.
