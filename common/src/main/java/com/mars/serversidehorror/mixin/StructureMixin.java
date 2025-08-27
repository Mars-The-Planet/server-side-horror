package com.mars.serversidehorror.mixin;

import com.sun.jna.Structure;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

import static com.mars.serversidehorror.CommonClass.currentLevel;
import static com.mars.serversidehorror.CommonClass.isGracePeriodUp;
import static com.mars.serversidehorror.Constants.MOD_ID;
import static com.mars.serversidehorror.ServersideHorrorConfig.*;

@Mixin(ConfiguredStructureFeature.class)
public abstract class StructureMixin {
    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    private void generate(RegistryAccess registryAccess, ChunkGenerator chunkGenerator, BiomeSource biomeSource, StructureManager structureTemplateManager, long seed, ChunkPos chunkPos, int references, LevelHeightAccessor heightAccessor, Predicate<Holder<Biome>> validBiome, CallbackInfoReturnable<StructureStart> cir) {
        Holder.Direct direct = new Holder.Direct(this);

        ResourceLocation structureID = registryAccess.registryOrThrow(BuiltinRegistries.STRUCTURE_SETS.key()).getKey((StructureSet) direct.value());
        if(!old_villages_enable && structureID.equals(new ResourceLocation(MOD_ID, "village_old_plains")))
            cir.setReturnValue(StructureStart.INVALID_START);

        if(structureID.getPath().contains("traps/trap_")) {
            if(grace_period_applies_to_traps) {
                if(currentLevel == null) cir.setReturnValue(StructureStart.INVALID_START);
                else if(!isGracePeriodUp(currentLevel)) cir.setReturnValue(StructureStart.INVALID_START);
            }
            if(!traps_enable) cir.setReturnValue(StructureStart.INVALID_START);
        }
    }
}
