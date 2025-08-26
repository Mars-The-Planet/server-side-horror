package com.mars.serversidehorror;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class ServersideHorror {
    public ServersideHorror() {
        CommonClass.init();
        RegisterCommandsEvent.BUS.addListener(ServersideHorror::onRegisterCommands);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        CommonClass.registerCommands(dispatcher);
    }
}
