package com.novvux.keepnewbs.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

// Optional integration with mods that keep their own inventory. The classes of those mods are
// only touched when the mod is actually installed.
public final class ModCompat {
    public static final String CURIOS = "curios";
    public static final String ACCESSORIES = "accessories";

    private ModCompat() {}

    public static void register() {
        if (ModList.get().isLoaded(CURIOS)) {
            CuriosCompat.register();
        }

        if (ModList.get().isLoaded(ACCESSORIES)) {
            AccessoriesCompat.register();
        }
    }

    public static void save(ServerPlayer player, CompoundTag snapshot) {
        if (ModList.get().isLoaded(CURIOS)) {
            CuriosCompat.save(player, snapshot);
        }

        if (ModList.get().isLoaded(ACCESSORIES)) {
            AccessoriesCompat.save(player, snapshot);
        }
    }

    public static void restore(ServerPlayer player, CompoundTag snapshot) {
        if (ModList.get().isLoaded(CURIOS)) {
            CuriosCompat.restore(player, snapshot);
        }

        if (ModList.get().isLoaded(ACCESSORIES)) {
            AccessoriesCompat.restore(player, snapshot);
        }
    }
}
