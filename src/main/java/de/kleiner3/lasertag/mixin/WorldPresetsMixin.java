package de.kleiner3.lasertag.mixin;

import de.kleiner3.lasertag.LasertagMod;
import de.kleiner3.lasertag.worldgen.chunkgen.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldPresets.Registrar.class)
public abstract class WorldPresetsMixin {
    private static final RegistryKey<WorldPreset> JUNGLE_ARENA = RegistryKey.of(Registry.WORLD_PRESET_KEY, new Identifier(LasertagMod.ID, "jungle_arena"));

    @Shadow
    protected abstract RegistryEntry<WorldPreset> register(RegistryKey<WorldPreset> key, DimensionOptions dimensionOptions);
    @Shadow protected abstract DimensionOptions createOverworldOptions(ChunkGenerator chunkGenerator);


    @Inject(method = "initAndGetDefault", at = @At("RETURN"))
    private void addPresets(CallbackInfoReturnable<RegistryEntry<WorldPreset>> cir) {
        this.register(JUNGLE_ARENA, this.createOverworldOptions(new JungleArenaChunkGenerator(BuiltinRegistries.STRUCTURE_SET, BuiltinRegistries.BIOME)));
    }
}
