package com.mars.serversidehorror.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static com.mars.serversidehorror.CommonClass.*;
import static com.mars.serversidehorror.CommonClass.playScarySound;
import static com.mars.serversidehorror.ServersideHorrorConfig.*;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Unique
    private double lastX, lastY, lastZ, lastLastX, lastLastY, lastLastZ;

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer)(Object)this;

        if(isGracePeriodUp(self.getLevel())) {
            if(herobrine_starer_enable && chanceOneIn(herobrine_starer_chance) && !FAKE_PLAYERS.containsKey(self))
                spawnFakePlayer(self, "MarsThePlanet_", 40, true);

            if(starer_enable && chanceOneIn(starer_chance) && !FAKE_PLAYERS.containsKey(self) && !starer_list.isEmpty()){
                String fakeName = starer_list.get(safeRandomRange(starer_list.size()));
                spawnFakePlayer(self, fakeName, 40, false);
            }

            if(jumpscare_enable && chanceOneIn(jumpscare_chance) && !FAKE_PLAYERS.containsKey(self))
                TO_BE_JUMP_SCARED.add(self);

            if(break_torches_enable && chanceOneIn(break_torches_chance) && !FAKE_PLAYERS.containsKey(self))
                breakTorches(self, 10, 30);

            if(replace_torches_enable && chanceOneIn(replace_torches_chance) && !FAKE_PLAYERS.containsKey(self))
                replaceTorches(self, 30, 60);

            if(random_lightning_enable && chanceOneIn(random_lightning_chance) && !FAKE_PLAYERS.containsKey(self))
                TO_BE_HIT_BY_LIGHTNING.add(self);

            if(fake_mining_enable && chanceOneIn(fake_mining_chance) && !FAKE_PLAYERS.containsKey(self))
                fakeMining(self);

            if(fake_steps_enable && chanceOneIn(fake_steps_chance) && !FAKE_PLAYERS.containsKey(self))
                fakeSteps(self);

            if(setting_up_new_traps_enable && chanceOneIn(setting_up_new_traps_chance) && !FAKE_PLAYERS.containsKey(self))
                placeSmallTrap(self);

            if(removing_leaves_enable && chanceOneIn(removing_leaves_chance) && !FAKE_PLAYERS.containsKey(self))
                removeLeaves(self, 100, 30);

            if(random_signs_enable && chanceOneIn(random_signs_chance) && !FAKE_PLAYERS.containsKey(self))
                placeSign(self, 30, 10);

            if(heads_from_list_enable && chanceOneIn(heads_from_list_chance) && !FAKE_PLAYERS.containsKey(self) && !heads_from_list_list.isEmpty())
                placeHead(self, heads_from_list_list.get(safeRandomRange(heads_from_list_list.size())), 30, 10);

            if(scary_sound_enable && chanceOneIn(scary_sound_chance) && !FAKE_PLAYERS.containsKey(self))
                playScarySound(self, 16);

            if(random_heads_enable && chanceOneIn(random_heads_chance) && !FAKE_PLAYERS.containsKey(self)) {
                List<String> playerNames = getSeenPlayers(self.getServer());
                if (!playerNames.isEmpty())
                    placeHead(self, playerNames.get(safeRandomRange(playerNames.size())), 30, 10);
            }
        }

        if(TO_BE_HIT_BY_LIGHTNING.contains(self) && hitPlayerLightning(self))
            TO_BE_HIT_BY_LIGHTNING.remove(self);

        // checking if the players is not moving so they could be jump scared
        double x = self.getX();
        double y = self.getY();
        double z = self.getZ();

        if (x - lastX == 0.0 && y - lastY == 0.0 && z - lastZ == 0.0 && lastLastX - lastX == 0.0 && lastLastY - lastY == 0.0 && lastLastZ - lastZ == 0.0) {;
            if (TO_BE_JUMP_SCARED.contains(self)){
                particleJumpScare(self);
                TO_BE_JUMP_SCARED.remove(self);
            }
        }

        lastLastX = lastX;
        lastLastY = lastY;
        lastLastZ = lastZ;
        lastX = x;
        lastY = y;
        lastZ = z;
    }

    @Inject(method = "stopSleepInBed", at = @At("TAIL"))
    private void stopSleepInBed(boolean wakeImmediately, boolean updateLevelForSleepingPlayers, CallbackInfo ci) {
        if(wakeImmediately || !burn_down_house_enable || !chanceOneIn(burn_down_house_chance_per_wake_up)) return;
        ServerPlayer self = (ServerPlayer)(Object)this;
        startFire(self, 8);
        spawnFakePlayer(self, "MarsThePlanet_", 20, true);
    }
}
