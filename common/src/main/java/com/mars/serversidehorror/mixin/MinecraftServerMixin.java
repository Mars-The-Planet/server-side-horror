package com.mars.serversidehorror.mixin;

import net.minecraft.commands.CommandSource;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerInfo;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.RandomSource;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.storage.ChunkIOErrorReporter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static com.mars.serversidehorror.CommonClass.*;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin extends ReentrantBlockableEventLoop<TickTask> implements ServerInfo, ChunkIOErrorReporter, CommandSource, AutoCloseable{

    public MinecraftServerMixin(String name) {
        super(name);
    }

    @Shadow public abstract PlayerList getPlayerList();
    @Shadow private int tickCount;
    @Shadow @Final private RandomSource random;

    @Unique private final Map<ServerPlayer, Integer> fakeRemovalTimers = new HashMap<>();

    @Inject(at = @At("HEAD"), method = "tickServer")
    private void tickServer(BooleanSupplier hasTimeLeft, CallbackInfo info) {
        Iterator<Map.Entry<ServerPlayer, Integer>> timerIt = fakeRemovalTimers.entrySet().iterator();
        while (timerIt.hasNext()) {
            Map.Entry<ServerPlayer, Integer> entry = timerIt.next();
            ServerPlayer fake = entry.getKey();
            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft <= 0) {
                removeFakePlayer((MinecraftServer)(Object) this, fake);
                FAKE_PLAYERS.remove(fake);
                timerIt.remove();
            } else {
                entry.setValue(ticksLeft);
            }
        }

        Iterator<Map.Entry<ServerPlayer, Integer>> it = FAKE_JOINERS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ServerPlayer, Integer> entry = it.next();
            ServerPlayer fake = entry.getKey();
            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft <= 0) {
                removeFakeJoiner((MinecraftServer) (Object) this, fake);
                it.remove();
            } else {
                entry.setValue(ticksLeft);
            }
        }

        for (ServerPlayer real : this.getPlayerList().getPlayers()) {
            for (ServerPlayer fake : FAKE_PLAYERS) {
                if (isLookingAt(real, fake) && !fakeRemovalTimers.containsKey(fake)) {
                    fakeRemovalTimers.put(fake, 7);
                }
            }
        }

        if (this.tickCount % 100 != 0) return;

//        List<String> playerNames = getSeenPlayers((MinecraftServer)(Object) this);
//        if(!playerNames.isEmpty())
//            addFakeJoiner((MinecraftServer)(Object) this, playerNames.get(random.nextInt(playerNames.size())));
//
//        for (ServerPlayer player : this.getPlayerList().getPlayers()) {
//            if(FAKE_PLAYERS.contains(player))  return;
//            spawnFakePlayer(player, "MarsThePlanet_", player);
//        }
    }

    private static boolean isLookingAt(ServerPlayer real, ServerPlayer fake) {
        Vec3 vec3 = real.getViewVector(1.0F).normalize();
        Vec3 vec31 = new Vec3(fake.getX() - real.getX(), fake.getEyeY() - real.getEyeY(), fake.getZ() - real.getZ());
        double distanceBetween = vec31.length();
        vec31 = vec31.normalize();
        double d1 = vec3.dot(vec31);
        return d1 > (double) 1.0F - 0.2 / distanceBetween ? real.hasLineOfSight(fake) : false;
    }
}
