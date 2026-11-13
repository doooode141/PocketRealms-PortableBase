package com.doodly141.pocketrealms.block;

import com.doodly141.pocketrealms.PocketRealms;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PocketRealms.MOD_ID);

    public static final DeferredBlock<PocketAnchorBlock> POCKET_ANCHOR = BLOCKS.registerBlock(
            "pocket_anchor",
            PocketAnchorBlock::new,
            BlockBehaviour.Properties.of().strength(2.0F, 4.0F).sound(SoundType.AMETHYST).noOcclusion()
    );

    public static final DeferredBlock<PocketGatewayBlock> POCKET_GATEWAY = BLOCKS.registerBlock(
            "pocket_gateway",
            PocketGatewayBlock::new,
            BlockBehaviour.Properties.of().strength(2.0F, 4.0F).sound(SoundType.AMETHYST).lightLevel(state -> 8)
    );

    private ModBlocks() {}
}
