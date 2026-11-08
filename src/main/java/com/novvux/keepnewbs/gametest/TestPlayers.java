package com.novvux.keepnewbs.gametest;

import java.util.HashSet;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

// Replacement for GameTestHelper#makeMockServerPlayerInLevel.
//
// The mock player of the game test framework has no client, so every modded payload that is sent
// while the player logs in (Curios and Accessories do that) would be rejected by NeoForge. The
// connection is created manually here with a channel set that accepts every payload id.
final class TestPlayers {
    private TestPlayers() {}

    static ServerPlayer spawn(GameTestHelper helper) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());

        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        connection.channel().attr(ChannelAttributes.ADHOC_CHANNELS).set(new HashSet<>() {
            @Override
            public boolean contains(Object value) {
                return true;
            }
        });

        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }
}
