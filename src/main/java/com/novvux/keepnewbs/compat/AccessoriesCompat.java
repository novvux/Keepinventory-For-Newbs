package com.novvux.keepnewbs.compat;

import java.lang.reflect.Proxy;
import java.util.Map;

import com.novvux.keepnewbs.GraceManager;
import com.novvux.keepnewbs.KeepinventoryForNewbs;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

// Accessories keeps one container per slot type. The items are removed from those containers
// during LivingDropsEvent, so the snapshot has to be taken at the death of the player.
public final class AccessoriesCompat {
    private static final String KEY = "Accessories";
    private static final String SLOT = "Slot";
    private static final String ITEMS = "Items";
    private static final String COSMETICS = "Cosmetics";

    private AccessoriesCompat() {}

    public static void register() {
        try {
            // Accessories uses a Fabric-style event for death drops; register reflectively so this
            // class does not need Fabric API types on the compile classpath.
            Class<?> callbackClass = Class.forName("io.wispforest.accessories.api.events.OnDeathCallback");
            Object event = callbackClass.getField("EVENT").get(null);
            Class<?> triStateClass = Class.forName("net.fabricmc.fabric.api.util.TriState");
            Object keep = triStateClass.getField("FALSE").get(null);

            Object callback = Proxy.newProxyInstance(
                    callbackClass.getClassLoader(),
                    new Class<?>[] { callbackClass },
                    (proxy, method, args) -> {
                        if ("shouldDrop".equals(method.getName()) && args != null && args.length >= 2) {
                            Object livingEntity = args[1];
                            if (livingEntity instanceof ServerPlayer player && GraceManager.hasSnapshot(player)) {
                                return keep;
                            }
                            return args[0];
                        }
                        if ("toString".equals(method.getName())) {
                            return "KeepinventoryForNewbsAccessoriesDeathCallback";
                        }
                        if ("hashCode".equals(method.getName())) {
                            return System.identityHashCode(proxy);
                        }
                        if ("equals".equals(method.getName())) {
                            return proxy == args[0];
                        }
                        return null;
                    });

            event.getClass().getMethod("register", Object.class).invoke(event, callback);
        } catch (ReflectiveOperationException | LinkageError exception) {
            KeepinventoryForNewbs.LOGGER.debug("Accessories death-drop callback could not be registered", exception);
        }
    }

    public static void save(ServerPlayer player, CompoundTag snapshot) {
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability == null) {
            return;
        }

        HolderLookup.Provider registries = player.level().registryAccess();
        ListTag containers = new ListTag();

        for (Map.Entry<String, AccessoriesContainer> entry : capability.getContainers().entrySet()) {
            AccessoriesContainer container = entry.getValue();
            CompoundTag tag = new CompoundTag();
            tag.putString(SLOT, entry.getKey());
            tag.put(ITEMS, write(container.getAccessories(), registries));
            tag.put(COSMETICS, write(container.getCosmeticAccessories(), registries));
            containers.add(tag);
        }

        snapshot.put(KEY, containers);
    }

    public static void restore(ServerPlayer player, CompoundTag snapshot) {
        if (!snapshot.contains(KEY, Tag.TAG_LIST)) {
            return;
        }

        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability == null) {
            return;
        }

        HolderLookup.Provider registries = player.level().registryAccess();
        ListTag containers = snapshot.getList(KEY, Tag.TAG_COMPOUND);

        for (Tag element : containers) {
            if (!(element instanceof CompoundTag tag)) {
                continue;
            }

            AccessoriesContainer container = capability.getContainers().get(tag.getString(SLOT));
            if (container == null) {
                continue;
            }

            read(container.getAccessories(), tag.getCompound(ITEMS), registries);
            read(container.getCosmeticAccessories(), tag.getCompound(COSMETICS), registries);
            // markChanged(true) flags the container for the Accessories sync tick so the client
            // menu is resynchronized with the restored contents.
            container.markChanged(true);
        }
    }

    private static CompoundTag write(Container container, HolderLookup.Provider registries) {
        ItemStackHandler handler = new ItemStackHandler(container.getContainerSize());
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            handler.setStackInSlot(slot, container.getItem(slot));
        }

        return handler.serializeNBT(registries);
    }

    private static void read(Container container, CompoundTag tag, HolderLookup.Provider registries) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            container.setItem(slot, ItemStack.EMPTY);
        }

        if (tag.isEmpty()) {
            return;
        }

        // Restore the stacks into the slots the container currently has. Saved slots beyond the
        // current size are skipped instead of being moved to the player inventory, which would
        // duplicate items that stay in the accessories inventory.
        ListTag items = tag.getList(ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            int slot = itemTag.getInt("Slot");
            if (slot < 0 || slot >= container.getContainerSize()) {
                continue;
            }

            ItemStack stack = ItemStack.parse(registries, itemTag).orElse(ItemStack.EMPTY);
            container.setItem(slot, stack);
        }
    }
}
