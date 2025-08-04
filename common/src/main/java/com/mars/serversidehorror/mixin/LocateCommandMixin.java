package com.mars.serversidehorror.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.mars.serversidehorror.ServersideHorrorConfig.old_villages_enable;

@Mixin(LocateCommand.class)
public class LocateCommandMixin {
    @Inject(method = "locateStructure", at = @At("HEAD"), cancellable = true)
    private static void locateStructure(CommandSourceStack source, ResourceOrTagKeyArgument.Result<Structure> structure, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
        System.out.println(structure.asPrintable());
        if(!old_villages_enable && structure.asPrintable().equals("serversidehorror:village_old_plains"))
            throw new SimpleCommandExceptionType(Component.translatable("serversidehorror.commands.locate.structure.disabled")).create();
    }
}
