package com.doodly141.pocketrealms.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/** World-persistent ownership and navigation data. Existing 0.1.0-alpha keys are retained. */
public class PocketWorldData extends SavedData {
    private static final String DATA_NAME = "pocketrealms_world_data";
    private static final int MAX_POCKETS_PER_PLAYER = 5;

    private final Map<UUID, List<Integer>> ownedPockets = new HashMap<>();
    private final Map<UUID, ReturnPoint> returnPoints = new HashMap<>();
    private final Set<Integer> generatedPlots = new HashSet<>();
    private final Set<Integer> enclosedPlots = new HashSet<>();
    private final Map<Integer, String> homeNames = new HashMap<>();
    private final Map<Integer, AnchorLocation> activeAnchors = new HashMap<>();
    private final Map<Integer, HomeSpawn> homeSpawns = new HashMap<>();
    private final Map<UUID, Set<Integer>> recoveryMailbox = new HashMap<>();
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

    public String getHomeName(UUID owner, int pocketId) {
        String saved = homeNames.get(pocketId);
        if (saved != null && !saved.isBlank()) return saved;
        int slot = getOwnedPockets(owner).indexOf(pocketId);
        return slot >= 0 ? "Home " + (slot + 1) : "Home";
    }

    public boolean renamePocket(UUID owner, int pocketId, String name) {
        if (!ownsPocket(owner, pocketId) || name == null) return false;
        String clean = name.trim();
        if (clean.isEmpty() || clean.length() > 24) return false;
        homeNames.put(pocketId, clean);
        setDirty();
        return true;
    }

    public UUID getPocketOwnerAt(int blockX, int blockZ) {
        for (Map.Entry<UUID, List<Integer>> entry : ownedPockets.entrySet()) {
            for (int id : entry.getValue()) {
                int centerX = id * 300;
                if (blockX >= centerX - 16 && blockX < centerX + 16
                        && blockZ >= -16 && blockZ < 16) return entry.getKey();
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

    public boolean isPlotEnclosed(int pocketId) {
        return enclosedPlots.contains(pocketId);
    }

    public void markPlotEnclosed(int pocketId) {
        if (enclosedPlots.add(pocketId)) setDirty();
    }

    public boolean isAnchorActive(int pocketId) {
        return activeAnchors.containsKey(pocketId);
    }

    public void registerAnchor(UUID owner, int pocketId, ServerLevel level, BlockPos pos) {
        if (!ownsPocket(owner, pocketId)) return;
        AnchorLocation next = new AnchorLocation(owner, level.dimension().location().toString(),
                pos.getX(), pos.getY(), pos.getZ());
        if (!next.equals(activeAnchors.get(pocketId))) {
            activeAnchors.put(pocketId, next);
            setDirty();
        }
    }

    public void unregisterAnchor(UUID owner, int pocketId) {
        AnchorLocation current = activeAnchors.get(pocketId);
        if (current != null && current.owner().equals(owner)) {
            activeAnchors.remove(pocketId);
            setDirty();
        }
    }

    public void setHomeSpawn(int pocketId, ServerPlayer player) {
        HomeSpawn next = new HomeSpawn(player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot());
        homeSpawns.put(pocketId, next);
        setDirty();
    }

    public HomeSpawn getHomeSpawn(int pocketId) {
        return homeSpawns.get(pocketId);
    }

    public boolean addRecovery(UUID owner, int pocketId) {
        if (!ownsPocket(owner, pocketId)) return false;
        boolean added = recoveryMailbox.computeIfAbsent(owner, ignored -> new HashSet<>()).add(pocketId);
        if (added) setDirty();
        return added;
    }

    public boolean removeRecovery(UUID owner, int pocketId) {
        Set<Integer> stored = recoveryMailbox.get(owner);
        if (stored == null || !stored.remove(pocketId)) return false;
        if (stored.isEmpty()) recoveryMailbox.remove(owner);
        setDirty();
        return true;
    }

    public List<Integer> getRecoveryMailbox(UUID owner) {
        return recoveryMailbox.getOrDefault(owner, Set.of()).stream().sorted().toList();
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
                List<Integer> ids = new ArrayList<>();
                for (int id : ownerTag.getIntArray("pockets")) ids.add(id);
                data.ownedPockets.put(owner, ids);
                for (int id : ids) data.nextPocketId = Math.max(data.nextPocketId, id + 1);
            } catch (IllegalArgumentException ignored) {
                // Skip malformed saved owner entries rather than failing the world load.
            }
        }

        ListTag returns = tag.getList("returns", Tag.TAG_COMPOUND);
        for (int i = 0; i < returns.size(); i++) {
            CompoundTag pointTag = returns.getCompound(i);
            try {
                UUID player = UUID.fromString(pointTag.getString("uuid"));
                data.returnPoints.put(player, new ReturnPoint(pointTag.getString("dimension"),
                        pointTag.getDouble("x"), pointTag.getDouble("y"), pointTag.getDouble("z"),
                        pointTag.getFloat("yaw"), pointTag.getFloat("pitch")));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed return points.
            }
        }

        for (int id : tag.getIntArray("generated_plots")) data.generatedPlots.add(id);
        for (int id : tag.getIntArray("enclosed_plots")) data.enclosedPlots.add(id);

        ListTag names = tag.getList("home_names", Tag.TAG_COMPOUND);
        for (int i = 0; i < names.size(); i++) {
            CompoundTag entry = names.getCompound(i);
            String name = entry.getString("name");
            if (!name.isBlank()) data.homeNames.put(entry.getInt("id"), name);
        }

        ListTag anchors = tag.getList("active_anchors", Tag.TAG_COMPOUND);
        for (int i = 0; i < anchors.size(); i++) {
            CompoundTag entry = anchors.getCompound(i);
            try {
                UUID owner = UUID.fromString(entry.getString("owner"));
                int id = entry.getInt("id");
                if (data.ownsPocket(owner, id)) {
                    data.activeAnchors.put(id, new AnchorLocation(owner, entry.getString("dimension"),
                            entry.getInt("x"), entry.getInt("y"), entry.getInt("z")));
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed anchor index entries. Never remove the home itself.
            }
        }

        ListTag spawns = tag.getList("home_spawns", Tag.TAG_COMPOUND);
        for (int i = 0; i < spawns.size(); i++) {
            CompoundTag entry = spawns.getCompound(i);
            int id = entry.getInt("id");
            data.homeSpawns.put(id, new HomeSpawn(entry.getDouble("x"), entry.getDouble("y"),
                    entry.getDouble("z"), entry.getFloat("yaw"), entry.getFloat("pitch")));
        }

        ListTag mailboxes = tag.getList("recovery_mailboxes", Tag.TAG_COMPOUND);
        for (int i = 0; i < mailboxes.size(); i++) {
            CompoundTag entry = mailboxes.getCompound(i);
            try {
                UUID owner = UUID.fromString(entry.getString("owner"));
                Set<Integer> ids = new HashSet<>();
                for (int id : entry.getIntArray("pockets")) if (data.ownsPocket(owner, id)) ids.add(id);
                if (!ids.isEmpty()) data.recoveryMailbox.put(owner, ids);
            } catch (IllegalArgumentException ignored) {
                // Skip malformed mailbox entries.
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("next_pocket_id", nextPocketId);

        ListTag owners = new ListTag();
        for (Map.Entry<UUID, List<Integer>> entry : ownedPockets.entrySet()) {
            CompoundTag ownerTag = new CompoundTag();
            ownerTag.putString("uuid", entry.getKey().toString());
            ownerTag.putIntArray("pockets", entry.getValue().stream().mapToInt(Integer::intValue).toArray());
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
        tag.putIntArray("enclosed_plots", enclosedPlots.stream().mapToInt(Integer::intValue).toArray());

        ListTag names = new ListTag();
        for (Map.Entry<Integer, String> entry : homeNames.entrySet()) {
            CompoundTag value = new CompoundTag();
            value.putInt("id", entry.getKey());
            value.putString("name", entry.getValue());
            names.add(value);
        }
        tag.put("home_names", names);

        ListTag anchors = new ListTag();
        for (Map.Entry<Integer, AnchorLocation> entry : activeAnchors.entrySet()) {
            AnchorLocation p = entry.getValue();
            CompoundTag value = new CompoundTag();
            value.putInt("id", entry.getKey());
            value.putString("owner", p.owner().toString());
            value.putString("dimension", p.dimension());
            value.putInt("x", p.x());
            value.putInt("y", p.y());
            value.putInt("z", p.z());
            anchors.add(value);
        }
        tag.put("active_anchors", anchors);

        ListTag spawns = new ListTag();
        for (Map.Entry<Integer, HomeSpawn> entry : homeSpawns.entrySet()) {
            HomeSpawn p = entry.getValue();
            CompoundTag value = new CompoundTag();
            value.putInt("id", entry.getKey());
            value.putDouble("x", p.x());
            value.putDouble("y", p.y());
            value.putDouble("z", p.z());
            value.putFloat("yaw", p.yaw());
            value.putFloat("pitch", p.pitch());
            spawns.add(value);
        }
        tag.put("home_spawns", spawns);

        ListTag mailboxes = new ListTag();
        for (Map.Entry<UUID, Set<Integer>> entry : recoveryMailbox.entrySet()) {
            CompoundTag value = new CompoundTag();
            value.putString("owner", entry.getKey().toString());
            value.putIntArray("pockets", entry.getValue().stream().mapToInt(Integer::intValue).toArray());
            mailboxes.add(value);
        }
        tag.put("recovery_mailboxes", mailboxes);
        return tag;
    }

    public record ReturnPoint(String dimension, double x, double y, double z, float yaw, float pitch) {}
    public record AnchorLocation(UUID owner, String dimension, int x, int y, int z) {}
    public record HomeSpawn(double x, double y, double z, float yaw, float pitch) {}
}
