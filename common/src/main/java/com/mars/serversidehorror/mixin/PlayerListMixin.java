package com.mars.serversidehorror.mixin;

import com.mars.serversidehorror.SavedDataHorror;
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
        PlayerList self = (PlayerList)(Object)this;
        MinecraftServer server = self.getServer();

        SavedDataHorror savedData = SavedDataHorror.get(server);
        String playerName = player.getPlainTextName();
        savedData.addSeenPlayer(playerName);

        if(!isGracePeriodUp(server.overworld()))
            return;

        if(player.level() != server.overworld())
            return;

        ServerGamePacketListenerImpl listener = (ServerGamePacketListenerImpl)connection.getPacketListener();

        if(joining_on_bedrock_enable && chanceOneIn(joining_on_bedrock_chance) && !FAKE_PLAYERS.containsKey(player))
            joinOnBedrock(player, listener);

        if(joining_in_dungeon_enable && chanceOneIn(joining_in_dungeon_chance) && !FAKE_PLAYERS.containsKey(player))
            joinInDungeon(player, listener);
    }
}
