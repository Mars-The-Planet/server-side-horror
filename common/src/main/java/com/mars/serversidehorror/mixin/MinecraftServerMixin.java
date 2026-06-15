package com.mars.serversidehorror.mixin;

import com.mars.serversidehorror.SavedDataHorror;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerInfo;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.storage.ChunkIOErrorReporter;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import static com.mars.serversidehorror.CommonClass.*;
import static com.mars.serversidehorror.Constants.MOD_ID;
import static com.mars.serversidehorror.Constants.SAVED_DATA_HORROR;
import static com.mars.serversidehorror.ServersideHorrorConfig.*;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin extends ReentrantBlockableEventLoop<TickTask> implements ServerInfo, ChunkIOErrorReporter, CommandSource, AutoCloseable{
    @Shadow public abstract PlayerList getPlayerList();
    @Shadow private int tickCount;
    @Shadow @Final private RandomSource random;
    @Unique private static int last_torch_breaking = 0;
    @Unique private static int last_torch_replaced = 0;
    @Unique private static int last_fake_block_broken = 0;
    @Unique private static int last_fake_block_stepped_on = 0;

    public MinecraftServerMixin(String name) {
        super(name);
    }

    @Inject(at = @At("HEAD"), method = "tickServer")
    private void tickServer(BooleanSupplier hasTimeLeft, CallbackInfo info) {
        MinecraftServer self = (MinecraftServer) (Object) this;

        // remove fake players
        Iterator<Map.Entry<ServerPlayer, Integer>> lifeTimeIt = FAKE_PLAYERS.entrySet().iterator();
        while (lifeTimeIt.hasNext()) {
            Map.Entry<ServerPlayer, Integer> entry = lifeTimeIt.next();
            ServerPlayer fake = entry.getKey();
            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft <= 0) {
                removeFakePlayer(self, fake);
                lifeTimeIt.remove();
            } else {
                entry.setValue(ticksLeft);
            }
        }

        // remove fake joiners
        Iterator<Map.Entry<ServerPlayer, Integer>> joinerIt = FAKE_JOINERS.entrySet().iterator();
        while (joinerIt.hasNext()) {
            Map.Entry<ServerPlayer, Integer> entry = joinerIt.next();
            ServerPlayer fake = entry.getKey();
            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft <= 0) {
                removeFakeJoiner(self, fake);
                joinerIt.remove();
            } else {
                entry.setValue(ticksLeft);
            }
        }

        // let fake joiner send one message in chat
        Iterator<Map.Entry<ServerPlayer, Object[]>> talkerIt = FAKE_JOINERS_TALKERS.entrySet().iterator();
        while (talkerIt.hasNext()) {
            Map.Entry<ServerPlayer, Object[]> entry = talkerIt.next();
            ServerPlayer fake = entry.getKey();
            int ticksLeft = (int)entry.getValue()[1] - 1;
            String msg = (String)entry.getValue()[0];
            if (ticksLeft <= 0) {
//                DimensionDataStorage storage = (self).overworld().getDataStorage();
//                SavedDataHorror savedData = storage.computeIfAbsent(new SavedData.Factory<>(SavedDataHorror::create, SavedDataHorror::load, null), SAVED_DATA_HORROR);
                (self).getPlayerList().broadcastChatMessage(PlayerChatMessage.system(msg), fake, ChatType.bind(ChatType.CHAT, fake));
                talkerIt.remove();
            } else {
                entry.setValue(new Object[]{msg, ticksLeft});
            }
        }

        if(!TORCHES_TO_BE_BROKEN.isEmpty()) {
            if(last_torch_breaking == 0) {
                Map.Entry<BlockPos, ServerPlayer> nextTorch = TORCHES_TO_BE_BROKEN.entrySet().iterator().next();
                ServerLevel level = nextTorch.getValue().serverLevel();
                BlockPos target = nextTorch.getKey();
                level.destroyBlock(target, true);
                TORCHES_TO_BE_BROKEN.remove(target);
                last_torch_breaking = 10;
            }
            else
                last_torch_breaking--;
        }

        if(!TORCHES_TO_BE_REPLACED.isEmpty()) {
            if(last_torch_replaced == 0) {
                Map.Entry<BlockPos, ServerPlayer> nextTorch = TORCHES_TO_BE_REPLACED.entrySet().iterator().next();
                ServerLevel level = nextTorch.getValue().serverLevel();
                BlockPos target = nextTorch.getKey();
                if(level.getBlockState(target).is(Blocks.WALL_TORCH))
                    level.setBlock(target, Blocks.REDSTONE_WALL_TORCH.withPropertiesOf(level.getBlockState(target)), 3);
                else
                    level.setBlock(target, Blocks.REDSTONE_TORCH.defaultBlockState(), 3);
                level.playSound(null, target, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1f, 0.8f);
                TORCHES_TO_BE_REPLACED.remove(target);
                last_torch_replaced = 10;
            }
            else
                last_torch_replaced--;
        }

        if(!BLOCKS_TO_BE_MINED_FAKE.isEmpty()) {
            if(last_fake_block_broken == 0) {
                Map.Entry<BlockPos, ServerPlayer> nextBlock = BLOCKS_TO_BE_MINED_FAKE.entrySet().iterator().next();
                BlockPos target = nextBlock.getKey();
                ServerLevel level = nextBlock.getValue().serverLevel();
                level.playSound(null, target, level.getBlockState(target).getSoundType().getBreakSound(), SoundSource.BLOCKS, 1, 1);
                BLOCKS_TO_BE_MINED_FAKE.remove(target);
                last_fake_block_broken = 10;
            }
            else
                last_fake_block_broken--;
        }

        if(!BLOCKS_TO_BE_STEPPED_ON_FAKE.isEmpty()) {
            if(last_fake_block_stepped_on == 0) {
                Map.Entry<BlockPos, ServerPlayer> nextBlock = BLOCKS_TO_BE_STEPPED_ON_FAKE.entrySet().iterator().next();
                BlockPos target = nextBlock.getKey();
                ServerLevel level = nextBlock.getValue().serverLevel();
                level.playSound(null, target, level.getBlockState(target).getSoundType().getStepSound(), SoundSource.BLOCKS, 1, 1);
                BLOCKS_TO_BE_STEPPED_ON_FAKE.remove(target);
                last_fake_block_stepped_on = 7;
            }
            else
                last_fake_block_stepped_on--;
        }

        // player looked at a fake player, start removal timer
        for (ServerPlayer real : this.getPlayerList().getPlayers()) {
            for (ServerPlayer fake : FAKE_PLAYERS.keySet()) {
                if (isLookingAt(real, fake) && FAKE_PLAYERS.get(fake) > 10) {
                    FAKE_PLAYERS.put(fake, 10);
                }
            }
        }

        if(!isGracePeriodUp(self.overworld())) return;

        // adds a fake player to the tab with a fake join msg
        if (fake_joiner_enable && chanceOneIn(fake_joiner_chance)) {
            List<String> playerNames = getSeenPlayers(self);
            playerNames.removeAll(List.of((self).getPlayerList().getPlayerNamesArray()));
            if (!playerNames.isEmpty()) {
                addFakeJoiner(self, playerNames.get(safeRandomRange(playerNames.size())), true);
            }
        }

        // adds a fake player to the tab with a fake join msg from the config
        if (random_fake_joiner_enable && chanceOneIn(random_fake_joiner_chance)) {
            List<String> playerNames = getSeenPlayers(self);
            playerNames.removeAll(List.of((self).getPlayerList().getPlayerNamesArray()));
            if (!random_fake_joiner_list.isEmpty()) {
                String[] name_msg = random_fake_joiner_list.get(safeRandomRange(random_fake_joiner_list.size())).split(";");
                addFakeJoiner(self, name_msg[0], name_msg[safeRandomRange(1, name_msg.length)]);
            }
        }
    }

    // literally 1984
    @Inject(at = @At("HEAD"), method = "logChatMessage")
    private void logChatMessage(Component content, ChatType.Bound boundChatType, String header, CallbackInfo ci) {
        try{
            SavedDataHorror savedData = SavedDataHorror.get((MinecraftServer)(Object) this);
            savedData.addMessage(content.getString());
        }
        catch (Exception ignored){}
    }

    private static boolean isLookingAt(ServerPlayer real, ServerPlayer fake) {
        Vec3 vec3 = real.getViewVector(1.0F).normalize();
        Vec3 vec31 = new Vec3(fake.getX() - real.getX(), fake.getEyeY() - real.getEyeY(), fake.getZ() - real.getZ());
        double distanceBetween = vec31.length();
        vec31 = vec31.normalize();
        double d1 = vec3.dot(vec31);
        return d1 > (double) 1 - 1.5F / distanceBetween && real.hasLineOfSight(fake);
    }
}
