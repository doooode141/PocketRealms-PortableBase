package com.doodly141.pocketrealms.travel;

import com.doodly141.pocketrealms.PocketRealms;
import com.doodly141.pocketrealms.block.ModBlocks;
import com.doodly141.pocketrealms.block.PocketAnchorBlockEntity;
import com.doodly141.pocketrealms.data.PocketWorldData;
import com.doodly141.pocketrealms.menu.PocketNavigatorMenu;
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
            player.sendSystemMessage(Component.literal("Pocket Realm dimension is unavailable. Restart the world and check the mod installation."));
            return;
        }

        if (!player.serverLevel().dimension().equals(POCKET_DIMENSION)) data.saveReturnPoint(player);
        ensurePlot(pocketLevel, data, pocketId);

        PocketWorldData.HomeSpawn saved = data.getHomeSpawn(pocketId);
        double x = saved == null ? pocketId * (double) PLOT_SPACING + 0.5 : saved.x();
        double y = saved == null ? SPAWN_Y : saved.y();
        double z = saved == null ? 0.5 : saved.z();
        float yaw = saved == null ? player.getYRot() : saved.yaw();
        float pitch = saved == null ? player.getXRot() : saved.pitch();
        if (!isSafePosition(pocketLevel, player, x, y, z)) {
            x = pocketId * (double) PLOT_SPACING + 0.5;
            y = SPAWN_Y;
            z = 0.5;
        }
        player.teleportTo(pocketLevel, x, y, z, Set.of(), yaw, pitch);
        player.fallDistance = 0;
        player.sendSystemMessage(Component.literal("Welcome home — " + data.getHomeName(player.getUUID(), pocketId) + "."));
    }

    public static void openNavigator(ServerPlayer player) {
        PocketNavigatorMenu.open(player);
    }

    /** Void rescue without inventory loss or fall damage. */
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
        player.fallDistance = 0;
        player.sendSystemMessage(Component.literal("Dimensional safety system returned you to your home."));
    }

    public static void returnToLastPlace(ServerPlayer player) {
        if (!player.serverLevel().dimension().equals(POCKET_DIMENSION)) {
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
                // The fallback below uses the Overworld's shared spawn.
            }
        }

        double[] safe = findSafePosition(target, player, x, y, z);
        if (safe == null) {
            target = server.overworld();
            BlockPos spawn = target.getSharedSpawnPos();
            safe = findSafePosition(target, player, spawn.getX() + 0.5, spawn.getY() + 1.0, spawn.getZ() + 0.5);
        }
        if (safe == null) {
            player.sendSystemMessage(Component.literal("No safe return point is available yet. Try /spawn or ask an administrator for help."));
            return;
        }

        player.teleportTo(target, safe[0], safe[1], safe[2], Set.of(), yaw, pitch);
        player.fallDistance = 0;
        player.sendSystemMessage(Component.literal("Returned to your saved entrance location."));
    }

    public static boolean setHomeSpawn(ServerPlayer player, int pocketId) {
        if (!player.serverLevel().dimension().equals(POCKET_DIMENSION)) return false;
        PocketWorldData data = PocketWorldData.get(player.serverLevel());
        if (!data.ownsPocket(player.getUUID(), pocketId)) return false;
        int centerX = pocketId * PLOT_SPACING;
        if (Math.abs(player.getX() - centerX) > 15 || Math.abs(player.getZ()) > 15) return false;
        data.setHomeSpawn(pocketId, player);
        return true;
    }

    private static boolean isSafePosition(ServerLevel level, ServerPlayer player, double x, double y, double z) {
        return findSafePosition(level, player, x, y, z) != null;
    }

    private static double[] findSafePosition(ServerLevel level, ServerPlayer player, double x, double y, double z) {
        BlockPos origin = BlockPos.containing(x, y, z);
        for (int radius = 0; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int dy = 0; dy <= 4; dy++) {
                        double px = origin.getX() + dx + 0.5;
                        double py = Math.max(level.getMinBuildHeight() + 1, origin.getY() + dy);
                        double pz = origin.getZ() + dz + 0.5;
                        BlockPos feet = BlockPos.containing(px, py, pz);
                        BlockPos floor = feet.below();
                        if (!level.getWorldBorder().isWithinBounds(feet)) continue;
                        if (py >= level.getMaxBuildHeight() - 1 || !level.getFluidState(feet).isEmpty()) continue;
                        if (!level.getBlockState(floor).isSolid()) continue;
                        if (level.noCollision(player, player.getBoundingBox().move(px - player.getX(),
                                py - player.getY(), pz - player.getZ()))) {
                            return new double[]{px, py, pz};
                        }
                    }
                }
            }
        }
        return null;
    }

    private static int nearestPocketId(double playerX, List<Integer> pockets) {
        int nearest = pockets.getFirst();
        double bestDistance = Double.MAX_VALUE;
        for (int id : pockets) {
            double distance = Math.abs(playerX - id * (double) PLOT_SPACING);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = id;
            }
        }
        return nearest;
    }

    private static void ensurePlot(ServerLevel level, PocketWorldData data, int pocketId) {
        if (data.isPlotGenerated(pocketId)) {
            if (!data.isPlotEnclosed(pocketId)) upgradeLegacyPlot(level, data, pocketId);
            return;
        }

        int centerX = pocketId * PLOT_SPACING;
        int min = -PLOT_SIZE / 2;
        int max = min + PLOT_SIZE - 1;
        int wallMin = min - 1;
        int wallMax = max + 1;
        int ceilingY = FLOOR_Y + 13;

        // 30x30 editable floor, surrounded by 12-block-high walls and a roof.
        for (int dx = wallMin; dx <= wallMax; dx++) {
            for (int dz = wallMin; dz <= wallMax; dz++) {
                boolean wall = dx == wallMin || dx == wallMax || dz == wallMin || dz == wallMax;
                BlockPos floor = new BlockPos(centerX + dx, FLOOR_Y, dz);
                level.setBlock(floor, (wall ? Blocks.POLISHED_DEEPSLATE : Blocks.SMOOTH_STONE).defaultBlockState(), 3);

                // Leave a 3-block-wide doorway in the +Z wall for the free gateway.
                boolean doorway = dz == wallMax && dx >= -1 && dx <= 1;
                if (wall && !doorway) {
                    for (int h = 1; h <= 12; h++) {
                        BlockPos wallPos = floor.above(h);
                        level.setBlock(wallPos, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 3);
                    }
                }
                if (wall) {
                }
            }
        }

        // Fill the entire roof, not just its perimeter, so the generated room is enclosed.
        for (int dx = wallMin; dx <= wallMax; dx++) {
            for (int dz = wallMin; dz <= wallMax; dz++) {
                level.setBlock(new BlockPos(centerX + dx, ceilingY, dz),
                        Blocks.DEEPSLATE_TILES.defaultBlockState(), 3);
            }
        }

        level.setBlock(new BlockPos(centerX, SPAWN_Y, max - 1), ModBlocks.POCKET_GATEWAY.get().defaultBlockState(), 3);
        level.setBlock(new BlockPos(centerX - 12, SPAWN_Y, -12), Blocks.LANTERN.defaultBlockState(), 3);
        level.setBlock(new BlockPos(centerX + 12, SPAWN_Y, -12), Blocks.LANTERN.defaultBlockState(), 3);
        level.setBlock(new BlockPos(centerX - 12, SPAWN_Y, 12), Blocks.LANTERN.defaultBlockState(), 3);
        level.setBlock(new BlockPos(centerX + 12, SPAWN_Y, 12), Blocks.LANTERN.defaultBlockState(), 3);
        level.setBlock(new BlockPos(centerX, FLOOR_Y + 12, 0), Blocks.SEA_LANTERN.defaultBlockState(), 3);

        data.markPlotGenerated(pocketId);
        data.markPlotEnclosed(pocketId);
    }

    /**
     * One-time, non-destructive shell migration for 0.1.0-alpha homes.
     * Only air blocks are filled; existing player blocks and machines are left untouched.
     */
    private static void upgradeLegacyPlot(ServerLevel level, PocketWorldData data, int pocketId) {
        int centerX = pocketId * PLOT_SPACING;
        int min = -PLOT_SIZE / 2;
        int max = min + PLOT_SIZE - 1;
        int wallMin = min - 1;
        int wallMax = max + 1;
        int ceilingY = FLOOR_Y + 13;

        for (int dx = wallMin; dx <= wallMax; dx++) {
            for (int dz = wallMin; dz <= wallMax; dz++) {
                boolean wall = dx == wallMin || dx == wallMax || dz == wallMin || dz == wallMax;
                if (wall) {
                    for (int h = 2; h <= 12; h++) {
                        BlockPos wallPos = new BlockPos(centerX + dx, FLOOR_Y + h, dz);
                        if (level.getBlockState(wallPos).isAir()) {
                            level.setBlock(wallPos, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 3);
                        }
                    }
                }
                BlockPos roof = new BlockPos(centerX + dx, ceilingY, dz);
                if (level.getBlockState(roof).isAir()) {
                    level.setBlock(roof, Blocks.DEEPSLATE_TILES.defaultBlockState(), 3);
                }
            }
        }

        // Old generated wall blocks at this doorway position are removed only when they
        // are still the original polished-andesite perimeter block.
        BlockPos doorway = new BlockPos(centerX, FLOOR_Y + 1, wallMax);
        if (level.getBlockState(doorway).is(Blocks.POLISHED_ANDESITE)) {
            level.removeBlock(doorway, false);
        }

        BlockPos gateway = new BlockPos(centerX, SPAWN_Y, max - 1);
        if (level.getBlockState(gateway).isAir()) {
            level.setBlock(gateway, ModBlocks.POCKET_GATEWAY.get().defaultBlockState(), 3);
        }

        data.markPlotEnclosed(pocketId);
    }

    private PocketTravel() {}
}
