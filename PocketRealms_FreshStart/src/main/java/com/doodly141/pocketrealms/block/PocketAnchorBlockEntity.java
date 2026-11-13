package com.doodly141.pocketrealms.block;

import com.doodly141.pocketrealms.data.PocketWorldData;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
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

    public void bind(UUID owner, int pocketId) {
        this.owner = owner;
        this.pocketId = pocketId;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void initializeFromItem(ItemStack stack, ServerPlayer placer, ServerLevel serverLevel) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("pocket_id") && tag.contains("owner")) {
                try {
                    int storedId = tag.getInt("pocket_id");
                    UUID storedOwner = UUID.fromString(tag.getString("owner"));
                    if (storedId >= 0) {
                        bind(storedOwner, storedId);
                        return;
                    }
                } catch (IllegalArgumentException ignored) {
                    // Invalid item data is treated as a new, unbound anchor.
                }
            }
        }

        int allocated = PocketWorldData.get(serverLevel).allocatePocket(placer.getUUID());
        if (allocated >= 0) {
            bind(placer.getUUID(), allocated);
            placer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "Pocket Anchor claimed: Pocket " + (PocketWorldData.get(serverLevel).getOwnedPockets(placer.getUUID()).indexOf(allocated) + 1) + "/5."
            ));
        } else {
            placer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "You already have 5 Pocket Anchors. This one stays unbound."
            ));
        }
    }

    public ItemStack createBoundItem() {
        return createBoundItem(owner, pocketId);
    }

    public static ItemStack createBoundItem(UUID owner, int pocketId) {
        ItemStack stack = new ItemStack(ModItems.POCKET_ANCHOR.get());
        if (owner != null && pocketId >= 0) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("pocket_id", pocketId);
            tag.putString("owner", owner.toString());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return stack;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("pocket_id", pocketId);
        if (owner != null) {
            tag.putString("owner", owner.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pocketId = tag.getInt("pocket_id");
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
