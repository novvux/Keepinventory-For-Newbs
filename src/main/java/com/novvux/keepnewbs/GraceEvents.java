package com.novvux.keepnewbs;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

// Server side logic of the keep-inventory grace period.
//
// A player that is inside the grace period behaves like the vanilla keepInventory game rule:
// nothing is dropped on death and the items plus the experience are given back on respawn.
@EventBusSubscriber(modid = KeepinventoryForNewbs.MODID)
public final class GraceEvents {
    private GraceEvents() {}

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        GraceManager.recordFirstJoin(player);

        // A snapshot of a death that never happened (the death was cancelled by another mod)
        // would restore items the player still owns, so it is dropped here.
        if (GraceManager.hasSnapshot(player) && player.getHealth() > 0.0F) {
            GraceManager.clearSnapshot(player);
        }

        if (Config.announceOnJoin() && GraceManager.isEnabled(player)) {
            player.sendSystemMessage(GraceManager.statusMessage(player));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!GraceManager.hasGrace(player)) {
            return;
        }

        // The inventory is still complete at this point, the drops happen later on.
        GraceManager.storeSnapshot(player);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // The snapshot is the proof that this death is protected, the timer is only checked once
        // when the player dies. That way the grace period cannot run out between the death and the drops.
        if (!GraceManager.hasSnapshot(player)) {
            return;
        }

        // The items of the player are only collected by NeoForge at this point, cancelling
        // the event keeps them out of the world. They are restored on respawn instead.
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!Config.keepExperience() || !GraceManager.hasSnapshot(player)) {
            return;
        }

        // Experience orbs are spawned directly into the level, so they have to be stopped here.
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer deadPlayer) || !(event.getEntity() instanceof ServerPlayer respawnedPlayer)) {
            return;
        }

        GraceManager.copyMetadata(deadPlayer, respawnedPlayer);

        if (event.isWasDeath()) {
            GraceManager.restoreSnapshot(deadPlayer, respawnedPlayer);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("keepinv")
                .requires(source -> source.hasPermission(0))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    Component message = GraceManager.statusMessage(player);
                    context.getSource().sendSuccess(() -> message, false);
                    return 1;
                }));
    }
}
