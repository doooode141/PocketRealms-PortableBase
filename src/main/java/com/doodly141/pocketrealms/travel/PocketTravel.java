package com.doodly141.pocketrealms.travel;

import com.doodly141.pocketrealms.PocketRealms;
import com.doodly141.pocketrealms.block.ModBlocks;
import com.doodly141.pocketrealms.data.PocketWorldData;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class PocketTravel {
    public static final ResourceKey<Level> POCKET_DIMENSION = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(PocketRealms.MOD_ID, "pockets")
    );

    private static final int PLOT_SPACING = 300;
    private static final int PLOT_SIZE = 30;
    private static final int FLOOR_Y = 64;
    private static final int SPAWN_Y = FLOOR_Y + 1;

    public static void enterPocket(ServerPlayer player, int pocketId) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        PocketWorldData data = PocketWorldData.get(server.overworld());
        if (!data.ownsPocket(player.getUUID(), pocketId)) {
            player.sendSystemMessage(Component.literal("You do not own that pocket."));
            return;
        }

        ServerLevel pocketLevel = server.getLevel(POCKET_DIMENSION);
        if (pocketLevel == null) {
            player.sendSystemMessage(Component.literal("Pocket Realm dimension is not loaded. Make sure the mod is installed and restart the world."));
            return;
        }

        if (!player.level().dimension().equals(POCKET_DIMENSION)) {
            data.saveReturnPoint(player);
        }

        ensurePlot(pocketLevel, data, pocketId);
        double x = pocketId * (double) PLOT_SPACING;
        player.teleportTo(pocketLevel, x + 0.5, SPAWN_Y, 0.5, Set.of(), player.getYRot(), player.getXRot());
        player.sendSystemMessage(Component.literal("Welcome home — Pocket " + (data.getOwnedPockets(player.getUUID()).indexOf(pocketId) + 1) + "/5."));
    }

    public static void visitNextPocket(ServerPlayer player) {
        PocketWorldData data = PocketWorldData.get(player.serverLevel());
        List<Integer> pockets = data.getOwnedPockets(player.getUUID());
        if (pockets.isEmpty()) {
            player.sendSystemMessage(Component.literal("You have no Pocket Anchors yet."));
            return;
        }
        int current = nearestPocketId(player.getX(), pockets);
        int index = pockets.indexOf(current);
        int next = pockets.get((index + 1 + pockets.size()) % pockets.size());
        if (next == current) {
            player.sendSystemMessage(Component.literal("This is your only pocket so far. Craft another Pocket Anchor to make a second home."));
            return;
        }
        enterPocket(player, next);
    }

    /** Returns players safely to their nearest home if they fall off a plot into the void. */
    public static void rescueFromVoid(ServerPlayer player) {
        if (!player.serverLevel().dimension().equals(POCKET_DIMENSION) || player.getY() > -16.0) return;
        PocketWorldData data = PocketWorldData.get(player.serverLevel());
        List<Integer> pockets = data.getOwnedPockets(player.getUUID());
        if (pockets.isEmpty()) {
            returnToLastPlace(player);
            return;
        }
        int nearest = nearestPocketId(player.getX(), pockets);
        enterPocket(player, nearest);
        player.sendSystemMessage(Component.literal("Pocket safety net activated. You were returned to your home platform."));
    }

    public static void returnToLastPlace(ServerPlayer player) {
        if (!player.level().dimension().equals(POCKET_DIMENSION)) {
            player.sendSystemMessage(Component.literal("You are not inside a Pocket Realm."));
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) return;
        PocketWorldData data = PocketWorldData.get(server.overworld());
        PocketWorldData.ReturnPoint point = data.getReturnPoint(player.getUUID());
        ServerLevel target = server.overworld();
        double x = target.getSharedSpawnPos().getX() + 0.5;
        double y = target.getSharedSpawnPos().getY() + 1.0;
        double z = target.getSharedSpawnPos().getZ() + 0.5;
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        if (point != null) {
            try {
                ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(point.dimension()));
                ServerLevel savedLevel = server.getLevel(dimensionKey);
                if (savedLevel != null) {
                    target = savedLevel;
                    x = point.x();
                    y = point.y();
                    z = point.z();
                    yaw = point.yaw();
                    pitch = point.pitch();
                }
            } catch (RuntimeException ignored) {
                // If the old dimension is unavailable, safely return to the Overworld spawn.
            }
        }

        player.teleportTo(target, x, y, z, Set.of(), yaw, pitch);
        player.sendSystemMessage(Component.literal("Returned from your Pocket Realm."));
    }

    private static int nearestPocketId(double playerX, List<Integer> pockets) {
        int nearest = pockets.getFirst();
        double bestDistance = Double.MAX_VALUE;
        for (int id : pockets) {
            double centerX = id * (double) PLOT_SPACING;
            double distance = Math.abs(playerX - centerX);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = id;
            }
        }
        return nearest;
    }

    private static void ensurePlot(ServerLevel level, PocketWorldData data, int pocketId) {
        if (data.isPlotGenerated(pocketId)) return;
        int centerX = pocketId * PLOT_SPACING;
        int half = PLOT_SIZE / 2;

        // The 30x30 usable floor is surrounded by a one-block perimeter wall.
        for (int dx = -half - 1; dx <= half; dx++) {
            for (int dz = -half - 1; dz <= half; dz++) {
                boolean edge = dx == -half - 1 || dx == half || dz == -half - 1 || dz == half;
                BlockPos floor = new BlockPos(centerX + dx, FLOOR_Y, dz);
                level.setBlock(floor, (edge ? Blocks.POLISHED_ANDESITE : Blocks.SMOOTH_STONE).defaultBlockState(), 3);
                if (edge) {
                    level.setBlock(floor.above(), Blocks.POLISHED_ANDESITE.defaultBlockState(), 3);
                }
            }
        }

        // A free built-in gateway is included on the front edge of every new plot.
        level.setBlock(new BlockPos(centerX, SPAWN_Y, half - 2), ModBlocks.POCKET_GATEWAY.get().defaultBlockState(), 3);
        level.setBlock(new BlockPos(centerX - half + 1, SPAWN_Y, -half + 1), Blocks.LANTERN.defaultBlockState(), 3);
        level.setBlock(new BlockPos(centerX + half - 2, SPAWN_Y, -half + 1), Blocks.LANTERN.defaultBlockState(), 3);
        level.setBlock(new BlockPos(centerX - half + 1, SPAWN_Y, half - 2), Blocks.LANTERN.defaultBlockState(), 3);
        level.setBlock(new BlockPos(centerX + half - 2, SPAWN_Y, half - 2), Blocks.LANTERN.defaultBlockState(), 3);

        data.markPlotGenerated(pocketId);
    }

    private PocketTravel() {}
}
