package com.doodly141.pocketrealms.block;

import com.doodly141.pocketrealms.data.PocketWorldData;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class PocketAnchorBlockEntity extends BlockEntity {
    private int pocketId = -1;
    private UUID owner;

    public PocketAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POCKET_ANCHOR.get(), pos, state);
    }

    public int getPocketId() {
        return pocketId;
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean isBound() {
        return pocketId >= 0 && owner != null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel && isBound()) {
            // Register legacy anchors as their chunks load so middle-click copies are rejected.
            PocketWorldData.get(serverLevel).registerAnchor(owner, pocketId, serverLevel, worldPosition);
        }
    }

    public void bind(UUID owner, int pocketId) {
        this.owner = owner;
        this.pocketId = pocketId;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void initializeFromItem(ItemStack stack, ServerPlayer placer, ServerLevel serverLevel) {
        PocketWorldData data = PocketWorldData.get(serverLevel);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("pocket_id") || tag.contains("owner")) {
                try {
                    int storedId = tag.getInt("pocket_id");
                    UUID storedOwner = UUID.fromString(tag.getString("owner"));
                    if (storedId >= 0 && storedOwner.equals(placer.getUUID())
                            && data.ownsPocket(storedOwner, storedId)
                            && !data.isAnchorActive(storedId)) {
                        bind(storedOwner, storedId);
                        data.registerAnchor(storedOwner, storedId, serverLevel, worldPosition);
                        return;
                    }
                } catch (IllegalArgumentException ignored) {
                    // Invalid identity data is rejected; it must not silently claim a new home.
                }
                placer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "This Pocket Anchor identity is invalid, belongs to another player, or is already placed."
                ));
                return;
            }
        }

        int allocated = data.allocatePocket(placer.getUUID());
        if (allocated >= 0) {
            bind(placer.getUUID(), allocated);
            data.registerAnchor(placer.getUUID(), allocated, serverLevel, worldPosition);
            placer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "Pocket Anchor claimed: " + data.getHomeName(placer.getUUID(), allocated)
                            + " (" + (data.getOwnedPockets(placer.getUUID()).indexOf(allocated) + 1) + "/5)."
            ));
        } else {
            placer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "You already have 5 Pocket Anchors. This one stays unbound."
            ));
        }
    }

    public ItemStack createBoundItem() {
        String name = "Home";
        if (level instanceof ServerLevel serverLevel && isBound()) {
            name = PocketWorldData.get(serverLevel).getHomeName(owner, pocketId);
        }
        return createBoundItem(owner, pocketId, name);
    }

    public static ItemStack createBoundItem(UUID owner, int pocketId) {
        return createBoundItem(owner, pocketId, "Home");
    }

    public static ItemStack createBoundItem(UUID owner, int pocketId, String homeName) {
        ItemStack stack = new ItemStack(ModItems.POCKET_ANCHOR.get());
        if (owner != null && pocketId >= 0) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("pocket_id", pocketId);
            tag.putString("owner", owner.toString());
            tag.putString("home_name", homeName == null || homeName.isBlank() ? "Home" : homeName);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return stack;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("pocket_id", pocketId);
        if (owner != null) tag.putString("owner", owner.toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pocketId = tag.contains("pocket_id") ? tag.getInt("pocket_id") : -1;
        owner = null;
        if (tag.contains("owner")) {
            try {
                owner = UUID.fromString(tag.getString("owner"));
            } catch (IllegalArgumentException ignored) {
                owner = null;
                pocketId = -1;
            }
        }
    }
}
