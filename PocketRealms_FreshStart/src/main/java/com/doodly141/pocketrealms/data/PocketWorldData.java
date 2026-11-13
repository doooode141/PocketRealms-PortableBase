package com.doodly141.pocketrealms.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

public class PocketWorldData extends SavedData {
    private static final String DATA_NAME = "pocketrealms_world_data";
    private static final int MAX_POCKETS_PER_PLAYER = 5;

    private final Map<UUID, List<Integer>> ownedPockets = new HashMap<>();
    private final Map<UUID, ReturnPoint> returnPoints = new HashMap<>();
    private final Set<Integer> generatedPlots = new HashSet<>();
    private int nextPocketId = 0;

    public static PocketWorldData get(ServerLevel anyLevel) {
        return anyLevel.getServer().overworld().getDataStorage().computeIfAbsent(
                new Factory<>(PocketWorldData::new, PocketWorldData::load), DATA_NAME
        );
    }

    public int allocatePocket(UUID owner) {
        List<Integer> list = ownedPockets.computeIfAbsent(owner, ignored -> new ArrayList<>());
        if (list.size() >= MAX_POCKETS_PER_PLAYER) return -1;
        int id = nextPocketId++;
        list.add(id);
        setDirty();
        return id;
    }

    public List<Integer> getOwnedPockets(UUID owner) {
        return List.copyOf(ownedPockets.getOrDefault(owner, List.of()));
    }

    public boolean ownsPocket(UUID owner, int pocketId) {
        return ownedPockets.getOrDefault(owner, List.of()).contains(pocketId);
    }

    /** Returns the owner of the plot covering the supplied X/Z block coordinates. */
    public UUID getPocketOwnerAt(int blockX, int blockZ) {
        for (Map.Entry<UUID, List<Integer>> entry : ownedPockets.entrySet()) {
            for (int id : entry.getValue()) {
                int centerX = id * 300;
                int half = 15; // Half of the 30x30 interior; one extra block is the perimeter wall.
                if (blockX >= centerX - half - 1 && blockX < centerX + half + 1
                        && blockZ >= -half - 1 && blockZ < half + 1) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public boolean isPlotGenerated(int pocketId) {
        return generatedPlots.contains(pocketId);
    }

    public void markPlotGenerated(int pocketId) {
        if (generatedPlots.add(pocketId)) setDirty();
    }

    public void saveReturnPoint(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        returnPoints.put(player.getUUID(), new ReturnPoint(
                level.dimension().location().toString(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()
        ));
        setDirty();
    }

    public ReturnPoint getReturnPoint(UUID player) {
        return returnPoints.get(player);
    }

    public static PocketWorldData load(CompoundTag tag, HolderLookup.Provider registries) {
        PocketWorldData data = new PocketWorldData();
        data.nextPocketId = tag.getInt("next_pocket_id");

        ListTag owners = tag.getList("owners", Tag.TAG_COMPOUND);
        for (int i = 0; i < owners.size(); i++) {
            CompoundTag ownerTag = owners.getCompound(i);
            try {
                UUID owner = UUID.fromString(ownerTag.getString("uuid"));
                int[] ids = ownerTag.getIntArray("pockets");
                List<Integer> list = new ArrayList<>();
                for (int id : ids) list.add(id);
                data.ownedPockets.put(owner, list);
            } catch (IllegalArgumentException ignored) {
                // Skip malformed saved owner entries rather than failing the world load.
            }
        }

        ListTag returns = tag.getList("returns", Tag.TAG_COMPOUND);
        for (int i = 0; i < returns.size(); i++) {
            CompoundTag pointTag = returns.getCompound(i);
            try {
                UUID player = UUID.fromString(pointTag.getString("uuid"));
                data.returnPoints.put(player, new ReturnPoint(
                        pointTag.getString("dimension"),
                        pointTag.getDouble("x"), pointTag.getDouble("y"), pointTag.getDouble("z"),
                        pointTag.getFloat("yaw"), pointTag.getFloat("pitch")
                ));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed return points.
            }
        }

        int[] generated = tag.getIntArray("generated_plots");
        for (int id : generated) data.generatedPlots.add(id);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("next_pocket_id", nextPocketId);
        ListTag owners = new ListTag();
        for (Map.Entry<UUID, List<Integer>> entry : ownedPockets.entrySet()) {
            CompoundTag ownerTag = new CompoundTag();
            ownerTag.putString("uuid", entry.getKey().toString());
            int[] ids = entry.getValue().stream().mapToInt(Integer::intValue).toArray();
            ownerTag.putIntArray("pockets", ids);
            owners.add(ownerTag);
        }
        tag.put("owners", owners);

        ListTag returns = new ListTag();
        for (Map.Entry<UUID, ReturnPoint> entry : returnPoints.entrySet()) {
            CompoundTag pointTag = new CompoundTag();
            pointTag.putString("uuid", entry.getKey().toString());
            ReturnPoint p = entry.getValue();
            pointTag.putString("dimension", p.dimension());
            pointTag.putDouble("x", p.x());
            pointTag.putDouble("y", p.y());
            pointTag.putDouble("z", p.z());
            pointTag.putFloat("yaw", p.yaw());
            pointTag.putFloat("pitch", p.pitch());
            returns.add(pointTag);
        }
        tag.put("returns", returns);
        tag.putIntArray("generated_plots", generatedPlots.stream().mapToInt(Integer::intValue).toArray());
        return tag;
    }

    public record ReturnPoint(String dimension, double x, double y, double z, float yaw, float pitch) {}
}
