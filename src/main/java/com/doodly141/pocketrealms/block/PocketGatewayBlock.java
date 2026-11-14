package com.doodly141.pocketrealms.block;

import com.mojang.serialization.MapCodec;
import com.doodly141.pocketrealms.travel.PocketTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class PocketGatewayBlock extends Block {
    public PocketGatewayBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<PocketGatewayBlock> codec() {
        return ModBlockTypes.POCKET_GATEWAY.get();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) {
            PocketTravel.openNavigator(serverPlayer);
        }
        return InteractionResult.CONSUME;
    }
}
