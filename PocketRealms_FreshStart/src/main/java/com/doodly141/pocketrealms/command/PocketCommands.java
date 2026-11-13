package com.doodly141.pocketrealms.command;

import com.doodly141.pocketrealms.block.PocketAnchorBlockEntity;
import com.doodly141.pocketrealms.data.PocketWorldData;
import com.doodly141.pocketrealms.travel.PocketTravel;
import java.util.List;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class PocketCommands {
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("pockets")
                .executes(context -> listPockets(context.getSource().getPlayerOrException()))
                .then(Commands.literal("visit")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 5))
                                .executes(context -> visitPocket(
                                        context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "slot")))))
                .then(Commands.literal("recover")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 5))
                                .executes(context -> recoverAnchor(
                                        context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "slot")))))
                .then(Commands.literal("return")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            PocketTravel.returnToLastPlace(player);
                            return 1;
                        })));
    }

    private static int listPockets(ServerPlayer player) {
        List<Integer> ids = PocketWorldData.get(player.serverLevel()).getOwnedPockets(player.getUUID());
        if (ids.isEmpty()) {
            player.sendSystemMessage(Component.literal("You have no Pocket Anchors yet. Craft and place one to claim Pocket 1/5."));
            return 0;
        }
        StringBuilder message = new StringBuilder("Your Pocket Realms: ");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) message.append(" | ");
            message.append("Pocket ").append(i + 1).append(" (plot ").append(ids.get(i)).append(")");
        }
        player.sendSystemMessage(Component.literal(message.toString()));
        player.sendSystemMessage(Component.literal("Use /pockets visit <1-" + ids.size() + "> or /pockets recover <slot>."));
        return ids.size();
    }

    private static int visitPocket(ServerPlayer player, int slot) {
        List<Integer> ids = PocketWorldData.get(player.serverLevel()).getOwnedPockets(player.getUUID());
        if (slot < 1 || slot > ids.size()) {
            player.sendSystemMessage(Component.literal("You do not own Pocket slot " + slot + "."));
            return 0;
        }
        PocketTravel.enterPocket(player, ids.get(slot - 1));
        return 1;
    }

    private static int recoverAnchor(ServerPlayer player, int slot) {
        List<Integer> ids = PocketWorldData.get(player.serverLevel()).getOwnedPockets(player.getUUID());
        if (slot < 1 || slot > ids.size()) {
            player.sendSystemMessage(Component.literal("You do not own Pocket slot " + slot + "."));
            return 0;
        }
        ItemStack anchor = PocketAnchorBlockEntity.createBoundItem(player.getUUID(), ids.get(slot - 1));
        if (!player.getInventory().add(anchor)) player.drop(anchor, false);
        player.sendSystemMessage(Component.literal("Recovered the bound Pocket Anchor for slot " + slot + "."));
        return 1;
    }

    private PocketCommands() {}
}
