package com.mars.serversidehorror;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mars.deimos.config.DeimosConfig;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.math.Vector3f;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mars.serversidehorror.Constants.MOD_ID;
import static com.mars.serversidehorror.Constants.SAVED_DATA_HORROR;
import static com.mars.serversidehorror.ServersideHorrorConfig.grace_period;
import static com.mars.serversidehorror.ServersideHorrorConfig.random_signs_texts;
import static net.minecraft.commands.Commands.literal;

public class CommonClass {
    public static Map<ServerPlayer, Integer> FAKE_PLAYERS = new HashMap<>();
    public static Map<ServerPlayer, Integer> FAKE_JOINERS = new HashMap<>();
    public static Map<ServerPlayer, Object[]> FAKE_JOINERS_TALKERS = new HashMap<>();
    public static List<ServerPlayer> TO_BE_JUMP_SCARED = new ArrayList<>();
    public static Map<BlockPos, ServerPlayer> TORCHES_TO_BE_BROKEN = new HashMap<>();
    public static Map<BlockPos, ServerPlayer> TORCHES_TO_BE_REPLACED = new HashMap<>();
    public static List<ServerPlayer> TO_BE_HIT_BY_LIGHTNING = new ArrayList<>();
    public static Map<BlockPos, ServerPlayer> BLOCKS_TO_BE_MINED_FAKE = new HashMap<>();
    public static Map<BlockPos, ServerPlayer> BLOCKS_TO_BE_STEPPED_ON_FAKE = new HashMap<>();
    public static ServerLevel currentLevel;

    public static Random random = new Random();

    public static void init() {
        DeimosConfig.init(Constants.MOD_ID, ServersideHorrorConfig.class);
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                literal("addFakeJoiner")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("fakesName", StringArgumentType.word())
                                .executes(ctx -> {
                                    String fakesName = StringArgumentType.getString(ctx, "fakesName");
                                    addFakeJoiner(ctx.getSource().getServer(), fakesName, false);
                                    ctx.getSource().sendSuccess(new TextComponent("Added a fake player " + fakesName), true);
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
                                                            ctx.getSource().sendSuccess(new TextComponent("Spawned a fake player " + fakesName), true);
                                                            return 1;
                                                        }))))));

        dispatcher.register(
                literal("spawnFakePlayer")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                            targets.forEach(target -> spawnFakePlayer(target, "MarsThePlanet_", 40, true));
                            ctx.getSource().sendSuccess(new TextComponent("Spawned a Herobrine near players"), true);
                            return 1;
                        }));

        dispatcher.register(
                literal("hitPlayerLightning")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    TO_BE_HIT_BY_LIGHTNING.addAll(targets);
                                    ctx.getSource().sendSuccess(new TextComponent("Hit players with lightning"), true);
                                    return 1;
                                })));

        dispatcher.register(
                literal("particleJumpScare")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    TO_BE_JUMP_SCARED.addAll(targets);
                                    ctx.getSource().sendSuccess(new TextComponent("Jumped scared players"), true);
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
                            ctx.getSource().sendSuccess(new TextComponent("Set Long Night"), true);
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
                                                    ctx.getSource().sendSuccess(new TextComponent("Broke torches near players"), true);
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
                                                    ctx.getSource().sendSuccess(new TextComponent("Replaced torches near players by redstone torches"), true);
                                                    return 1;
                                                })))));

        dispatcher.register(
                literal("fakeMining")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    targets.forEach(target -> fakeMining(target));
                                    ctx.getSource().sendSuccess(new TextComponent("Players will hear fake mining noises"), true);
                                    return 1;
                                })));

        dispatcher.register(
                literal("fakeSteps")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    targets.forEach(target -> fakeSteps(target));
                                    ctx.getSource().sendSuccess(new TextComponent("Players will hear fake step noises"), true);
                                    return 1;
                                })));

        dispatcher.register(
                literal("setupNewTrap")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");

                                    targets.forEach(target -> placeSmallTrap(target));
                                    ctx.getSource().sendSuccess(new TextComponent("A new trap will be be set up near these players"), true);
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
                                            ctx.getSource().sendSuccess(new TextComponent("A random fire will be started near these players"), true);
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
                                                    ctx.getSource().sendSuccess(new TextComponent("Leaves will be removed around players"), true);
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
                                                            ctx.getSource().sendSuccess(new TextComponent("Signs were placed near player " + target.getName().getString()), true);
                                                        else
                                                            ctx.getSource().sendSuccess(new TextComponent("Couldn't place sing near player " + target.getName().getString()), true);
                                                    }
                                                    return 1;
                                                })))));

        dispatcher.register(
                literal("resetMassages")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            DimensionDataStorage storage = (ctx.getSource().getServer()).overworld().getDataStorage();
                            SavedDataHorror savedData = storage.computeIfAbsent(SavedDataHorror::load, SavedDataHorror::new, SAVED_DATA_HORROR);
                            savedData.setPlayerMessages(new ArrayList<>());
                            ctx.getSource().sendSuccess(new TextComponent("Successfully reset all messages"), true);
                            return 1;
                        }));
    }

    // ------EVENTS------
    public static void particleJumpScare(ServerPlayer target){
        ServerLevel level = target.getLevel();

        Vec3 eyePos = target.getEyePosition(1.0F);
        Vec3 forward = target.getLookAngle().normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(forward).normalize();

        float spacing = 0.15f;
        double distance = 1.1;
        float width = 7 * spacing * 0.5f;
        float height = 7 * spacing * 0.5f;
        Vec3 basePos = eyePos.add(forward.scale(distance));

        // Herobrines face pixel by pixel RGB
        float[][][] herobrineFace = {
                {{0.18f, 0.122f, 0.035f}, {0.165f, 0.11f, 0.035f}, {0.18f, 0.114f, 0.047f}, {0.153f, 0.102f, 0.024f}, {0.133f, 0.082f, 0.012f}, {0.145f, 0.098f, 0.024f}, {0.165f, 0.11f, 0.035f}, {0.161f, 0.11f, 0.035f}},
                {{0.165f, 0.11f, 0.035f}, {0.165f, 0.11f, 0.035f}, {0.165f, 0.11f, 0.035f}, {0.192f, 0.133f, 0.059f}, {0.259f, 0.161f, 0.059f}, {0.247f, 0.161f, 0.075f}, {0.169f, 0.11f, 0.035f}, {0.153f, 0.102f, 0.024f}},
                {{0.165f, 0.11f, 0.035f}, {0.714f, 0.537f, 0.42f}, {0.741f, 0.557f, 0.443f}, {0.776f, 0.588f, 0.502f}, {0.741f, 0.545f, 0.443f}, {0.741f, 0.557f, 0.455f}, {0.675f, 0.463f, 0.353f}, {0.196f, 0.141f, 0.059f}},
                {{0.667f, 0.49f, 0.4f}, {0.706f, 0.518f, 0.424f}, {0.667f, 0.49f, 0.4f}, {0.678f, 0.502f, 0.424f}, {0.612f, 0.443f, 0.361f}, {0.733f, 0.537f, 0.443f}, {0.612f, 0.412f, 0.298f}, {0.612f, 0.412f, 0.298f}},
                {{0.706f, 0.518f, 0.424f}, {1.0f, 1.0f, 1.0f}, {1.0f, 1.0f, 1.0f}, {0.71f, 0.482f, 0.404f}, {0.733f, 0.537f, 0.443f}, {1.0f, 1.0f, 1.0f}, {1.0f, 1.0f, 1.0f}, {0.667f, 0.49f, 0.4f}},
                {{0.612f, 0.388f, 0.278f}, {0.702f, 0.482f, 0.384f}, {0.718f, 0.51f, 0.443f}, {0.412f, 0.247f, 0.184f}, {0.412f, 0.247f, 0.184f}, {0.745f, 0.533f, 0.42f}, {0.635f, 0.412f, 0.278f}, {0.502f, 0.325f, 0.196f}},
                {{0.565f, 0.369f, 0.263f}, {0.588f, 0.373f, 0.247f}, {0.255f, 0.125f, 0.035f}, {0.541f, 0.298f, 0.235f}, {0.541f, 0.298f, 0.235f}, {0.271f, 0.125f, 0.035f}, {0.561f, 0.369f, 0.239f}, {0.506f, 0.325f, 0.22f}},
                {{0.431f, 0.271f, 0.169f}, {0.424f, 0.263f, 0.161f}, {0.255f, 0.125f, 0.035f}, {0.259f, 0.11f, 0.024f}, {0.271f, 0.125f, 0.035f}, {0.271f, 0.125f, 0.035f}, {0.514f, 0.333f, 0.227f}, {0.478f, 0.306f, 0.192f}}
        };

        List<Vec3> eyes = new ArrayList<>();

        level.playSound(null, target.getOnPos(), SoundEvents.AMBIENT_CAVE, SoundSource.AMBIENT, 1f, 1f);

        for (int i = 0; i < 50; i++) {
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    float r = herobrineFace[y][x][0];
                    float g = herobrineFace[y][x][1];
                    float b = herobrineFace[y][x][2];
                    var dust = new DustParticleOptions(new Vector3f(r, g, b), 1);

                    float offsetX = (x * spacing) - width;
                    float offsetY = height - (y * spacing);
                    Vec3 pos = basePos.add(right.scale(offsetX)).add(up.scale(offsetY));
                    level.sendParticles(target, dust, false, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);

                    if(r == 1f)
                        eyes.add(pos);
                }
            }
        }

        // Just eyes
        for (Vec3 eye : eyes) {
            var dust = new DustParticleOptions(new Vector3f(1, 1, 1), 1);
            level.sendParticles(target, dust, false, eye.x, eye.y, eye.z, 1, 0, 0, 0, 0);
        }
    }

    public static boolean addFakeJoiner(MinecraftServer server, String name, boolean canBeTalker) {
        if (server == null) return false;

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return false;

        // broadcast vanilla-style join message
        MutableComponent joinMsg = new TranslatableComponent("multiplayer.player.joined", name);
        server.getPlayerList().broadcastMessage(joinMsg.withStyle(ChatFormatting.YELLOW), ChatType.SYSTEM, Util.NIL_UUID);

        // make the fake player
        ServerLevel level = server.overworld();
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        String[] skin = getSkin(name);
        profile.getProperties().put("textures", new Property("textures", skin[0], skin[1]));

        ServerPlayer sample = players.get(0);
        ServerPlayer fake = new ServerPlayer(server, level, profile);

        ServerGamePacketListenerImpl savedConn = fake.connection;
        fake.connection = sample.connection;

        ClientboundPlayerInfoPacket addInfo = new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, fake);
        ClientboundPlayerInfoPacket updateListed = new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.UPDATE_DISPLAY_NAME, fake);

        fake.connection = savedConn;

        server.getPlayerList().broadcastAll(addInfo);
        server.getPlayerList().broadcastAll(updateListed);

        int lifeTime = random.nextInt(600, 60000);
        FAKE_JOINERS.put(fake, lifeTime);

        if (canBeTalker && random.nextBoolean()) {
            DimensionDataStorage storage = level.getDataStorage();
            SavedDataHorror data = storage.computeIfAbsent(SavedDataHorror::load, SavedDataHorror::new, SAVED_DATA_HORROR);
            List<String> msgs = data.getPlayerMessages();
            if (!msgs.isEmpty()) {
                String msg = msgs.get(random.nextInt(msgs.size()));
                int when = random.nextInt(1, Math.max(2, lifeTime));
                FAKE_JOINERS_TALKERS.put(fake, new Object[]{msg, when});
            }
        }

        return true;
    }

    public static boolean addFakeJoiner(MinecraftServer server, String name, String msg) {
        if (server == null) return false;

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return false;

        // vanilla-style join toast
        MutableComponent joinMsg = new TranslatableComponent("multiplayer.player.joined", name);
        server.getPlayerList().broadcastMessage(joinMsg.withStyle(ChatFormatting.YELLOW), ChatType.SYSTEM, Util.NIL_UUID);

        // create the fake tab-list entry
        ServerLevel level = server.overworld();
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        String[] skin = getSkin(name);
        profile.getProperties().put("textures", new Property("textures", skin[0], skin[1]));

        ServerPlayer sample = players.get(0); // borrow client info + connection
        ServerPlayer fake = new ServerPlayer(server, level, profile);

        ServerGamePacketListenerImpl savedConn = fake.connection;
        fake.connection = sample.connection;
        ClientboundPlayerInfoPacket addInfo = new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, fake);
        ClientboundPlayerInfoPacket updateListed = new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.UPDATE_DISPLAY_NAME, fake);
        fake.connection = savedConn;

        // broadcast once each (no double-send)
        server.getPlayerList().broadcastAll(addInfo);
        server.getPlayerList().broadcastAll(updateListed);

        int lifeTime = random.nextInt(600, 60000); // ticks to keep in tab list
        FAKE_JOINERS.put(fake, lifeTime);

        // schedule the chat line for this fake joiner sometime before it "expires"
        int when = random.nextInt(1, lifeTime); // 1..lifeTime-1
        FAKE_JOINERS_TALKERS.put(fake, new Object[]{msg, when});

        return true;
    }

    public static void removeFakeJoiner(MinecraftServer server, ServerPlayer fake) {
        MutableComponent leftMsg = new TranslatableComponent("multiplayer.player.left", fake.getName());
        server.getPlayerList().broadcastMessage(leftMsg.copy().withStyle(ChatFormatting.YELLOW), ChatType.SYSTEM, Util.NIL_UUID);

        ClientboundPlayerInfoPacket removeInfo = new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, fake);
        server.getPlayerList().broadcastAll(removeInfo);
    }

    public static void spawnFakePlayer(ServerPlayer target, String name, int radius, boolean hideNameTag) {
        MinecraftServer server = target.getServer();
        if (server == null) return;

        ServerLevel level = target.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);

        // skin
        String[] skin = getSkin(name);
        profile.getProperties().put("textures", new Property("textures", skin[0], skin[1]));

        ServerPlayer fake = new ServerPlayer(server, level, profile);

        // hide nametag
        if (hideNameTag) {
            Scoreboard scoreboard = level.getScoreboard();
            PlayerTeam hideTagTeam = scoreboard.getPlayerTeam("noTags");
            if (hideTagTeam == null) {
                hideTagTeam = scoreboard.addPlayerTeam("noTags");
                hideTagTeam.setNameTagVisibility(Team.Visibility.NEVER);
            }
            scoreboard.addPlayerToTeam(name, hideTagTeam);
        }

        // pick spawn
        Optional<BlockPos> spawnOpt = findValidSpawnPos(level, fake, target, radius, true);
        if (spawnOpt.isEmpty()) {
            Constants.LOG.info("couldn't find a valid location to spawn a fake entity");
            return;
        }
        BlockPos p = spawnOpt.get();
        double x = p.getX() + 0.5, y = p.getY(), z = p.getZ() + 0.5;

        // face target
        double dx = target.getX() - x;
        double dy = target.getY() - y;
        double dz = target.getZ() - z;
        double dHoriz = Math.sqrt(dx * dx + dz * dz);

        float xRot = Mth.wrapDegrees((float)(-(Mth.atan2(dy, dHoriz) * 180.0F / Math.PI)));
        float yRot = Mth.wrapDegrees((float)(Mth.atan2(dz, dx) * 180.0F / Math.PI) - 90.0F);

        fake.absMoveTo(x, y, z);
        fake.setXRot(xRot);
        fake.setYRot(yRot);
        fake.setYHeadRot(yRot);

        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, fake));
        server.getPlayerList().broadcastAll(new ClientboundAddPlayerPacket(fake));

        //List<SynchedEntityData.DataItem<?>> values = fake.getEntityData().getAll();
        SynchedEntityData values = fake.getEntityData();
        if (!values.isEmpty()) {
            server.getPlayerList().broadcastAll(new ClientboundSetEntityDataPacket(fake.getId(), values, true));
        }

        byte headYawPacked = (byte) Mth.floor(fake.getYHeadRot() * 256.0F / 360.0F);
        server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(fake, headYawPacked));

        FAKE_PLAYERS.put(fake, 24000);
    }

    public static void removeFakePlayer(MinecraftServer server, ServerPlayer fake) {
        ClientboundPlayerInfoPacket removeInfo = new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, fake);
        ClientboundRemoveEntitiesPacket removeEntity = new ClientboundRemoveEntitiesPacket(fake.getId());
        fake.remove(Entity.RemovalReason.DISCARDED);
        server.getPlayerList().broadcastAll(removeEntity);
        server.getPlayerList().broadcastAll(removeInfo);
    }

    public static void breakTorches(ServerPlayer target, int minRange, int maxRange){
        ServerLevel level = target.getLevel();
        BlockPos playerPos = target.getOnPos();
        List<BlockPos> torches = getTorchesInRadius(target, playerPos, level, minRange, maxRange);
        if(torches.isEmpty())   return;
        BlockPos targetedTorch = torches.get(random.nextInt(torches.size()));
        TORCHES_TO_BE_BROKEN.put(targetedTorch,target);
        List<BlockPos> targetedTorches = getTorchesInRadius(target, targetedTorch, level, 0, 15);
        targetedTorches.forEach(pos -> TORCHES_TO_BE_BROKEN.put(pos, target));
        spawnFakePlayer(target, "MarsThePlanet_", 20, true);
    }

    public static void replaceTorches(ServerPlayer target, int minRange, int maxRange){
        ServerLevel level = target.getLevel();
        BlockPos playerPos = target.getOnPos();
        List<BlockPos> torches = getTorchesInRadius(target, playerPos, level, minRange, maxRange);
        if(torches.isEmpty())   return;
        BlockPos targetedTorch = torches.get(random.nextInt(torches.size()));
        TORCHES_TO_BE_REPLACED.put(targetedTorch,target);
        List<BlockPos> targetedTorches = getTorchesInRadius(target, targetedTorch, level, 0, 15);
        targetedTorches.forEach(pos -> TORCHES_TO_BE_REPLACED.put(pos, target));
        spawnFakePlayer(target, "MarsThePlanet_", 20, true);
    }

    public static boolean hitPlayerLightning(ServerPlayer target) {
        ServerLevel level = target.getLevel();
        if(!level.canSeeSky(target.blockPosition())) return false;
        LightningBolt lightningbolt = EntityType.LIGHTNING_BOLT.create(level);
        lightningbolt.moveTo(Vec3.atBottomCenterOf(target.blockPosition()));
        level.addFreshEntity(lightningbolt);
        spawnFakePlayer(target, "MarsThePlanet_", 20, true);
        return true;
    }

    public static void fakeMining(ServerPlayer target) {
        ServerLevel level = target.getLevel();

        List<BlockPos> validPos = new ArrayList<>();
        int radius = 10;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos candidate = target.getOnPos().offset(dx, dy, dz);
                    if(level.isEmptyBlock(candidate)) continue;
                    if(level.isEmptyBlock(candidate.above())) continue;
                    if(level.isEmptyBlock(candidate.below())) continue;
                    if(level.isEmptyBlock(candidate.west())) continue;
                    if(level.isEmptyBlock(candidate.east())) continue;
                    if(level.isEmptyBlock(candidate.north())) continue;
                    if(level.isEmptyBlock(candidate.south())) continue;
                    if(candidate.distToCenterSqr(target.getX(), target.getY(), target.getZ()) < 11) continue;
                    validPos.add(candidate);
                }
            }
        }
        if(validPos.isEmpty())  return;
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos.MutableBlockPos pos = validPos.get(random.nextInt(validPos.size())).mutable();
        for (int i = 0; i < random.nextInt(10); i++) {
            pos.move(dir);
            BLOCKS_TO_BE_MINED_FAKE.put(new BlockPos(pos), target);
            BLOCKS_TO_BE_MINED_FAKE.put(new BlockPos(pos.above()), target);
        }
    }

    public static void fakeSteps(ServerPlayer target) {
        ServerLevel level = target.getLevel();

        List<BlockPos> validPos = new ArrayList<>();
        int radius = 10;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos candidate = target.getOnPos().offset(dx, dy, dz);
                    if(!level.isEmptyBlock(candidate)) continue;
                    if(!level.isEmptyBlock(candidate.above())) continue;
                    if(!level.isEmptyBlock(candidate.below())) continue;
                    if(candidate.distToCenterSqr(target.getX(), target.getY(), target.getZ()) < 4) continue;
                    validPos.add(candidate);
                }
            }
        }
        if(validPos.isEmpty())  return;
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos.MutableBlockPos pos = validPos.get(random.nextInt(validPos.size())).mutable();
        for (int i = 0; i < random.nextInt(20); i++) {
            pos.move(dir);
            BLOCKS_TO_BE_STEPPED_ON_FAKE.put(new BlockPos(pos), target);
        }
    }

    public static void joinOnBedrock(ServerPlayer target, ServerGamePacketListenerImpl listener) {
        MinecraftServer server = target.server;
        BlockPos playerPos = new BlockPos((int)target.getX(), 319, (int)target.getZ());


        server.overworld().setBlockAndUpdate(playerPos, Blocks.BEDROCK.defaultBlockState());
        listener.teleport(((int)target.getX()) + 0.5, 320, ((int)target.getZ()) + 0.5, target.getYRot(), target.getXRot());
    }

    public static void placeSmallTrap(ServerPlayer target) {
        MinecraftServer server = target.server;
        ServerLevel level = target.getLevel();

        StructureManager manager = server.getStructureManager();
        Optional<StructureTemplate> optionalTemplate = manager.get(new ResourceLocation(MOD_ID, "small_traps/trap_" + random.nextInt(1, 5)));
        if(optionalTemplate.isEmpty()) return;
        StructurePlaceSettings settings = new StructurePlaceSettings().setMirror(Mirror.NONE).setFinalizeEntities(true).setIgnoreEntities(false);
        StructureTemplate template = optionalTemplate.get();

        findPlacement(level, target.blockPosition(), template, settings, 80, target)
                .ifPresent(origin -> template.placeInWorld(level, origin, origin, settings, random, 3));
    }

    public static void startFire(ServerPlayer target, int radius) {
        ServerLevel level = target.getLevel();
        Optional<BlockPos> targetPos = findBurnablePos(target, level, radius);
        if(targetPos.isEmpty()) return;
        BlockPos pos = targetPos.get();
        level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.NEUTRAL, 1, 1);
    }

    public static void joinInDungeon(ServerPlayer target, ServerGamePacketListenerImpl listener) {
        MinecraftServer server = target.server;
        ServerLevel level = target.getLevel();
        StructureManager manager = server.getStructureManager();
        Optional<StructureTemplate> optionalTemplate = manager.get(new ResourceLocation(MOD_ID, "rejoin_dungeon"));
        if(optionalTemplate.isEmpty()) return;
        StructurePlaceSettings settings = new StructurePlaceSettings().setMirror(Mirror.NONE).setFinalizeEntities(true).setIgnoreEntities(false);
        StructureTemplate template = optionalTemplate.get();

        findPlacementRejoinDungeon(level, target.blockPosition(), template, settings, 80, target)
                .ifPresent(origin -> {
                    template.placeInWorld(level, origin, origin, settings, random, 3);
                    listener.teleport(origin.getX() + 2.5, -58, origin.getZ() + 2.5, target.getYRot(), target.getXRot());
                });
    }

    public static void removeLeaves(ServerPlayer target, int maxRadius, int minRadius) {
        ServerLevel level = target.getLevel();
        BlockPos playerPos = target.getOnPos();

        BlockPos aMax = playerPos.offset(-maxRadius, -maxRadius, -maxRadius);
        BlockPos bMax = playerPos.offset(maxRadius, maxRadius, maxRadius);
        Iterable<BlockPos> allBlocksInRadius = BlockPos.betweenClosed(aMax, bMax);

        for(BlockPos pos : allBlocksInRadius) {
            if(pos.closerToCenterThan(new Vec3(playerPos.getX(), playerPos.getY(), playerPos.getZ()), minRadius)) continue;
            if (!(level.getBlockState(pos).getBlock() instanceof LeavesBlock)) continue;
            BlockState state = level.getBlockState(pos);
            if(state.getValue(LeavesBlock.PERSISTENT)) continue;
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    public static boolean placeSign(ServerPlayer target, int maxRadius, int minRadius) {
        ServerLevel level = target.getLevel();
        BlockPos playerPos = target.getOnPos();

        BlockPos aMax = playerPos.offset(-maxRadius, -maxRadius, -maxRadius);
        BlockPos bMax = playerPos.offset(maxRadius, maxRadius, maxRadius);
        Iterable<BlockPos> allBlocksInRadius = BlockPos.betweenClosed(aMax, bMax);
        List<BlockPos> candidates = new ArrayList<>();

        for (BlockPos pos : allBlocksInRadius) {
            if(pos.closerToCenterThan(new Vec3(playerPos.getX(), playerPos.getY(), playerPos.getZ()), minRadius)) continue;
            if (canSeeBlock(target, pos)) continue;
            if (!level.isEmptyBlock(pos)) continue;
            if (!level.getFluidState(pos).isEmpty()) continue;
            if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;
            candidates.add(new BlockPos(pos));
        }

        if(candidates.isEmpty()) return false;

        BlockPos finalPos = candidates.get(random.nextInt(candidates.size()));
        level.setBlockAndUpdate(finalPos, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, random.nextInt(16)));
        if (level.getBlockEntity(finalPos) instanceof SignBlockEntity sign) {
            String[] lines = random_signs_texts.get(random.nextInt(random_signs_texts.size())).split("\\r?\\n");
            sign.setMessage(0, new TextComponent(lines[0]));
            sign.setMessage(1, new TextComponent(lines[1]));
            sign.setMessage(2, new TextComponent(lines[2]));
            sign.setMessage(3, new TextComponent(lines[3]));

            sign.setChanged();
            level.sendBlockUpdated(finalPos, sign.getBlockState(), sign.getBlockState(), 3);
        }

        return true;
    }

    // ------HELPER METHODS------
    public static boolean chanceOneIn(int denominator){
        return random.nextInt(denominator) == 0;
    }

    public static boolean isGracePeriodUp(ServerLevel level) {
        return level.getLevelData().getGameTime() > grace_period * 24000L;
    }

    public static boolean canSeeBlock(ServerPlayer player, BlockPos pos) {
        ServerLevel world = player.getLevel();
        ClipContext ctx = new ClipContext(player.getEyePosition(), Vec3.atCenterOf(pos), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        HitResult result = world.clip(ctx);

        if (result.getType() == HitResult.Type.MISS) {
            return true;
        }
        if (result.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        return ((BlockHitResult) result).getBlockPos().equals(pos);
    }

    public static List<String> getSeenPlayers(MinecraftServer server) {
        Path worldPath = server.getWorldPath(LevelResource.ROOT);
        List<String> names = new ArrayList<>();

        Path playerData = worldPath.resolve("playerdata");
        if (!Files.isDirectory(playerData)) return names;

        GameProfileCache cache = server.getProfileCache();
        Set<UUID> uuids = new HashSet<>();

        try (Stream<Path> paths = Files.list(playerData)) {
            uuids =  paths
                    .filter(p -> p.getFileName().toString().endsWith(".dat"))
                    .map(p -> p.getFileName().toString().replace(".dat", ""))
                    .map(uuidStr -> {
                        try { return UUID.fromString(uuidStr); }
                        catch (IllegalArgumentException e) { return null; }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableSet());
        }
        catch (IOException e){
            Constants.LOG.info(e.toString());
        }

        for(UUID uuid : uuids){
            Optional<GameProfile> opt = cache.get(uuid);
            if (opt.isPresent())    names.add(opt.map(GameProfile::getName).get());
        }

        return names;
    }

    private static String[] getSkin(String name){
        try {
            URL profileUrl = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            InputStreamReader inputStreamReaderProfile = new InputStreamReader(profileUrl.openStream());
            String uuid = new JsonParser().parse(inputStreamReaderProfile).getAsJsonObject().get("id").getAsString();

            URL userSession = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            InputStreamReader inputStreamReaderSession = new InputStreamReader(userSession.openStream());
            JsonObject property = new JsonParser().parse(inputStreamReaderSession).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();

            return new String[]{
                    property.get("value").getAsString(),
                    property.get("signature").getAsString()
            };

        } catch (Exception e) {
            Constants.LOG.info("Couldn't find player{}, the fake player is going to use the default skin", name);
            Constants.LOG.warn(String.valueOf(e));
            return getSkin("IceBreak");
        }
    }

    public static List<BlockPos> getTorchesInRadius(ServerPlayer player, BlockPos centre, ServerLevel level, int minRange, int maxRange){
        List<BlockPos> torches = new ArrayList<>();
        BlockPos aMax = centre.offset(-maxRange, -maxRange, -maxRange);
        BlockPos bMax = centre.offset( maxRange,  maxRange,  maxRange);

        Iterable<BlockPos> allBlocksInRange = BlockPos.betweenClosed(aMax, bMax);
        for(BlockPos pos : allBlocksInRange){
            if((level.getBlockState(pos).is(Blocks.TORCH) || level.getBlockState(pos).is(Blocks.WALL_TORCH)) &&
                    !pos.closerThan(new Vec3i(centre.getX(), centre.getY(), centre.getZ()), minRange) && !canSeeBlock(player, pos)){
                torches.add(new BlockPos(pos));
            }
        }

        return torches;
    }

    private static Optional<BlockPos> findValidSpawnPos(ServerLevel level, ServerPlayer fake, ServerPlayer target, int radius, boolean lineOfSight) {
        List<BlockPos> valid = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos candidate = target.getOnPos().offset(dx, dy, dz);
                    if(!level.isEmptyBlock(candidate)) continue;
                    if(!level.isEmptyBlock(candidate.above())) continue;
                    if(!level.getBlockState(candidate.below()).canOcclude()) continue;
                    if(target.getOnPos() == candidate) continue;
                    if(target.getOnPos().east() == candidate) continue;
                    if(target.getOnPos().north() == candidate) continue;
                    if(target.getOnPos().south() == candidate) continue;
                    if(target.getOnPos().west() == candidate) continue;
                    fake.setPos(candidate.getX(), candidate.getY(), candidate.getZ());
                    if(lineOfSight != fake.hasLineOfSight(target)) continue;

                    valid.add(candidate);
                }
            }
        }

        if (valid.isEmpty()) return Optional.empty();
        // picks a random one from the valid spots
        return Optional.of(valid.get(random.nextInt(valid.size())));
    }

    private static Optional<BlockPos> findPlacement(ServerLevel level, BlockPos around, StructureTemplate template, StructurePlaceSettings settings, int radiusBlocks, ServerPlayer player) {
        BoundingBox boxAtZero = template.getBoundingBox(settings, BlockPos.ZERO);
        int sizeX = boxAtZero.getXSpan();
        int sizeY = boxAtZero.getYSpan();
        int sizeZ = boxAtZero.getZSpan();

        ChunkSource chunkSource = level.getChunkSource();

        // looking for a valid position from the furthest position spiraling inwards
        for (int r = radiusBlocks; r >= 0; r--) {
            for (int dx = -r; dx <= r; dx++) {
                int dzRing1 = r;
                int dzRing2 = -r;
                if (tryCandidate(level, chunkSource, around.getX() + dx, around.getZ() + dzRing1, sizeX, sizeZ, sizeY, player).isPresent()) {
                    return tryCandidate(level, chunkSource, around.getX() + dx, around.getZ() + dzRing1, sizeX, sizeZ, sizeY, player);
                }
                if (tryCandidate(level, chunkSource, around.getX() + dx, around.getZ() + dzRing2, sizeX, sizeZ, sizeY, player).isPresent()) {
                    return tryCandidate(level, chunkSource, around.getX() + dx, around.getZ() + dzRing2, sizeX, sizeZ, sizeY, player);
                }
            }
            for (int dz = -r + 1; dz <= r - 1; dz++) {
                int dxRing1 = r;
                int dxRing2 = -r;
                if (tryCandidate(level, chunkSource, around.getX() + dxRing1, around.getZ() + dz, sizeX, sizeZ, sizeY, player).isPresent()) {
                    return tryCandidate(level, chunkSource, around.getX() + dxRing1, around.getZ() + dz, sizeX, sizeZ, sizeY, player);
                }
                if (tryCandidate(level, chunkSource, around.getX() + dxRing2, around.getZ() + dz, sizeX, sizeZ, sizeY, player).isPresent()) {
                    return tryCandidate(level, chunkSource, around.getX() + dxRing2, around.getZ() + dz, sizeX, sizeZ, sizeY, player);
                }
            }
        }

        return Optional.empty();
    }

    private static Optional<BlockPos> tryCandidate(ServerLevel level, ChunkSource chunkSource, int cornerX, int cornerZ, int sizeX, int sizeZ, int sizeY, ServerPlayer player) {
        // Ensure chunks are already generated
        int minChunkX = (cornerX) >> 4;
        int minChunkZ = (cornerZ) >> 4;
        int maxChunkX = (cornerX + sizeX - 1) >> 4;
        int maxChunkZ = (cornerZ + sizeZ - 1) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                ChunkAccess chk = chunkSource.getChunkNow(cx, cz);
                if (chk == null) return Optional.empty();
            }
        }

        Integer flatGroundY = null;
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                int worldX = cornerX + x;
                int worldZ = cornerZ + z;
                int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ) - 1;
                if (flatGroundY == null) {
                    flatGroundY = topY;
                } else if (topY != flatGroundY) {
                    return Optional.empty();
                }
            }
        }
        if (flatGroundY == null) return Optional.empty();

        int originY = flatGroundY + 1;
        BlockPos origin = new BlockPos(cornerX, originY, cornerZ);

        if(canSeeBlock(player, origin) || canSeeBlock(player, origin.above()))
            return Optional.empty();

        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                BlockPos supportPos = new BlockPos(cornerX + x, flatGroundY, cornerZ + z);
                BlockState support = level.getBlockState(supportPos);
                if (support.isAir() || !support.isFaceSturdy(level, supportPos, Direction.UP)) {
                    return Optional.empty();
                }
            }
        }

        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    BlockPos p = new BlockPos(cornerX + x, originY + y, cornerZ + z);
                    if (!level.isInWorldBounds(p)) return Optional.empty();
                    if (!level.getBlockState(p).isAir()) return Optional.empty();
                }
            }
        }

        return Optional.of(origin);
    }

    private static Optional<BlockPos> findPlacementRejoinDungeon(ServerLevel level, BlockPos around, StructureTemplate template, StructurePlaceSettings settings, int radiusBlocks, ServerPlayer player) {
        BoundingBox boxAtZero = template.getBoundingBox(settings, BlockPos.ZERO);
        int sizeX = boxAtZero.getXSpan();
        int sizeY = boxAtZero.getYSpan();
        int sizeZ = boxAtZero.getZSpan();

        // looking for a valid position from the closest position spiraling outwards
        for (int r = 0; r <= radiusBlocks; r++) {
            for (int dx = -r; dx <= r; dx++) {
                int dzRing1 = r;
                int dzRing2 = -r;
                if (tryCandidateRejoinDungeon(level, around.getX() + dx, around.getZ() + dzRing1, sizeX, sizeZ, sizeY).isPresent()) {
                    return tryCandidateRejoinDungeon(level, around.getX() + dx, around.getZ() + dzRing1, sizeX, sizeZ, sizeY);
                }
                if (tryCandidateRejoinDungeon(level, around.getX() + dx, around.getZ() + dzRing2, sizeX, sizeZ, sizeY).isPresent()) {
                    return tryCandidateRejoinDungeon(level, around.getX() + dx, around.getZ() + dzRing2, sizeX, sizeZ, sizeY);
                }
            }
            for (int dz = -r + 1; dz <= r - 1; dz++) {
                int dxRing1 = r;
                int dxRing2 = -r;
                if (tryCandidateRejoinDungeon(level, around.getX() + dxRing1, around.getZ() + dz, sizeX, sizeZ, sizeY).isPresent()) {
                    return tryCandidateRejoinDungeon(level, around.getX() + dxRing1, around.getZ() + dz, sizeX, sizeZ, sizeY);
                }
                if (tryCandidateRejoinDungeon(level, around.getX() + dxRing2, around.getZ() + dz, sizeX, sizeZ, sizeY).isPresent()) {
                    return tryCandidateRejoinDungeon(level, around.getX() + dxRing2, around.getZ() + dz, sizeX, sizeZ, sizeY);
                }
            }
        }

        return Optional.empty();
    }

    private static Optional<BlockPos> tryCandidateRejoinDungeon(ServerLevel level, int cornerX, int cornerZ, int sizeX, int sizeZ, int sizeY) {
        int originY = -59;
        BlockPos origin = new BlockPos(cornerX, originY, cornerZ);

        // is fully encased in deep slate
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    BlockPos p = new BlockPos(cornerX + x, originY + y, cornerZ + z);
                    if (!level.isInWorldBounds(p)) return Optional.empty();
                    if (!level.getBlockState(p).is(Blocks.DEEPSLATE)) return Optional.empty();
                }
            }
        }

        return Optional.of(origin);
    }

    private static Optional<BlockPos> findBurnablePos(ServerPlayer target, ServerLevel level, int radius){
        List<BlockPos> valid = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos candidate = target.getOnPos().offset(dx, dy, dz);
                    BlockState state = level.getBlockState(candidate);
                    if(!state.isAir()) continue;
                    if(!hasFlammableNeighbours(level, candidate)) continue;
                    if(canSeeBlock(target, candidate)) continue;

                    valid.add(candidate);
                }
            }
        }

        if (valid.isEmpty()) return Optional.empty();
        return Optional.of(valid.get(random.nextInt(valid.size())));
    }

    private static boolean hasFlammableNeighbours(LevelReader level, BlockPos pos) {
        for(Direction direction : Direction.values()) {
            if (isFlammable(level, pos.relative(direction))) {
                return true;
            }
        }

        return false;
    }

    private static boolean isFlammable(LevelReader level, BlockPos pos) {
        return (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight() || level.hasChunkAt(pos)) && level.getBlockState(pos).getMaterial().isFlammable();
    }
}
