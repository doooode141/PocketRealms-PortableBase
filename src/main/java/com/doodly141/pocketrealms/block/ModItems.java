package com.doodly141.pocketrealms.block;

import com.doodly141.pocketrealms.PocketRealms;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PocketRealms.MOD_ID);

    public static final DeferredItem<PocketAnchorItem> POCKET_ANCHOR = ITEMS.register(
            "pocket_anchor",
            () -> new PocketAnchorItem(ModBlocks.POCKET_ANCHOR.get(), new Item.Properties().stacksTo(1))
    );

    public static final DeferredItem<BlockItem> POCKET_GATEWAY = ITEMS.register(
            "pocket_gateway",
            () -> new BlockItem(ModBlocks.POCKET_GATEWAY.get(), new Item.Properties().stacksTo(16))
    );

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS || event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(POCKET_ANCHOR);
            event.accept(POCKET_GATEWAY);
        }
    }

    private ModItems() {}
}
