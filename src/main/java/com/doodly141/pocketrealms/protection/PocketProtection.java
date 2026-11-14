package com.doodly141.pocketrealms.protection;

import com.doodly141.pocketrealms.data.PocketWorldData;
import com.doodly141.pocketrealms.block.PocketAnchorBlockEntity;
import com.doodly141.pocketrealms.travel.PocketTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import java.util.UUID;

/** Lightweight owner checks for player block interactions inside private pocket plots. */
public final class PocketProtection {
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.getBlockEntity(event.getPos()) instanceof PocketAnchorBlockEntity anchor
                && anchor.isBound() && !event.getPlayer().getUUID().equals(anchor.getOwner())) {
            event.setCanceled(true);
            message(event.getPlayer());
            return;
        }
        if (isForeignPlot(level, event.getPos(), event.getPlayer())) {
            event.setCanceled(true);
            message(event.getPlayer());
        }
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (isForeignPlot(level, event.getPos(), player)) {
            event.setCanceled(true);
            message(player);
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (isForeignPlot(level, event.getPos(), event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            message(event.getEntity());
        }
    }

    private static boolean isForeignPlot(ServerLevel level, BlockPos pos, Player player) {
        if (!level.dimension().equals(PocketTravel.POCKET_DIMENSION)) return false;
        UUID owner = PocketWorldData.get(level).getPocketOwnerAt(pos.getX(), pos.getZ());
        return owner != null && !owner.equals(player.getUUID());
    }

    private static void message(Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.literal("This Pocket Realm belongs to another player."), true);
        }
    }

    private PocketProtection() {}
}
