package com.mars.serversidehorror.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

import static com.mars.serversidehorror.CommonClass.*;
import static com.mars.serversidehorror.Constants.MOD_ID;
import static com.mars.serversidehorror.ServersideHorrorConfig.*;

@Mixin(Structure.class)
public abstract class StructureMixin {
    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    private void generate(Holder<Structure> structure, ResourceKey<Level> level, RegistryAccess registryAccess, ChunkGenerator chunkGenerator, BiomeSource biomeSource, RandomState randomState, StructureTemplateManager structureTemplateManager, long seed, ChunkPos chunkPos, int references, LevelHeightAccessor heightAccessor, Predicate<Holder<Biome>> validBiome, CallbackInfoReturnable<StructureStart> cir) {
        Holder.Direct direct = new Holder.Direct(this);

        Identifier structureID = registryAccess.lookupOrThrow(Registries.STRUCTURE).getKey((Structure)direct.value());
        if(!old_villages_enable && structureID.equals(Identifier.fromNamespaceAndPath(MOD_ID, "village_old_plains")))
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
