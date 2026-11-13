package com.doodly141.pocketrealms;

import com.doodly141.pocketrealms.block.ModBlockEntities;
import com.doodly141.pocketrealms.block.ModBlockTypes;
import com.doodly141.pocketrealms.block.ModBlocks;
import com.doodly141.pocketrealms.block.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import com.doodly141.pocketrealms.command.PocketCommands;
import com.doodly141.pocketrealms.protection.PocketProtection;
import com.doodly141.pocketrealms.protection.PocketSafety;

@Mod(PocketRealms.MOD_ID)
public final class PocketRealms {
    public static final String MOD_ID = "pocketrealms";

    public PocketRealms(IEventBus modEventBus) {
        ModBlockTypes.BLOCK_TYPES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        modEventBus.addListener(ModItems::addCreative);
        NeoForge.EVENT_BUS.addListener(PocketCommands::register);
        NeoForge.EVENT_BUS.addListener(PocketProtection::onBreak);
        NeoForge.EVENT_BUS.addListener(PocketProtection::onPlace);
        NeoForge.EVENT_BUS.addListener(PocketProtection::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(PocketSafety::onPlayerTick);
    }
}
