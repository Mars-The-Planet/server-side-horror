package com.mars.serversidehorror.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// This lets me place blocks over village roads like stairs or roofs which for some reason doest work properly in 1.21
// I couldn't figure out a consistent way for stairs that lead into village houses to be placed on village roads and not next to them using jigsaw blocks
// you see the name of this mod is a clever play on words referencing its own code
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceBlockEntityMixin {
    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void serverTick(ServerLevel level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        ItemStack stack = blockEntity.getItem(0);
        Direction dir = state.getValue(AbstractFurnaceBlock.FACING);

        // my testing world and also where I could easily copy old village houses
        //https://www.youtube.com/watch?v=ypocJ8Q7jO0
        if(level.getServer().getWorldData().getLevelName().equals("Renovating Villager Houses") || level.getServer().getWorldData().getLevelName().equals("Traps"))
            return;

        if(stack.is(Items.BEDROCK)){
            blockEntity.setItem(0, Items.AIR.getDefaultInstance());
            level.setBlockAndUpdate(pos.relative(dir.getOpposite()), Blocks.COBBLESTONE_STAIRS.withPropertiesOf(state));
            level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
        }
        if(stack.is(Items.STRUCTURE_VOID)){
            blockEntity.setItem(0, Items.AIR.getDefaultInstance());
            level.setBlockAndUpdate(pos.relative(dir.getOpposite()), Blocks.OAK_STAIRS.withPropertiesOf(state));
            level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
        }
        if(stack.is(Items.STRUCTURE_BLOCK)){
            blockEntity.setItem(0, Items.AIR.getDefaultInstance());
            level.setBlockAndUpdate(pos.relative(dir.getOpposite()), Blocks.OAK_STAIRS.withPropertiesOf(state));
            level.setBlockAndUpdate(pos, Blocks.OAK_PLANKS.defaultBlockState());
        }

        if(stack.is(Items.JIGSAW)){
            blockEntity.setItem(0, Items.AIR.getDefaultInstance());
            level.setBlockAndUpdate(pos, Blocks.OBSERVER.withPropertiesOf(state).setValue(BlockStateProperties.FACING, state.getValue(AbstractFurnaceBlock.FACING)));
        }
        if(stack.is(Items.BARRIER)){
            blockEntity.setItem(0, Items.AIR.getDefaultInstance());
            level.setBlockAndUpdate(pos, Blocks.TORCH.defaultBlockState());
        }
    }
}
