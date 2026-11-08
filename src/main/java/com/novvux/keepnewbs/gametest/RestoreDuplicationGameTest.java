package com.novvux.keepnewbs.gametest;

import java.util.Map;

import com.novvux.keepnewbs.GraceManager;
import com.novvux.keepnewbs.KeepinventoryForNewbs;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

// Verifies that restoring a grace-period snapshot does not duplicate accessory or curio items
// into the main player inventory, even when the respawn target already has items in those slots.
@GameTestHolder(KeepinventoryForNewbs.MODID)
@PrefixGameTestTemplate(false)
public class RestoreDuplicationGameTest {
    @GameTest(template = "keepsinventory")
    public static void restoreCuriosOverOccupiedSlotsDoesNotDuplicate(GameTestHelper helper) {
        if (!ModList.get().isLoaded("curios")) {
            helper.succeed();
            return;
        }

        CuriosTestData.ensurePlayerSlots(helper.getLevel().getServer(), "ring", "necklace");

        ServerPlayer source = TestPlayers.spawn(helper);
        ServerPlayer target = TestPlayers.spawn(helper);
        source.setGameMode(GameType.SURVIVAL);
        target.setGameMode(GameType.SURVIVAL);

        ICuriosItemHandler sourceInventory = CuriosApi.getCuriosInventory(source).orElse(null);
        ICuriosItemHandler targetInventory = CuriosApi.getCuriosInventory(target).orElse(null);
        helper.assertTrue(sourceInventory != null && targetInventory != null, "players must have curios inventories");

        ICurioStacksHandler sourceSlot = null;
        ICurioStacksHandler targetSlot = null;
        String curioKey = null;
        for (Map.Entry<String, ICurioStacksHandler> entry : sourceInventory.getCurios().entrySet()) {
            if (entry.getValue().getSlots() > 0) {
                sourceSlot = entry.getValue();
                curioKey = entry.getKey();
                targetSlot = targetInventory.getStacksHandler(curioKey).orElse(null);
                break;
            }
        }
        helper.assertTrue(sourceSlot != null && targetSlot != null, "players must have a curio slot");

        sourceSlot.getStacks().setStackInSlot(0, new ItemStack(Items.DIAMOND, 4));
        targetSlot.getStacks().setStackInSlot(0, new ItemStack(Items.EMERALD, 2));
        target.getInventory().clearContent();

        GraceManager.storeSnapshot(source);
        GraceManager.restoreSnapshot(source, target);

        ICurioStacksHandler restoredSlot = CuriosApi.getCuriosInventory(target).orElseThrow().getStacksHandler(curioKey).orElseThrow();
        ItemStack restored = restoredSlot.getStacks().getStackInSlot(0);
        helper.assertTrue(restored.is(Items.DIAMOND) && restored.getCount() == 4, "the snapshotted curio should replace occupied slots");
        helper.assertTrue(!hasItem(target, Items.EMERALD), "preexisting curio items must not spill into player inventory");
        helper.assertTrue(countItem(target, Items.DIAMOND) == 0, "restored curios must not duplicate into player inventory");

        helper.succeed();
    }

    @GameTest(template = "keepsinventory")
    public static void restoreAccessoriesOverOccupiedSlotsDoesNotDuplicate(GameTestHelper helper) {
        if (!ModList.get().isLoaded("accessories")) {
            helper.succeed();
            return;
        }

        ServerPlayer source = TestPlayers.spawn(helper);
        ServerPlayer target = TestPlayers.spawn(helper);
        source.setGameMode(GameType.SURVIVAL);
        target.setGameMode(GameType.SURVIVAL);

        AccessoriesCapability sourceCapability = AccessoriesCapability.get(source);
        AccessoriesCapability targetCapability = AccessoriesCapability.get(target);
        helper.assertTrue(sourceCapability != null && targetCapability != null, "players must have accessories capabilities");

        AccessoriesContainer sourceContainer = sourceCapability.getContainers().values().stream()
                .filter(entry -> entry.getAccessories().getContainerSize() > 0)
                .findFirst()
                .orElse(null);
        helper.assertTrue(sourceContainer != null, "the player must have an accessory slot");

        String slotName = sourceContainer.getSlotName();
        AccessoriesContainer targetContainer = targetCapability.getContainers().get(slotName);
        helper.assertTrue(targetContainer != null, "the target player must have the accessory slot");

        sourceContainer.getAccessories().setItem(0, new ItemStack(Items.DIAMOND, 4));
        targetContainer.getAccessories().setItem(0, new ItemStack(Items.EMERALD, 2));
        target.getInventory().clearContent();

        GraceManager.storeSnapshot(source);
        GraceManager.restoreSnapshot(source, target);

        AccessoriesContainer restoredContainer = AccessoriesCapability.get(target).getContainers().get(slotName);
        ItemStack restored = restoredContainer.getAccessories().getItem(0);
        helper.assertTrue(restored.is(Items.DIAMOND) && restored.getCount() == 4, "the snapshotted accessory should replace occupied slots");
        helper.assertTrue(!hasItem(target, Items.EMERALD), "preexisting accessories must not spill into player inventory");
        helper.assertTrue(countItem(target, Items.DIAMOND) == 0, "restored accessories must not duplicate into player inventory");

        helper.succeed();
    }

    private static boolean hasItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        return countItem(player, item) > 0;
    }

    private static int countItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
