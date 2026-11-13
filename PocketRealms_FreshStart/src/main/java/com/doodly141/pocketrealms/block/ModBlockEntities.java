package com.doodly141.pocketrealms.block;

import com.doodly141.pocketrealms.PocketRealms;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, PocketRealms.MOD_ID);

    public static final Supplier<BlockEntityType<PocketAnchorBlockEntity>> POCKET_ANCHOR = BLOCK_ENTITIES.register(
            "pocket_anchor",
            () -> BlockEntityType.Builder.of(PocketAnchorBlockEntity::new, ModBlocks.POCKET_ANCHOR.get()).build(null)
    );

    private ModBlockEntities() {}
}
