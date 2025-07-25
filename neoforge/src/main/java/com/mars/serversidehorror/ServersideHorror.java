package com.mars.serversidehorror;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class ServersideHorror {
    public ServersideHorror(IEventBus eventBus) {
        CommonClass.init();
    }
}
