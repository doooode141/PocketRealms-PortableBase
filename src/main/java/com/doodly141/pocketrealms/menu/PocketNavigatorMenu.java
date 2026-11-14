package com.doodly141.pocketrealms.menu;

import com.doodly141.pocketrealms.data.PocketWorldData;
import com.doodly141.pocketrealms.travel.PocketTravel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** A small server-authoritative five-home navigator. The GUI only sends slot clicks. */
public class PocketNavigatorMenu extends AbstractContainerMenu {
    private final SimpleContainer buttons = new SimpleContainer(27);

    public PocketNavigatorMenu(int containerId, Inventory playerInventory) {
        this(ModMenus.NAVIGATOR.get(), containerId, playerInventory);
    }

    private PocketNavigatorMenu(MenuType<?> type, int containerId, Inventory playerInventory) {
        super(type, containerId);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(buttons, col + row * 9, 20 + col * 20, 40 + row * 20) {
                    @Override public boolean mayPlace(ItemStack stack) { return false; }
                    // Clicks are intercepted by clicked(); the server never removes these display items.
                    @Override public boolean mayPickup(Player player) { return true; }
                });
            }
        }
    }

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, opener) -> {
                    PocketNavigatorMenu menu = new PocketNavigatorMenu(id, inventory);
                    if (opener instanceof ServerPlayer serverPlayer) menu.populate(serverPlayer);
                    return menu;
                },
                Component.literal("POCKET REALMS — DIMENSIONAL NAVIGATOR")
        ));
    }

    private void populate(ServerPlayer player) {
        PocketWorldData data = PocketWorldData.get(player.serverLevel());
        var owned = data.getOwnedPockets(player.getUUID());
        int currentId = -1;
        if (player.serverLevel().dimension().equals(PocketTravel.POCKET_DIMENSION) && !owned.isEmpty()) {
            currentId = owned.get(0);
            double best = Double.MAX_VALUE;
            for (int id : owned) {
                double distance = Math.abs(player.getX() - id * 300.0);
                if (distance < best) { best = distance; currentId = id; }
            }
        }

        net.minecraft.world.item.Item[] icons = {
                Items.NETHER_STAR, Items.ENDER_EYE, Items.AMETHYST_SHARD,
                Items.BEACON, Items.PRISMARINE_CRYSTALS
        };
        for (int slot = 0; slot < 5; slot++) {
            if (slot < owned.size()) {
                int id = owned.get(slot);
                String name = data.getHomeName(player.getUUID(), id);
                ItemStack icon = new ItemStack(icons[slot]);
                icon.set(DataComponents.CUSTOM_NAME, Component.literal((id == currentId ? "§b★ " : "§f") + name));
                buttons.setItem(10 + slot, icon);
            } else {
                ItemStack locked = new ItemStack(Items.GRAY_DYE);
                locked.set(DataComponents.CUSTOM_NAME, Component.literal("§8Home " + (slot + 1) + " — Locked"));
                buttons.setItem(10 + slot, locked);
            }
        }

        ItemStack returnButton = new ItemStack(Items.ENDER_PEARL);
        returnButton.set(DataComponents.CUSTOM_NAME, Component.literal("§bRETURN TO LAST LOCATION"));
        buttons.setItem(22, returnButton);

        ItemStack closeButton = new ItemStack(Items.BARRIER);
        closeButton.set(DataComponents.CUSTOM_NAME, Component.literal("§cCLOSE"));
        buttons.setItem(26, closeButton);
        broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType != ClickType.PICKUP || button != 0 || !(player instanceof ServerPlayer serverPlayer)) return;
        if (slotId >= 10 && slotId <= 14) {
            var homes = PocketWorldData.get(serverPlayer.serverLevel()).getOwnedPockets(serverPlayer.getUUID());
            int selected = slotId - 10;
            if (selected >= homes.size()) {
                serverPlayer.sendSystemMessage(Component.literal("That home slot is not unlocked yet."));
                return;
            }
            int pocketId = homes.get(selected);
            serverPlayer.closeContainer();
            PocketTravel.enterPocket(serverPlayer, pocketId);
        } else if (slotId == 22) {
            serverPlayer.closeContainer();
            PocketTravel.returnToLastPlace(serverPlayer);
        } else if (slotId == 26) {
            serverPlayer.closeContainer();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
