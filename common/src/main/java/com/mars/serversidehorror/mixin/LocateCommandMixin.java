package com.mars.serversidehorror.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceOrTagLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.mars.serversidehorror.CommonClass.isGracePeriodUp;
import static com.mars.serversidehorror.ServersideHorrorConfig.*;

@Mixin(LocateCommand.class)
public class LocateCommandMixin {
    @Inject(method = "locateStructure", at = @At("HEAD"), cancellable = true)
    private static void locateStructure(CommandSourceStack source, ResourceOrTagLocationArgument.Result<Structure> structure, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
        if(!old_villages_enable && structure.asPrintable().equals("serversidehorror:village_old_plains"))
            throw new SimpleCommandExceptionType(Component.translatable("serversidehorror.commands.locate.structure.disabled")).create();

        if(structure.asPrintable().contains("serversidehorror:traps/trap_")){
            if(!traps_enable)
                throw new SimpleCommandExceptionType(Component.translatable("serversidehorror.commands.locate.structure.disabled")).create();
            if(!isGracePeriodUp(source.getServer().overworld()) && grace_period_applies_to_traps)
                throw new SimpleCommandExceptionType(Component.translatable("serversidehorror.commands.locate.structure.grace_period_not_up")).create();
        }
    }
}
