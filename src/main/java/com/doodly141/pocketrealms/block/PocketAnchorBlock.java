package com.doodly141.pocketrealms.block;

import com.mojang.serialization.MapCodec;
import com.doodly141.pocketrealms.data.PocketWorldData;
import com.doodly141.pocketrealms.travel.PocketTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.Nullable;

public class PocketAnchorBlock extends Block implements EntityBlock {
    public PocketAnchorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<PocketAnchorBlock> codec() {
        return ModBlockTypes.POCKET_ANCHOR.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PocketAnchorBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel
                && placer instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof PocketAnchorBlockEntity blockEntity) {
            blockEntity.initializeFromItem(stack, serverPlayer, serverLevel);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.CONSUME;
        }
        if (!(level.getBlockEntity(pos) instanceof PocketAnchorBlockEntity blockEntity)) {
            return InteractionResult.CONSUME;
        }

        if (!blockEntity.isBound()) {
            blockEntity.initializeFromItem(ItemStack.EMPTY, serverPlayer, serverLevel);
        }
        if (!blockEntity.isBound()) return InteractionResult.CONSUME;

        if (!serverPlayer.getUUID().equals(blockEntity.getOwner())) {
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("This Pocket Anchor belongs to another player."));
            return InteractionResult.CONSUME;
        }
        PocketTravel.enterPocket(serverPlayer, blockEntity.getPocketId());
        return InteractionResult.CONSUME;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.isCreative()
                && level.getBlockEntity(pos) instanceof PocketAnchorBlockEntity blockEntity) {
            Block.popResource(level, pos, blockEntity.createBoundItem());
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
