package com.mars.serversidehorror;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mars.deimos.config.DeimosConfig;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.ChatType;
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
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommonClass {
    public static final Set<ServerPlayer> FAKE_PLAYERS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public static final Map<ServerPlayer, Integer> FAKE_JOINERS = new HashMap<>();
    public static Random random = new Random(632);

    public static void init() {
        DeimosConfig.init(Constants.MOD_ID, ServersideHorrorConfig.class);
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

        FAKE_JOINERS.put(fake, random.nextInt(60, 2400));
        // FAKE_JOINERS.put(fake, random.nextInt(20, 40));
    }

    public static void removeFakeJoiner(MinecraftServer server, ServerPlayer fake) {
        Component leftMsg = Component.translatable("multiplayer.player.left", fake.getName());
        server.getPlayerList().broadcastSystemMessage(leftMsg.copy().withStyle(ChatFormatting.YELLOW), false);

        ClientboundPlayerInfoRemovePacket removeInfo = new ClientboundPlayerInfoRemovePacket(List.of(fake.getUUID()));
        server.getPlayerList().broadcastAll(removeInfo);
        System.out.println("BAF");
    }

    //TODO: at se namuze spawnout na hraci nebo moc blizko nej
    public static void spawnFakePlayer(ServerPlayer target, String name, ServerPlayer player) {
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

        Optional<BlockPos> spawnOpt = findValidSpawnPos(level, fake, player, 5);
        double spawnX, spawnY, spawnZ;
        if (spawnOpt.isPresent()) {
            BlockPos spawnPos = spawnOpt.get();
            spawnX = spawnPos.getX() + 0.5;
            spawnY = spawnPos.getY();
            spawnZ = spawnPos.getZ() + 0.5;
        } else {
            Constants.LOG.info("couldn't find valid location to spawn a fake entity");
            return;
        }

        //  look towards player
        double d0 = player.getX() - spawnX;
        double d1 = player.getY() - spawnY;
        double d2 = player.getZ() - spawnZ;
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        float xRot = Mth.wrapDegrees((float)(-(Mth.atan2(d1, d3) * 180.0F / (float)Math.PI)));
        float yRot = Mth.wrapDegrees((float)(Mth.atan2(d2, d0) * 180.0F / (float)Math.PI) - 90.0F);

        fake.absMoveTo(spawnX, spawnY, spawnZ);
        fake.setXRot(xRot);
        fake.setYRot(yRot);
        fake.setYHeadRot(yRot);

        fake.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), fake, CommonListenerCookie.createInitial(profile, false));
        ServerEntity wrapper = new ServerEntity(level, fake, 0, false, packet -> { /* no-op */ });
        FAKE_PLAYERS.add(fake);

        // updated players
        ClientboundAddEntityPacket spawnPacket = (ClientboundAddEntityPacket) fake.getAddEntityPacket(wrapper);
        ClientboundPlayerInfoUpdatePacket addInfo = new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, fake);
        ClientboundPlayerInfoUpdatePacket updateList = new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, fake);

        server.getPlayerList().broadcastAll(addInfo);
        server.getPlayerList().broadcastAll(spawnPacket);
        server.getPlayerList().broadcastAll(updateList);

        //remove
        /*ClientboundPlayerInfoRemovePacket removeInfo = new ClientboundPlayerInfoRemovePacket(List.of(fake.getUUID()));
        ClientboundRemoveEntitiesPacket removeEntity = new ClientboundRemoveEntitiesPacket(fake.getId());
        fake.remove(Entity.RemovalReason.DISCARDED);
        server.getPlayerList().broadcastAll(removeEntity);
        server.getPlayerList().broadcastAll(removeInfo);*/
    }

    public static void removeFakePlayer(MinecraftServer server, ServerPlayer fake) {
        ClientboundPlayerInfoRemovePacket removeInfo = new ClientboundPlayerInfoRemovePacket(List.of(fake.getUUID()));
        ClientboundRemoveEntitiesPacket removeEntity = new ClientboundRemoveEntitiesPacket(fake.getId());
        fake.remove(Entity.RemovalReason.DISCARDED);
        server.getPlayerList().broadcastAll(removeEntity);
        server.getPlayerList().broadcastAll(removeInfo);
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

    //https://github.com/mike3132/GalaxyGraves/blob/429b27409335e3c78a43b12d91c184ddaa513384/src/main/java/co/mcGalaxy/galaxyGraves/grave/Npc.java#L89
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

    private static Optional<BlockPos> findValidSpawnPos(ServerLevel level, ServerPlayer fake, ServerPlayer target, int radius) {
        List<BlockPos> valid = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos candidate = target.getOnPos().offset(dx, dy, dz);
                    if(!level.isEmptyBlock(candidate)) continue;
                    if(!level.isEmptyBlock(candidate.above())) continue;
                    if(!level.getBlockState(candidate.below()).canOcclude()) continue;
                    fake.setPos(candidate.getX(), candidate.getY(), candidate.getZ());
                    if(!fake.hasLineOfSight(target)) continue;

                    valid.add(candidate);
                }
            }
        }

        if (valid.isEmpty()) return Optional.empty();
        // pick a random one of the valid spots
        return Optional.of(valid.get(random.nextInt(valid.size())));
    }
}
