package com.mars.serversidehorror;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;

public class ServersideHorror implements ModInitializer {
    @Override
    public void onInitialize() {
        CommonClass.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            CommonClass.registerCommands(dispatcher);
        });
    }
}
