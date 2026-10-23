package com.mars.serversidehorror;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.Collection;

import static com.mars.serversidehorror.CommonClass.*;
import static com.mars.serversidehorror.Constants.SAVED_DATA_HORROR;
import static net.minecraft.commands.Commands.literal;

public class CommandRegistry {

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                literal("listFakeJoiner")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            int i = 0;
                            for (ServerPlayer fakeJoiner : FAKE_JOINERS.keySet()) {
                                Component entry = Component.literal("").append(Component.literal(i + ".)").withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.GREEN)).append(
                                                Component.literal(" Name: ").withStyle(ChatFormatting.BOLD)).append(
                                                Component.literal(fakeJoiner.getScoreboardName())).append(
                                                Component.literal(", UUID: ").withStyle(ChatFormatting.BOLD)).append(
                                                Component.literal(fakeJoiner.getStringUUID()+ " "))
                                        .append(
                                                Component.literal("[REMOVE]")
                                                        .withStyle(ChatFormatting.RED)
                                                        .withStyle((style -> style
                                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/removeFakeJoiner " + fakeJoiner.getStringUUID()))
                                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Make this fake player leave")))
                                                        ))
                                        );



                                ctx.getSource().sendSystemMessage(entry);
                                i++;
                            }

                            String successMsg = i + " fake players were found";

                            ctx.getSource().sendSuccess(() -> Component.literal(successMsg), true);

                            return 1;
                        }));

        dispatcher.register(
                literal("removeFakeJoiner")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("fakesUUID", StringArgumentType.word())
                                .executes(ctx -> {
                                    String fakesUUID = StringArgumentType.getString(ctx, "fakesUUID");
                                    for (ServerPlayer fakeJoiner : FAKE_JOINERS.keySet()) {
                                        if (fakeJoiner.getStringUUID().equals(fakesUUID)) {
                                            fakeJoinerLeave(ctx.getSource().getServer(), fakeJoiner);
                                            FAKE_JOINERS.remove(fakeJoiner);
                                            FAKE_JOINERS_TALKERS.remove(fakeJoiner);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Removed fake joiner with uuid " + fakesUUID), true);
                                            return 1;
                                        }
                                    }

                                    ctx.getSource().sendSuccess(() -> Component.literal("Did not find fake joiner with uuid " + fakesUUID), true);
                                    return 0;
                                })));

        dispatcher.register(
                literal("addFakeJoiner")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("fakesName", StringArgumentType.word())
                                .executes(ctx -> {
                                    String fakesName = StringArgumentType.getString(ctx, "fakesName");
                                    addFakeJoiner(ctx.getSource().getServer(), fakesName, false);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Added fake player " + fakesName), true);
                                    return 1;
                                })));

        dispatcher.register(
                literal("spawnFakePlayer")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("fakesName", StringArgumentType.word())
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("hideNametag", BoolArgumentType.bool())
                                                        .executes(ctx -> {
                                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                                            String fakesName = StringArgumentType.getString(ctx, "fakesName");
                                                            int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                                            boolean hideNametag = BoolArgumentType.getBool(ctx, "hideNametag");
                                                            targets.forEach(target -> spawnFakePlayer(target, fakesName, radius, hideNametag));
                                                            ctx.getSource().sendSuccess(() -> Component.literal("Spawned fake player " + fakesName), true);
                                                            return 1;
                                                        }))))));

        dispatcher.register(
                literal("spawnFakePlayer")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                            targets.forEach(target -> spawnFakePlayer(target, "MarsThePlanet_", 40, true));
                            ctx.getSource().sendSuccess(() -> Component.literal("Spawned Herobrine near players"), true);
                            return 1;
                        }));

        dispatcher.register(
                literal("hitPlayerLightning")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    TO_BE_HIT_BY_LIGHTNING.addAll(targets);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Struck players with lightning"), true);
                                    return 1;
                                })));

        dispatcher.register(
                literal("particleJumpScare")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    TO_BE_JUMP_SCARED.addAll(targets);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Jump-scared players"), true);
                                    return 1;
                                })));

        dispatcher.register(
                literal("setLongNight")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            MinecraftServer server = ctx.getSource().getServer();
                            ServerLevel level = server.overworld();
                            DimensionDataStorage storage = level.getDataStorage();
                            SavedDataHorror savedData = storage.computeIfAbsent(SavedDataHorror::load, SavedDataHorror::new, SAVED_DATA_HORROR);
                            savedData.setLongNight(true);
                            level.setDayTime(17999);
                            ctx.getSource().sendSuccess(() -> Component.literal("Set Long Night"), true);
                            return 1;
                        }));

        dispatcher.register(
                literal("breakTorches")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("minRadius", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("maxRadius", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                                    int minRadius = IntegerArgumentType.getInteger(ctx, "minRadius");
                                                    int maxRadius = IntegerArgumentType.getInteger(ctx, "maxRadius");
                                                    targets.forEach(target -> breakTorches(target, minRadius, maxRadius));
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Broke torches near players"), true);
                                                    return 1;
                                                })))));

        dispatcher.register(
                literal("replaceTorches")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("minRadius", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("maxRadius", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                                    int minRadius = IntegerArgumentType.getInteger(ctx, "minRadius");
                                                    int maxRadius = IntegerArgumentType.getInteger(ctx, "maxRadius");
                                                    targets.forEach(target -> replaceTorches(target, minRadius, maxRadius));
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Replaced torches near players with redstone torches"), true);
                                                    return 1;
                                                })))));

        dispatcher.register(
                literal("fakeMining")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    targets.forEach(target -> fakeMining(target));
                                    ctx.getSource().sendSuccess(() -> Component.literal("Players will hear fake mining noises"), true);
                                    return 1;
                                })));

        dispatcher.register(
                literal("fakeSteps")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    targets.forEach(target -> fakeSteps(target));
                                    ctx.getSource().sendSuccess(() -> Component.literal("Players will hear fake footsteps"), true);
                                    return 1;
                                })));

        dispatcher.register(
                literal("setupNewTrap")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");

                                    targets.forEach(target -> placeSmallTrap(target));
                                    ctx.getSource().sendSuccess(() -> Component.literal("A new trap will be be set up near these players"), true);
                                    return 1;
                                })));

        dispatcher.register(
                literal("startRandomFire")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                            int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                            targets.forEach(target -> startFire(target, radius));
                                            ctx.getSource().sendSuccess(() -> Component.literal("A random fire will be started near these players"), true);
                                            return 1;
                                        }))));

        dispatcher.register(
                literal("removeLeaves")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("maxRadius", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("minRadius", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                                    int maxRadius = IntegerArgumentType.getInteger(ctx, "maxRadius");
                                                    int minRadius = IntegerArgumentType.getInteger(ctx, "minRadius");
                                                    targets.forEach(target -> removeLeaves(target, maxRadius, minRadius));
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Leaves will be removed around players"), true);
                                                    return 1;
                                                })))));

        dispatcher.register(
                literal("placeSign")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("maxRadius", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("minRadius", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                                    int maxRadius = IntegerArgumentType.getInteger(ctx, "maxRadius");
                                                    int minRadius = IntegerArgumentType.getInteger(ctx, "minRadius");
                                                    for(ServerPlayer target : targets){
                                                        boolean canPlace = placeSign(target, maxRadius, minRadius);
                                                        if(canPlace)
                                                            ctx.getSource().sendSuccess(() -> Component.literal("Placed a sign near player " + target.getName().getString()), true);
                                                        else
                                                            ctx.getSource().sendSuccess(() -> Component.literal("Couldn't place a sign near player " + target.getName().getString()), true);
                                                    }
                                                    return 1;
                                                })))));

        dispatcher.register(
                literal("placeHead")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .then(Commands.argument("maxRadius", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("minRadius", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> {
                                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                                            String name = StringArgumentType.getString(ctx, "name");
                                                            int maxRadius = IntegerArgumentType.getInteger(ctx, "maxRadius");
                                                            int minRadius = IntegerArgumentType.getInteger(ctx, "minRadius");
                                                            for(ServerPlayer target : targets){
                                                                boolean canPlace = placeHead(target, name, maxRadius, minRadius);
                                                                if(canPlace)
                                                                    ctx.getSource().sendSuccess(() -> Component.literal("Placed a head near player " + target.getName().getString()), true);
                                                                else
                                                                    ctx.getSource().sendSuccess(() -> Component.literal("Couldn't place head near player " + target.getName().getString()), true);
                                                            }
                                                            return 1;
                                                        }))))));

        dispatcher.register(
                literal("playScarySound")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players()).
                                then(Commands.argument("radius", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                            int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                            for(ServerPlayer target : targets){
                                                boolean didPlay = playScarySound(target, radius);
                                                if(didPlay)
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Played a scary sound near player " + target.getName().getString()), true);
                                                else
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Couldn't play a scary sound because the scary_sound_list config is empty"), true);
                                            }
                                            return 1;
                                        }))));

        dispatcher.register(
                literal("resetMessages")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            DimensionDataStorage storage = (ctx.getSource().getServer()).overworld().getDataStorage();
                            SavedDataHorror savedData = storage.computeIfAbsent(SavedDataHorror::load, SavedDataHorror::new, SAVED_DATA_HORROR);
                            savedData.setPlayerMessages(new ArrayList<>());
                            ctx.getSource().sendSuccess(() -> Component.literal("Successfully reset all messages"), true);
                            return 1;
                        }));
    }
}
