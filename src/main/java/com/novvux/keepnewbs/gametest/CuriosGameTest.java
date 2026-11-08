package com.novvux.keepnewbs.gametest;

import java.util.Map;

import com.novvux.keepnewbs.GraceManager;
import com.novvux.keepnewbs.KeepinventoryForNewbs;

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

// Checks the optional Curios integration. The test is skipped when Curios is not installed.
@GameTestHolder(KeepinventoryForNewbs.MODID)
@PrefixGameTestTemplate(false)
public class CuriosGameTest {
    @GameTest(template = "keepsinventory")
    public static void keepsCuriosOnDeath(GameTestHelper helper) {
        if (!ModList.get().isLoaded("curios")) {
            helper.succeed();
            return;
        }

        ServerPlayer player = TestPlayers.spawn(helper);
        player.setGameMode(GameType.SURVIVAL);

        // Curios 9 does not define player slots on its own, they are added by data packs of the
        // content mods, so this test only runs in a pack that ships such a data pack.
        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) {
            KeepinventoryForNewbs.LOGGER.info("Skipping the Curios game test, no player curio slot is defined");
            helper.succeed();
            return;
        }

        Map.Entry<String, ICurioStacksHandler> slot = inventory.getCurios().entrySet().stream()
                .filter(entry -> entry.getValue().getSlots() > 0)
                .findFirst()
                .orElse(null);
        helper.assertTrue(slot != null, "the player has no curio slot");

        slot.getValue().getStacks().setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
        int equipped = slot.getValue().getStacks().getStackInSlot(0).getCount();
        helper.assertTrue(equipped > 0, "the curio could not be equipped");
        helper.assertTrue(GraceManager.hasGrace(player), "the player should be inside the grace period");

        player.die(helper.getLevel().damageSources().generic());

        helper.assertTrue(slot.getValue().getStacks().getStackInSlot(0).isEmpty(), "curios should empty the slot on death");
        helper.assertTrue(
                helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(16.0)).isEmpty(),
                "no items should have been dropped");

        ServerPlayer respawned = helper.getLevel().getServer().getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);

        ICuriosItemHandler respawnedInventory = CuriosApi.getCuriosInventory(respawned).orElse(null);
        helper.assertTrue(respawnedInventory != null, "the respawned player has no curios inventory");

        ICurioStacksHandler respawnedSlot = respawnedInventory.getStacksHandler(slot.getKey()).orElse(null);
        helper.assertTrue(respawnedSlot != null, "the curio slot is missing after the respawn");
        helper.assertTrue(respawnedSlot.getStacks().getStackInSlot(0).getCount() == equipped, "the curio should be restored on respawn");
        helper.assertTrue(!hasDiamond(respawned), "no duplicated curios in the player inventory");

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
