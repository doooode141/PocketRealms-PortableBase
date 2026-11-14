# Pocket Realms 0.2.0

Minecraft Java 1.21.1 · NeoForge 21.1.265 · Java 21

Pocket Realms lets each player carry up to five homes in one shared pocket dimension. Each home has a 30×30 editable floor, and plot centers are spaced 300 blocks apart.

## Navigation

Right-click a Dimension Shift Console (the former Pocket Gateway) to open the five-slot navigator. Select any owned home directly or choose Return to Last Location.

Commands:
- /pockets or /pockets list
- /pockets visit <slot>
- /pockets return
- /pockets recover <slot>
- /pockets rename <slot> <name>
- /pockets spawn <slot>
- /pockets mailbox

## Portable anchors

A Pocket Anchor keeps the identity of its original home when broken and replaced. It drops in both Survival and Creative. Creative placement consumes one anchor item. Bound identities are checked on the server to prevent another player or a duplicate from placing the same home twice.

Anchor drops resist ordinary damage, fire/lava, and despawning. If an anchor falls into the void, it is returned to the owner or stored in the persistent recovery mailbox.

## Existing worlds

The update retains the existing pocketrealms_world_data keys (next_pocket_id, owners, returns, and generated_plots). New home names, spawn points, active anchors, and mailbox entries are additive. Existing generated plots are never regenerated, so old player builds are not overwritten.

Back up your world before installing any mod update. The GitHub build checks compilation and packaged mod classes/assets; actual gameplay should still be tested in Minecraft before using it on a valuable world.
