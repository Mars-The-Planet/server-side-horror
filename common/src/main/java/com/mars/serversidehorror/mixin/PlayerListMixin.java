package com.mars.serversidehorror.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.mars.serversidehorror.CommonClass.*;
import static com.mars.serversidehorror.CommonClass.FAKE_PLAYERS;
import static com.mars.serversidehorror.ServersideHorrorConfig.*;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void placeNewPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        System.out.println("CAU");

        PlayerList self = (PlayerList)(Object)this;
        MinecraftServer server = self.getServer();
        if(!isGracePeriodUp(grace_period, server.overworld()))
            return;

        if(player.serverLevel() != server.overworld())
            return;

        if(!(joining_on_bedrock_enable && chanceOneIn(joining_on_bedrock_chance) && !FAKE_PLAYERS.containsKey(player)))
            return;

        ServerGamePacketListenerImpl listener = (ServerGamePacketListenerImpl)connection.getPacketListener();
        joinOnBedrock(player, listener);
    }
}
