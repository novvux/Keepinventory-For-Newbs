package com.novvux.keepnewbs;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;

// Keeps track of when a player joined the server for the first time and holds the
// inventory snapshot that is used to restore the items of a player that died inside
// the grace period.
public final class GraceManager {
    private static final String KEY_FIRST_JOIN = "keepinv_first_join";
    private static final String KEY_SNAPSHOT = "keepinv_snapshot";

    private static final String SNAP_ITEMS = "Items";
    private static final String SNAP_XP_LEVEL = "XpLevel";
    private static final String SNAP_XP_TOTAL = "XpTotal";
    private static final String SNAP_XP_PROGRESS = "XpProgress";
    private static final String SNAP_SELECTED = "SelectedSlot";

    //private static final long HOUR_MILLIS = 3_600_000L;
    private static final long HOUR_MILLIS = 3_600_000L;

    private GraceManager() {}

    public static long graceMillis() {
        return (long) Config.gracePeriodHours() * HOUR_MILLIS;
    }

    // True when the grace period may be applied to the given player at all.
    public static boolean isEnabled(ServerPlayer player) {
        if (Config.gracePeriodHours() <= 0) {
            return false;
        }

        MinecraftServer server = player.getServer();
        return server == null || !server.isHardcore() || Config.applyInHardcore();
    }

    // True while the player is still inside the grace period that started at their first login.
    public static boolean hasGrace(ServerPlayer player) {
        if (!isEnabled(player)) {
            return false;
        }

        long firstJoin = firstJoinMillis(player);
        return firstJoin > 0L && System.currentTimeMillis() - firstJoin < graceMillis();
    }

    public static long firstJoinMillis(ServerPlayer player) {
        return player.getPersistentData().getLong(KEY_FIRST_JOIN);
    }

    public static long remainingMillis(ServerPlayer player) {
        long firstJoin = firstJoinMillis(player);
        if (firstJoin <= 0L) {
            return 0L;
        }

        return Math.max(0L, graceMillis() - (System.currentTimeMillis() - firstJoin));
    }

    // Stores the moment of the first login, later logins never overwrite it.
    public static void recordFirstJoin(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(KEY_FIRST_JOIN, Tag.TAG_LONG)) {
            data.putLong(KEY_FIRST_JOIN, System.currentTimeMillis());
        }
    }

    // Takes a copy of the inventory and the experience of a player that is about to die.
    public static void storeSnapshot(ServerPlayer player) {
        CompoundTag snapshot = new CompoundTag();
        Inventory inventory = player.getInventory();

        snapshot.put(SNAP_ITEMS, inventory.save(new ListTag()));
        snapshot.putInt(SNAP_XP_LEVEL, player.experienceLevel);
        snapshot.putInt(SNAP_XP_TOTAL, player.totalExperience);
        snapshot.putFloat(SNAP_XP_PROGRESS, player.experienceProgress);
        snapshot.putInt(SNAP_SELECTED, inventory.selected);

        player.getPersistentData().put(KEY_SNAPSHOT, snapshot);
    }

    public static boolean hasSnapshot(ServerPlayer player) {
        return player.getPersistentData().contains(KEY_SNAPSHOT, Tag.TAG_COMPOUND);
    }

    public static void clearSnapshot(ServerPlayer player) {
        player.getPersistentData().remove(KEY_SNAPSHOT);
    }

    // Gives the items of the dead player back to the player that just respawned.
    public static void restoreSnapshot(ServerPlayer deadPlayer, ServerPlayer respawnedPlayer) {
        CompoundTag data = deadPlayer.getPersistentData();
        if (!data.contains(KEY_SNAPSHOT, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag snapshot = data.getCompound(KEY_SNAPSHOT);
        Inventory inventory = respawnedPlayer.getInventory();

        inventory.load(snapshot.getList(SNAP_ITEMS, Tag.TAG_COMPOUND));
        inventory.selected = snapshot.getInt(SNAP_SELECTED);

        if (Config.keepExperience()) {
            respawnedPlayer.experienceLevel = snapshot.getInt(SNAP_XP_LEVEL);
            respawnedPlayer.totalExperience = snapshot.getInt(SNAP_XP_TOTAL);
            respawnedPlayer.experienceProgress = snapshot.getFloat(SNAP_XP_PROGRESS);
        }

        data.remove(KEY_SNAPSHOT);
    }

    // The persistent data of an entity is not inherited by the player that respawns,
    // so the first login has to be carried over manually.
    public static void copyMetadata(ServerPlayer deadPlayer, ServerPlayer respawnedPlayer) {
        CompoundTag data = deadPlayer.getPersistentData();
        if (data.contains(KEY_FIRST_JOIN, Tag.TAG_LONG)) {
            respawnedPlayer.getPersistentData().putLong(KEY_FIRST_JOIN, data.getLong(KEY_FIRST_JOIN));
        }
    }

    public static Component statusMessage(ServerPlayer player) {
        long remaining = remainingMillis(player);
        if (remaining <= 0L) {
            return Component.translatable("keepinventoryfornewbs.grace.none");
        }

        long totalMinutes = remaining / 60_000L;
        return Component.translatable("keepinventoryfornewbs.grace.active", totalMinutes / 60L, totalMinutes % 60L);
    }
}
