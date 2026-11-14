package com.doodly141.pocketrealms.block;

import com.doodly141.pocketrealms.data.PocketWorldData;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Bound Pocket Anchor item with placement validation and drop recovery. */
public class PocketAnchorItem extends BlockItem {
    public PocketAnchorItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        Level level = context.getLevel();

        if (!level.isClientSide && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            PocketWorldData data = PocketWorldData.get(serverLevel);
            CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
            if (custom != null) {
                CompoundTag tag = custom.copyTag();
                boolean hasId = tag.contains("pocket_id");
                boolean hasOwner = tag.contains("owner");
                if (hasId || hasOwner) {
                    try {
                        int id = tag.getInt("pocket_id");
                        UUID owner = UUID.fromString(tag.getString("owner"));
                        if (!hasId || !hasOwner || id < 0 || !owner.equals(serverPlayer.getUUID())
                                || !data.ownsPocket(owner, id) || data.isAnchorActive(id)) {
                            serverPlayer.sendSystemMessage(Component.literal(
                                    "That Pocket Anchor is invalid, belongs to someone else, or is already placed."
                            ));
                            return InteractionResult.FAIL;
                        }
                    } catch (IllegalArgumentException exception) {
                        serverPlayer.sendSystemMessage(Component.literal("That Pocket Anchor has invalid identity data."));
                        return InteractionResult.FAIL;
                    }
                } else if (data.getOwnedPockets(serverPlayer.getUUID()).size() >= 5) {
                    serverPlayer.sendSystemMessage(Component.literal("You already own all five Pocket Realms."));
                    return InteractionResult.FAIL;
                }
            } else if (data.getOwnedPockets(serverPlayer.getUUID()).size() >= 5) {
                serverPlayer.sendSystemMessage(Component.literal("You already own all five Pocket Realms."));
                return InteractionResult.FAIL;
            }
        }

        InteractionResult result = super.useOn(context);
        // Vanilla does not consume block items in Creative. Pocket Anchors deliberately do.
        if (!level.isClientSide && player != null && player.getAbilities().instabuild
                && result.consumesAction() && !stack.isEmpty()) {
            stack.shrink(1);
        }
        return result;
    }

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canBeHurtBy(ItemStack stack, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    public void onDestroyed(ItemEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (entity.level().isClientSide || !(entity.level() instanceof ServerLevel level)) return;
        ItemStack stack = entity.getItem();
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return;
        CompoundTag tag = custom.copyTag();
        if (!tag.contains("pocket_id") || !tag.contains("owner")) return;
        try {
            UUID owner = UUID.fromString(tag.getString("owner"));
            int id = tag.getInt("pocket_id");
            PocketWorldData.get(level).addRecovery(owner, id);
        } catch (IllegalArgumentException ignored) {
            // Invalid item identity is never allowed to create recovery entries.
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.level().isClientSide || !(entity.level() instanceof ServerLevel level)) return false;
        if (entity.getY() >= level.getMinY() + 2) return false;

        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return false;
        CompoundTag tag = custom.copyTag();
        if (!tag.contains("pocket_id") || !tag.contains("owner")) return false;

        try {
            int pocketId = tag.getInt("pocket_id");
            UUID owner = UUID.fromString(tag.getString("owner"));
            PocketWorldData data = PocketWorldData.get(level);
            if (!data.ownsPocket(owner, pocketId)) return false;

            ServerPlayer onlineOwner = level.getServer().getPlayerList().getPlayer(owner);
            ItemStack recovered = stack.copy();
            if (onlineOwner != null && onlineOwner.getInventory().add(recovered)) {
                onlineOwner.sendSystemMessage(Component.literal("Your Pocket Anchor was recovered from the void."));
            } else {
                // One mailbox entry per home ID makes repeated entity callbacks idempotent.
                data.addRecovery(owner, pocketId);
            }
            entity.discard();
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        tooltip.add(Component.translatable("tooltip.pocketrealms.anchor.line1"));
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("pocket_id") && tag.contains("owner")) {
                String name = tag.contains("home_name") ? tag.getString("home_name") : "Bound Home";
                tooltip.add(Component.literal("§b" + name));
                tooltip.add(Component.literal("§8Bound to its original owner"));
            } else {
                tooltip.add(Component.translatable("tooltip.pocketrealms.anchor.line2"));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.pocketrealms.anchor.line2"));
        }
        super.appendHoverText(stack, context, tooltip, flags);
    }
}
