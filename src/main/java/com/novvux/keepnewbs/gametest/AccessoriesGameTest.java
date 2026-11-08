package com.novvux.keepnewbs.gametest;

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

// Checks the optional Accessories integration. The test is skipped when Accessories is not installed.
@GameTestHolder(KeepinventoryForNewbs.MODID)
@PrefixGameTestTemplate(false)
public class AccessoriesGameTest {
    @GameTest(template = "keepsinventory")
    public static void keepsAccessoriesOnDeath(GameTestHelper helper) {
        if (!ModList.get().isLoaded("accessories")) {
            helper.succeed();
            return;
        }

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
        int equipped = container.getAccessories().getItem(0).getCount();
        helper.assertTrue(equipped > 0, "the accessory could not be equipped");
        helper.assertTrue(!hasDiamond(player), "no accessories in the player inventory before death");
        helper.assertTrue(GraceManager.hasGrace(player), "the player should be inside the grace period");

        player.die(helper.getLevel().damageSources().generic());

        helper.assertTrue(container.getAccessories().getItem(0).isEmpty(), "accessories should empty the slot on death");
        helper.assertTrue(
                helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(16.0)).isEmpty(),
                "no items should have been dropped");

        ServerPlayer respawned = helper.getLevel().getServer().getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);

        AccessoriesCapability respawnedCapability = AccessoriesCapability.get(respawned);
        helper.assertTrue(respawnedCapability != null, "the respawned player has no accessories capability");

        AccessoriesContainer respawnedContainer = respawnedCapability.getContainers().get(slotName);
        helper.assertTrue(respawnedContainer != null, "the accessory slot is missing after the respawn");
        helper.assertTrue(respawnedContainer.getAccessories().getItem(0).getCount() == equipped, "the accessory should be restored on respawn");
        helper.assertTrue(!hasDiamond(respawned), "no duplicated accessories in the player inventory");

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
