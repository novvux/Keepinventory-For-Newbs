package com.novvux.keepnewbs.gametest;

import java.util.Map;

import com.novvux.keepnewbs.GraceManager;
import com.novvux.keepnewbs.KeepinventoryForNewbs;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

// Checks that accessories and curios items are kept together when both mods are installed, and
// that no item is duplicated into the player's main inventory.
@GameTestHolder(KeepinventoryForNewbs.MODID)
@PrefixGameTestTemplate(false)
public class AccessoriesCuriosCompatGameTest {
    @GameTest(template = "keepsinventory")
    public static void keepsAccessoriesAndCuriosOnDeath(GameTestHelper helper) {
        if (!ModList.get().isLoaded("accessories") || !ModList.get().isLoaded("curios")) {
            helper.succeed();
            return;
        }

        CuriosTestData.ensurePlayerSlots(helper.getLevel().getServer(), "ring", "necklace");

        ServerPlayer player = TestPlayers.spawn(helper);
        player.setGameMode(GameType.SURVIVAL);

        AccessoriesCapability capability = AccessoriesCapability.get(player);
        helper.assertTrue(capability != null, "the player has no accessories capability");

        AccessoriesContainer container = capability.getContainers().values().stream()
                .filter(entry -> entry.getAccessories().getContainerSize() > 0)
                .findFirst()
                .orElse(null);
        helper.assertTrue(container != null, "the player has no accessory slot");

        String slotName = container.getSlotName();
        container.getAccessories().setItem(0, new ItemStack(Items.DIAMOND, 2));
        int accessoryCount = container.getAccessories().getItem(0).getCount();
        helper.assertTrue(accessoryCount > 0, "the accessory could not be equipped");

        ICuriosItemHandler curios = CuriosApi.getCuriosInventory(player).orElse(null);
        helper.assertTrue(curios != null, "the player has no curios inventory");

        ICurioStacksHandler curioSlot = null;
        String curioKey = null;
        for (Map.Entry<String, ICurioStacksHandler> entry : curios.getCurios().entrySet()) {
            if (entry.getValue().getSlots() > 0) {
                curioSlot = entry.getValue();
                curioKey = entry.getKey();
                break;
            }
        }
        helper.assertTrue(curioSlot != null, "the player has no curio slot");

        curioSlot.getStacks().setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
        int curioCount = curioSlot.getStacks().getStackInSlot(0).getCount();
        helper.assertTrue(curioCount > 0, "the curio could not be equipped");

        helper.assertTrue(!hasDiamond(player), "no accessories or curios in the player inventory before death");
        helper.assertTrue(GraceManager.hasGrace(player), "the player should be inside the grace period");

        player.die(helper.getLevel().damageSources().generic());

        helper.assertTrue(container.getAccessories().getItem(0).isEmpty(), "accessories should empty the slot on death");
        helper.assertTrue(curioSlot.getStacks().getStackInSlot(0).isEmpty(), "curios should empty the slot on death");
        helper.assertTrue(
                helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(16.0)).isEmpty(),
                "no items should have been dropped");

        ServerPlayer respawned = helper.getLevel().getServer().getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);

        AccessoriesCapability respawnedCapability = AccessoriesCapability.get(respawned);
        helper.assertTrue(respawnedCapability != null, "the respawned player has no accessories capability");

        AccessoriesContainer respawnedContainer = respawnedCapability.getContainers().get(slotName);
        helper.assertTrue(respawnedContainer != null, "the accessory slot is missing after the respawn");
        helper.assertTrue(
                respawnedContainer.getAccessories().getItem(0).getCount() == accessoryCount,
                "the accessory should be restored on respawn");

        ICuriosItemHandler respawnedCurios = CuriosApi.getCuriosInventory(respawned).orElse(null);
        helper.assertTrue(respawnedCurios != null, "the respawned player has no curios inventory");

        ICurioStacksHandler respawnedCurioSlot = respawnedCurios.getStacksHandler(curioKey).orElse(null);
        helper.assertTrue(respawnedCurioSlot != null, "the curio slot is missing after the respawn");
        helper.assertTrue(
                respawnedCurioSlot.getStacks().getStackInSlot(0).getCount() == curioCount,
                "the curio should be restored on respawn");

        helper.assertTrue(!hasDiamond(respawned), "no duplicated accessories or curios in the player inventory");

        helper.succeed();
    }

    private static boolean hasDiamond(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.DIAMOND)) {
                return true;
            }
        }
        return false;
    }
}
