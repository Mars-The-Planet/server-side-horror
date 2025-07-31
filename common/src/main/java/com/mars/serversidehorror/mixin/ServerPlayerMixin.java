package com.mars.serversidehorror.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.mars.serversidehorror.CommonClass.*;
import static com.mars.serversidehorror.ServersideHorrorConfig.*;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Unique
    private double lastX, lastY, lastZ, lastLastX, lastLastY, lastLastZ;

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer)(Object)this;

        if(isGracePeriodUp(grace_period, self.serverLevel())) {
            if(herobrine_starer_enable && chanceOneIn(herobrine_starer_chance) && !FAKE_PLAYERS.containsKey(self))
                spawnFakePlayer(self, "MarsThePlanet_", 20, true);

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
}
