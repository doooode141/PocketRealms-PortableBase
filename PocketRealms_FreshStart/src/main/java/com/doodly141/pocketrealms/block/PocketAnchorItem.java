package com.doodly141.pocketrealms.block;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class PocketAnchorItem extends BlockItem {
    public PocketAnchorItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        tooltip.add(Component.translatable("tooltip.pocketrealms.anchor.line1"));
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("pocket_id")) {
                tooltip.add(Component.translatable("tooltip.pocketrealms.anchor.bound", tag.getInt("pocket_id")));
            } else {
                tooltip.add(Component.translatable("tooltip.pocketrealms.anchor.line2"));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.pocketrealms.anchor.line2"));
        }
        super.appendHoverText(stack, context, tooltip, flags);
    }
}
