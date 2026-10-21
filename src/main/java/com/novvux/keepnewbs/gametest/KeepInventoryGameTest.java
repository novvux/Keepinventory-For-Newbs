package com.novvux.keepnewbs.gametest;

import com.novvux.keepnewbs.GraceManager;
import com.novvux.keepnewbs.KeepinventoryForNewbs;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// Verifies that a player inside the grace period keeps the inventory and the experience
// when the player dies, and that nothing is dropped into the world.
@GameTestHolder(KeepinventoryForNewbs.MODID)
@PrefixGameTestTemplate(false)
public class KeepInventoryGameTest {
    @GameTest(template = "keepsinventory")
    @SuppressWarnings("removal") // makeMockServerPlayerInLevel is the only way to get a real server player in 1.21.1
    public static void keepsInventoryOnDeath(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(Items.DIAMOND, 5));
        player.experienceLevel = 7;
        player.totalExperience = 30;

        helper.assertTrue(GraceManager.hasGrace(player), "the player should be inside the grace period");

        player.die(helper.getLevel().damageSources().generic());

        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "vanilla empties the inventory on death");
        helper.assertTrue(GraceManager.hasSnapshot(player), "the inventory should have been snapshotted");
        helper.assertTrue(
                helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(16.0)).isEmpty(),
                "no items should have been dropped");
        helper.assertTrue(
                helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, player.getBoundingBox().inflate(16.0)).isEmpty(),
                "no experience orbs should have been dropped");

        ServerPlayer respawned = helper.getLevel().getServer().getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);

        helper.assertTrue(respawned.getInventory().getItem(0).getCount() == 5, "the inventory should be restored on respawn");
        helper.assertTrue(respawned.experienceLevel == 7, "the experience level should be restored on respawn");
        helper.assertTrue(GraceManager.firstJoinMillis(respawned) > 0L, "the first login should survive the respawn");

        helper.succeed();
    }
}
