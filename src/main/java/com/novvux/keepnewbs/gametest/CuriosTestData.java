package com.novvux.keepnewbs.gametest;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.novvux.keepnewbs.KeepinventoryForNewbs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;

import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.common.data.CuriosEntityManager;
import top.theillusivec4.curios.common.data.CuriosSlotManager;

// Curios 9 does not define player slots on its own, they are added by data packs of content
// mods. In the game test environment no such data pack is installed, so the slot registry for
// players is empty. The registry is patched reflectively here so the Curios integration can be
// exercised; when a content mod defines player slots its data is left untouched.
final class CuriosTestData {
    private CuriosTestData() {}

    static void ensurePlayerSlots(MinecraftServer server, String... slotNames) {
        try {
            Field field = CuriosEntityManager.class.getDeclaredField("entitySlots");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<EntityType<?>, Map<String, ISlotType>> current =
                    (Map<EntityType<?>, Map<String, ISlotType>>) field.get(CuriosEntityManager.SERVER);
            if (!current.getOrDefault(EntityType.PLAYER, Map.of()).isEmpty()) {
                return;
            }

            Map<String, ISlotType> slots = new HashMap<>();
            for (String name : slotNames) {
                CuriosSlotManager.SERVER.getSlot(name).ifPresent(slot -> slots.put(name, slot));
            }

            Map<EntityType<?>, Map<String, ISlotType>> updated = new HashMap<>(current);
            updated.put(EntityType.PLAYER, Map.copyOf(slots));
            field.set(CuriosEntityManager.SERVER, updated);
        } catch (Exception exception) {
            KeepinventoryForNewbs.LOGGER.warn("Failed to inject Curios player slots for testing", exception);
        }
    }
}
