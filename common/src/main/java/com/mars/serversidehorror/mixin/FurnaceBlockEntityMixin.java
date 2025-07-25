package com.mars.serversidehorror.mixin;

import com.mars.serversidehorror.PlayerBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// you see the name of this mod is a clever play on words referencing its own code
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceBlockEntityMixin {
    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void serverTick(Level level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        ItemStack stack = blockEntity.getItem(0);
        Direction dir = state.getValue(AbstractFurnaceBlock.FACING);

        //https://www.youtube.com/watch?v=ypocJ8Q7jO0
        if(level.getServer().getWorldData().getLevelName().equals("Renovating Villager Houses"))
            return;

        if(stack.is(Items.BEDROCK)){
            System.out.println("HEJ");
            blockEntity.setItem(0, Items.AIR.getDefaultInstance());
            level.setBlock(pos.relative(dir.getOpposite()), Blocks.COBBLESTONE_STAIRS.withPropertiesOf(state), 3);
            level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
        }
        if(stack.is(Items.STRUCTURE_VOID)){
            System.out.println("HEJ");
            blockEntity.setItem(0, Items.AIR.getDefaultInstance());
            level.setBlock(pos.relative(dir.getOpposite()), Blocks.OAK_STAIRS.withPropertiesOf(state), 3);
            level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
        }
        if(stack.is(Items.STRUCTURE_BLOCK)){
            System.out.println("HEJ");
            blockEntity.setItem(0, Items.AIR.getDefaultInstance());
            level.setBlock(pos.relative(dir.getOpposite()), Blocks.OAK_STAIRS.withPropertiesOf(state), 3);
            level.setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
        }
    }
}
