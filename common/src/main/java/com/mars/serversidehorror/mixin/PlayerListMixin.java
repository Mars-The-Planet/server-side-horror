package com.mars.serversidehorror.mixin;

import com.mars.serversidehorror.PlayerBehavior;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void placeNewPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        MinecraftServer server = player.server;

        DimensionDataStorage storage = server.overworld().getDataStorage();
        PlayerBehavior behavior = storage.computeIfAbsent(new SavedData.Factory<>(PlayerBehavior::create, PlayerBehavior::load, null), "player_behavior");

        //behavior.addPlayer(player.getName().getString());
        //System.out.println(behavior.getPlayer_names());

        System.out.println(behavior.getTest());
        behavior.setTest(behavior.getTest() + 1);
    }
}
