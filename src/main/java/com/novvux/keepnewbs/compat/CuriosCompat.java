package com.novvux.keepnewbs.compat;

import com.novvux.keepnewbs.GraceManager;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.ItemStackHandler;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

// Curios keeps its slots in a capability of the player. The items are removed from the slots
// during LivingDropsEvent, so the snapshot has to be taken at the death of the player.
public final class CuriosCompat {
    private static final String KEY = "Curios";
    private static final String SLOT = "Identifier";
    private static final String STACKS = "Stacks";
    private static final String COSMETICS = "Cosmetics";

    private CuriosCompat() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, DropRulesEvent.class, CuriosCompat::onDropRules);
    }

    private static void onDropRules(DropRulesEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && GraceManager.hasSnapshot(player)) {
            // The snapshot is restored by the mod on respawn, so Curios must not drop or move
            // the stacks on its own. Added last so it wins over other drop-rule overrides.
            event.addOverride(stack -> true, ICurio.DropRule.ALWAYS_KEEP);
        }
    }

    public static void save(ServerPlayer player, CompoundTag snapshot) {
        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) {
            return;
        }

        // false = only read the slots, the items stay in the inventory.
        snapshot.put(KEY, inventory.saveInventory(false));
    }

    public static void restore(ServerPlayer player, CompoundTag snapshot) {
        if (!snapshot.contains(KEY, Tag.TAG_LIST)) {
            return;
        }

        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) {
            return;
        }

        HolderLookup.Provider registries = player.level().registryAccess();
        ListTag saved = snapshot.getList(KEY, Tag.TAG_COMPOUND);

        for (Tag element : saved) {
            if (!(element instanceof CompoundTag tag)) {
                continue;
            }

            ICurioStacksHandler handler = inventory.getStacksHandler(tag.getString(SLOT)).orElse(null);
            if (handler == null) {
                continue;
            }

            readStacks(handler.getStacks(), tag.getCompound(STACKS), registries);
            readStacks(handler.getCosmeticStacks(), tag.getCompound(COSMETICS), registries);
        }
    }

    private static void readStacks(IDynamicStackHandler handler, CompoundTag tag, HolderLookup.Provider registries) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            handler.setStackInSlot(slot, ItemStack.EMPTY);
        }

        if (tag.isEmpty()) {
            return;
        }

        // Deserialize into an ItemStackHandler and copy the stacks directly into the curio slots.
        // This avoids Curios' loadInventory/loseInvalidStack path, which can spill existing stacks
        // into the player inventory when restoring on top of a respawned player.
        ItemStackHandler loaded = new ItemStackHandler();
        loaded.deserializeNBT(registries, tag);

        int limit = Math.min(handler.getSlots(), loaded.getSlots());
        for (int slot = 0; slot < limit; slot++) {
            handler.setStackInSlot(slot, loaded.getStackInSlot(slot));
        }
    }
}
