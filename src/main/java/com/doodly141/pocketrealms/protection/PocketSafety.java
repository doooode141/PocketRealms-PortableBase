package com.doodly141.pocketrealms.protection;

import com.doodly141.pocketrealms.travel.PocketTravel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Prevents a fall into the void from becoming a lost-items death. */
public final class PocketSafety {
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PocketTravel.rescueFromVoid(player);
        }
    }

    private PocketSafety() {}
}
