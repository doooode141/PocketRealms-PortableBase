package com.doodly141.pocketrealms.block;

import com.doodly141.pocketrealms.PocketRealms;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

/** Registers codecs used by Minecraft's block type registry. */
public final class ModBlockTypes {
    public static final DeferredRegister<MapCodec<? extends Block>> BLOCK_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_TYPE, PocketRealms.MOD_ID);

    public static final Supplier<MapCodec<PocketAnchorBlock>> POCKET_ANCHOR = BLOCK_TYPES.register(
            "pocket_anchor", () -> BlockBehaviour.simpleCodec(PocketAnchorBlock::new)
    );

    public static final Supplier<MapCodec<PocketGatewayBlock>> POCKET_GATEWAY = BLOCK_TYPES.register(
            "pocket_gateway", () -> BlockBehaviour.simpleCodec(PocketGatewayBlock::new)
    );

    private ModBlockTypes() {}
}
