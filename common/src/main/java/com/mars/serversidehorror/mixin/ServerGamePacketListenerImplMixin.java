package com.mars.serversidehorror.mixin;

import com.mars.serversidehorror.SavedDataHorror;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.mars.serversidehorror.Constants.SAVED_DATA_HORROR;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Inject(at = @At("HEAD"), method = "handleChat*")
    private void logChatMessage(ServerboundChatPacket packet, CallbackInfo ci) {
        String msg = StringUtils.normalizeSpace(packet.getMessage());
        DimensionDataStorage storage = ((ServerGamePacketListenerImpl)(Object) this).getPlayer().getServer().overworld().getDataStorage();
        SavedDataHorror savedData = storage.computeIfAbsent(SavedDataHorror::load, SavedDataHorror::new, SAVED_DATA_HORROR);
        savedData.addMessage(msg);
    }
}
