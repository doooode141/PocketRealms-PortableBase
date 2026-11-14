package com.doodly141.pocketrealms.command;

import com.doodly141.pocketrealms.block.PocketAnchorBlockEntity;
import com.doodly141.pocketrealms.block.ModItems;
import com.doodly141.pocketrealms.data.PocketWorldData;
import com.doodly141.pocketrealms.travel.PocketTravel;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class PocketCommands {
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("pockets")
                .executes(context -> listPockets(context.getSource().getPlayerOrException()))
                .then(Commands.literal("list")
                        .executes(context -> listPockets(context.getSource().getPlayerOrException())))
                .then(Commands.literal("visit")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 5))
                                .executes(context -> visitPocket(context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "slot")))))
                .then(Commands.literal("recover")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 5))
                                .executes(context -> recoverAnchor(context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "slot")))))
                .then(Commands.literal("rename")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 5))
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(context -> renamePocket(context.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(context, "slot"),
                                                StringArgumentType.getString(context, "name"))))))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 5))
                                .executes(context -> setHomeSpawn(context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "slot")))))
                .then(Commands.literal("mailbox")
                        .executes(context -> claimMailbox(context.getSource().getPlayerOrException())))
                .then(Commands.literal("return")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            PocketTravel.returnToLastPlace(player);
                            return 1;
                        })));
    }

    private static int listPockets(ServerPlayer player) {
        PocketWorldData data = PocketWorldData.get(player.serverLevel());
        List<Integer> ids = data.getOwnedPockets(player.getUUID());
        if (ids.isEmpty()) {
            player.sendSystemMessage(Component.literal("You have no Pocket Realms yet. Place a Pocket Anchor to claim Home 1/5."));
            return 0;
        }
        player.sendSystemMessage(Component.literal("§bPOCKET REALMS §7— " + ids.size() + "/5 homes"));
        for (int i = 0; i < ids.size(); i++) {
            int id = ids.get(i);
            String status = data.isAnchorActive(id) ? "Anchor placed" : "Anchor recoverable";
            player.sendSystemMessage(Component.literal("§f" + (i + 1) + ". §b" + data.getHomeName(player.getUUID(), id)
                    + " §8(" + status + ")"));
        }
        player.sendSystemMessage(Component.literal("Use /pockets visit <slot>, /pockets return, /pockets rename <slot> <name>, /pockets spawn <slot>, or /pockets recover <slot>."));
        return ids.size();
    }

    private static int visitPocket(ServerPlayer player, int slot) {
        List<Integer> ids = PocketWorldData.get(player.serverLevel()).getOwnedPockets(player.getUUID());
        if (slot < 1 || slot > ids.size()) {
            player.sendSystemMessage(Component.literal("You do not own Home " + slot + "."));
            return 0;
        }
        PocketTravel.enterPocket(player, ids.get(slot - 1));
        return 1;
    }

    private static int recoverAnchor(ServerPlayer player, int slot) {
        PocketWorldData data = PocketWorldData.get(player.serverLevel());
        List<Integer> ids = data.getOwnedPockets(player.getUUID());
        if (slot < 1 || slot > ids.size()) {
            player.sendSystemMessage(Component.literal("You do not own Home " + slot + "."));
            return 0;
        }
        int id = ids.get(slot - 1);
        if (data.isAnchorActive(id)) {
            player.sendSystemMessage(Component.literal("That home's anchor is already placed. Duplicate recovery was blocked."));
            return 0;
        }
        if (hasBoundAnchorInInventory(player, id)) {
            data.removeRecovery(player.getUUID(), id);
            player.sendSystemMessage(Component.literal("You already have this home's Pocket Anchor in your inventory."));
            return 1;
        }
        ItemStack anchor = PocketAnchorBlockEntity.createBoundItem(player.getUUID(), id,
                data.getHomeName(player.getUUID(), id));
        if (player.getInventory().add(anchor)) {
            data.removeRecovery(player.getUUID(), id);
            player.sendSystemMessage(Component.literal("Recovered " + data.getHomeName(player.getUUID(), id) + "."));
        } else {
            data.addRecovery(player.getUUID(), id);
            player.sendSystemMessage(Component.literal("Your inventory is full. The anchor is safely stored in /pockets mailbox."));
        }
        return 1;
    }

    private static int renamePocket(ServerPlayer player, int slot, String name) {
        PocketWorldData data = PocketWorldData.get(player.serverLevel());
        List<Integer> ids = data.getOwnedPockets(player.getUUID());
        if (slot < 1 || slot > ids.size()) {
            player.sendSystemMessage(Component.literal("You do not own Home " + slot + "."));
            return 0;
        }
        if (!data.renamePocket(player.getUUID(), ids.get(slot - 1), name)) {
            player.sendSystemMessage(Component.literal("Home names must be 1–24 characters."));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Home " + slot + " is now named \"" + name.trim() + "\"."));
        return 1;
    }

    private static int setHomeSpawn(ServerPlayer player, int slot) {
        List<Integer> ids = PocketWorldData.get(player.serverLevel()).getOwnedPockets(player.getUUID());
        if (slot < 1 || slot > ids.size()) {
            player.sendSystemMessage(Component.literal("You do not own Home " + slot + "."));
            return 0;
        }
        if (!PocketTravel.setHomeSpawn(player, ids.get(slot - 1))) {
            player.sendSystemMessage(Component.literal("Stand inside the selected home in the Pocket Realm to set its spawn."));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Saved the spawn point for Home " + slot + "."));
        return 1;
    }

    private static int claimMailbox(ServerPlayer player) {
        PocketWorldData data = PocketWorldData.get(player.serverLevel());
        List<Integer> ids = data.getRecoveryMailbox(player.getUUID());
        if (ids.isEmpty()) {
            player.sendSystemMessage(Component.literal("Your Pocket Anchor recovery mailbox is empty."));
            return 0;
        }
        int claimed = 0;
        for (int id : ids) {
            if (!data.ownsPocket(player.getUUID(), id) || data.isAnchorActive(id)) {
                data.removeRecovery(player.getUUID(), id);
                continue;
            }
            if (hasBoundAnchorInInventory(player, id)) {
                data.removeRecovery(player.getUUID(), id);
                continue;
            }
            ItemStack anchor = PocketAnchorBlockEntity.createBoundItem(player.getUUID(), id,
                    data.getHomeName(player.getUUID(), id));
            if (!player.getInventory().add(anchor)) break;
            data.removeRecovery(player.getUUID(), id);
            claimed++;
        }
        player.sendSystemMessage(Component.literal("Recovered " + claimed + " Pocket Anchor(s)."));
        return claimed;
    }

    private static boolean hasBoundAnchorInInventory(ServerPlayer player, int pocketId) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(ModItems.POCKET_ANCHOR.get())) continue;
            CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
            if (custom == null) continue;
            CompoundTag tag = custom.copyTag();
            if (tag.getInt("pocket_id") == pocketId
                    && player.getUUID().toString().equals(tag.getString("owner"))) return true;
        }
        return false;
    }

    private PocketCommands() {}
}
