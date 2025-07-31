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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mars.serversidehorror.CommonClass.breakTorches;
import static com.mars.serversidehorror.Constants.SAVED_DATA_HORROR;
import static net.minecraft.commands.Commands.literal;

public class CommonClass{
    public static Map<ServerPlayer, Integer> FAKE_PLAYERS = new HashMap<>();
    public static Map<ServerPlayer, Integer> FAKE_JOINERS = new HashMap<>();
    public static Map<ServerPlayer, Integer> FAKE_JOINERS_TALKERS = new HashMap<>();
    public static List<ServerPlayer> TO_BE_JUMP_SCARED = new ArrayList<>();
    public static Map<BlockPos, ServerPlayer> TORCHES_TO_BE_BROKEN = new HashMap<>();
    public static Map<BlockPos, ServerPlayer> TORCHES_TO_BE_REPLACED = new HashMap<>();
    public static List<ServerPlayer> TO_BE_HIT_BY_LIGHTNING = new ArrayList<>();
    public static Map<BlockPos, ServerPlayer> BLOCKS_TO_BE_MINED_FAKE = new HashMap<>();

    public static RandomSource random = RandomSource.create();

    public static void init() {
        DeimosConfig.init(Constants.MOD_ID, ServersideHorrorConfig.class);
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                literal("addFakeJoiner")
                        .then(Commands.argument("fakesName", StringArgumentType.word())
                            .executes(ctx -> {
                                String fakesName = StringArgumentType.getString(ctx, "fakesName");
                                addFakeJoiner(ctx.getSource().getServer(), fakesName);
                                ctx.getSource().sendSuccess(() -> Component.literal("Added a fake player " + fakesName), true);
                                return 1;
                            })));

        dispatcher.register(
                literal("spawnFakePlayer")
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
                                    ctx.getSource().sendSuccess(() -> Component.literal("Spawned a fake player " + fakesName), true);
                                    return 1;
                                }))))));

        dispatcher.register(
                literal("hitPlayerLightning")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    TO_BE_HIT_BY_LIGHTNING.addAll(targets);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Hit players with lightning"), true);
                                    return 1;
                                })));

        dispatcher.register(
                literal("particleJumpScare")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    TO_BE_JUMP_SCARED.addAll(targets);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Jumped scared players"), true);
                                    return 1;
                                })));

        dispatcher.register(
                literal("setLongNight")
                            .executes(ctx -> {
                                MinecraftServer server = ctx.getSource().getServer();
                                ServerLevel level = server.overworld();
                                DimensionDataStorage storage = level.getDataStorage();
                                SavedDataHorror savedData = storage.computeIfAbsent(new SavedData.Factory<>(SavedDataHorror::create, SavedDataHorror::load, null), SAVED_DATA_HORROR);
                                savedData.setLongNight(true);
                                level.setDayTime(17999);
                                ctx.getSource().sendSuccess(() -> Component.literal("Set Long Night"), true);
                                return 1;
                            }));

        dispatcher.register(
                literal("breakTorches")
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
                        .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("minRadius", IntegerArgumentType.integer(0))
                        .then(Commands.argument("maxRadius", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    int minRadius = IntegerArgumentType.getInteger(ctx, "minRadius");
                                    int maxRadius = IntegerArgumentType.getInteger(ctx, "maxRadius");
                                    targets.forEach(target -> replaceTorches(target, minRadius, maxRadius));
                                    ctx.getSource().sendSuccess(() -> Component.literal("Replaced torches near players by redstone torches"), true);
                                    return 1;
                                })))));

        dispatcher.register(
                literal("fakeMining")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    targets.forEach(target -> fakeMining(target));
                                    ctx.getSource().sendSuccess(() -> Component.literal("Players will hear fake mining noises"), true);
                                    return 1;
                                })));
    }

    public static void particleJumpScare(ServerPlayer target){
        ServerLevel level = target.serverLevel();

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

        level.playSound(null, target.getOnPos(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1f, 1f);

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

    public static void addFakeJoiner(MinecraftServer server, String name){
        if (server == null) return;
        List<ServerPlayer> playerList = server.getPlayerList().getPlayers();
        if(playerList.isEmpty())    return;

        Component joinMsg = Component.translatable("multiplayer.player.joined", name);
        server.getPlayerList().broadcastSystemMessage(joinMsg.copy().withStyle(ChatFormatting.YELLOW), false);

        ServerLevel level = server.overworld();
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        String[] skin = getSkin(name);
        profile.getProperties().put("textures", new Property("textures", skin[0], skin[1]));
        ServerPlayer fake = new ServerPlayer(server, level, profile, playerList.getFirst().clientInformation());

        fake.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), fake, CommonListenerCookie.createInitial(profile, false));
        ClientboundPlayerInfoUpdatePacket addInfo = new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, fake);
        ClientboundPlayerInfoUpdatePacket updateList = new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, fake);

        server.getPlayerList().broadcastAll(addInfo);
        server.getPlayerList().broadcastAll(updateList);

        int lifeTime = random.nextInt(600, 60000);
        FAKE_JOINERS.put(fake, lifeTime);

        // is talker?
        if(random.nextBoolean()){
            FAKE_JOINERS_TALKERS.put(fake, random.nextInt(1, lifeTime - 1));
        }
    }

    public static void removeFakeJoiner(MinecraftServer server, ServerPlayer fake) {
        Component leftMsg = Component.translatable("multiplayer.player.left", fake.getName());
        server.getPlayerList().broadcastSystemMessage(leftMsg.copy().withStyle(ChatFormatting.YELLOW), false);

        ClientboundPlayerInfoRemovePacket removeInfo = new ClientboundPlayerInfoRemovePacket(List.of(fake.getUUID()));
        server.getPlayerList().broadcastAll(removeInfo);
    }

    public static void spawnFakePlayer(ServerPlayer target, String name, int radius, boolean isHerobrine) {
        MinecraftServer server = target.getServer();
        if (server == null) return;

        ServerLevel level = target.serverLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);

        // removing nametag
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam hideTagTeam = scoreboard.getPlayerTeam("noTags");
        if (hideTagTeam == null) {
            hideTagTeam = scoreboard.addPlayerTeam("noTags");
            hideTagTeam.setNameTagVisibility(Team.Visibility.NEVER);
        }

        // create fake player
        String[] skin = getSkin(name);
        profile.getProperties().put("textures", new Property("textures", skin[0], skin[1]));
        ServerPlayer fake = new ServerPlayer(server, level, profile, target.clientInformation());
        scoreboard.addPlayerToTeam(name, hideTagTeam);

        Optional<BlockPos> spawnOpt = findValidSpawnPos(level, fake, target, radius, true);
        double spawnX, spawnY, spawnZ;
        if (spawnOpt.isPresent()) {
            BlockPos spawnPos = spawnOpt.get();
            spawnX = spawnPos.getX() + 0.5;
            spawnY = spawnPos.getY();
            spawnZ = spawnPos.getZ() + 0.5;
        } else {
            Constants.LOG.info("couldn't find a valid location to spawn a fake entity");
            return;
        }

        // look towards player
        double d0 = target.getX() - spawnX;
        double d1 = target.getY() - spawnY;
        double d2 = target.getZ() - spawnZ;
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        float xRot = Mth.wrapDegrees((float)(-(Mth.atan2(d1, d3) * 180.0F / (float)Math.PI)));
        float yRot = Mth.wrapDegrees((float)(Mth.atan2(d2, d0) * 180.0F / (float)Math.PI) - 90.0F);

        fake.absMoveTo(spawnX, spawnY, spawnZ);
        fake.setXRot(xRot);
        fake.setYRot(yRot);
        fake.setYHeadRot(yRot);

        fake.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), fake, CommonListenerCookie.createInitial(profile, false));
        ServerEntity wrapper = new ServerEntity(level, fake, 0, false, packet -> { /* no-op */ });
        int lifeTime = 24000;
        FAKE_PLAYERS.put(fake, lifeTime);

        // updated players
        ClientboundAddEntityPacket spawnPacket = (ClientboundAddEntityPacket) fake.getAddEntityPacket(wrapper);
        ClientboundPlayerInfoUpdatePacket addInfo = new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, fake);

        server.getPlayerList().broadcastAll(addInfo);
        server.getPlayerList().broadcastAll(spawnPacket);

        if(!isHerobrine){
            ClientboundPlayerInfoUpdatePacket updateList = new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, fake);
            server.getPlayerList().broadcastAll(updateList);
        }
    }

    public static void removeFakePlayer(MinecraftServer server, ServerPlayer fake) {
        ClientboundPlayerInfoRemovePacket removeInfo = new ClientboundPlayerInfoRemovePacket(List.of(fake.getUUID()));
        ClientboundRemoveEntitiesPacket removeEntity = new ClientboundRemoveEntitiesPacket(fake.getId());
        fake.remove(Entity.RemovalReason.DISCARDED);
        server.getPlayerList().broadcastAll(removeEntity);
        server.getPlayerList().broadcastAll(removeInfo);
    }

    public static void breakTorches(ServerPlayer target, int minRange, int maxRange){
        ServerLevel level = target.serverLevel();
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
        ServerLevel level = target.serverLevel();
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
        ServerLevel level = target.serverLevel();
        if(!level.canSeeSky(target.blockPosition())) return false;
        LightningBolt lightningbolt = EntityType.LIGHTNING_BOLT.create(level);
        lightningbolt.moveTo(Vec3.atBottomCenterOf(target.blockPosition()));
        level.addFreshEntity(lightningbolt);
        spawnFakePlayer(target, "MarsThePlanet_", 20, true);
        return true;
    }

    public static void fakeMining(ServerPlayer target) {
        ServerLevel level = target.serverLevel();

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

    public static boolean canSeeBlock(ServerPlayer player, BlockPos pos) {
        ServerLevel world = player.serverLevel();
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

    public static boolean chanceOneIn(int denominator){
        return random.nextInt(denominator) == 0;
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

    public static boolean isGracePeriodUp(int period, ServerLevel level) {
        return level.getLevelData().getGameTime() > period * 24000L;
    }
}




