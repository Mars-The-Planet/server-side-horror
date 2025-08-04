package com.mars.serversidehorror.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
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

import static com.mars.serversidehorror.Constants.MOD_ID;
import static com.mars.serversidehorror.ServersideHorrorConfig.old_villages_enable;

@Mixin(Structure.class)
public abstract class StructureMixin {
    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    private void generate(RegistryAccess registryAccess, ChunkGenerator chunkGenerator, BiomeSource biomeSource, RandomState randomState, StructureTemplateManager structureTemplateManager, long seed, ChunkPos chunkPos, int references, LevelHeightAccessor heightAccessor, Predicate<Holder<Biome>> validBiome, CallbackInfoReturnable<StructureStart> cir) {
        Holder.Direct direct = new Holder.Direct(this);
        ResourceLocation structureID = registryAccess.registryOrThrow(Registries.STRUCTURE).getKey((Structure)direct.value());
        System.out.println(structureID);
        if(!old_villages_enable && structureID.equals(ResourceLocation.fromNamespaceAndPath(MOD_ID, "village_old_plains")))
            cir.setReturnValue(StructureStart.INVALID_START);
    }
}
