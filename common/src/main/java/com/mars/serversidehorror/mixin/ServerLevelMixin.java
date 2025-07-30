package com.mars.serversidehorror.mixin;

import com.mars.serversidehorror.SavedDataHorror;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

import static com.mars.serversidehorror.CommonClass.*;
import static com.mars.serversidehorror.Constants.SAVED_DATA_HORROR;
import static com.mars.serversidehorror.ServersideHorrorConfig.*;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(at = @At("HEAD"), method = "tick")
    private void tickServer(BooleanSupplier hasTimeLeft, CallbackInfo info) {
        ServerLevel self = (ServerLevel)(Object)this;
        if(!isGracePeriodUp(grace_period, self)) return;

        TickRateManager tickratemanager = self.tickRateManager();
        if(!tickratemanager.runsNormally())
            return;

        long time = self.getLevelData().getDayTime();
        DimensionDataStorage storage = self.getServer().overworld().getDataStorage();
        SavedDataHorror savedData = storage.computeIfAbsent(new SavedData.Factory<>(SavedDataHorror::create, SavedDataHorror::load, null), SAVED_DATA_HORROR);

        //Midnight - rolls a chance to be a long night
        if(time == 18000 && self.getLevelData().getGameRules().getBoolean(GameRules.RULE_DAYLIGHT) && long_night_enable){
            savedData.setLongNight(chanceOneIn(long_night_chance));
        }

        // rolls a chance to end the long night
        if(savedData.getLongNight()){
            savedData.setLongNight(!chanceOneIn(12000));
            self.getLevelData().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(!savedData.getLongNight(), self.getServer());
        }
    }
}
