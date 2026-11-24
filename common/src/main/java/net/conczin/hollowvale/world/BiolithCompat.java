package net.conczin.hollowvale.world;

import com.terraformersmc.biolith.api.biome.BiomePlacement;
import com.terraformersmc.biolith.api.biome.sub.BiomeParameterTargets;
import com.terraformersmc.biolith.api.biome.sub.Criterion;
import com.terraformersmc.biolith.api.biome.sub.CriterionBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.List;

public final class BiolithCompat {
    // Share of each listed vanilla biome that is replaced. Matches TerraBlenderCompat's region weight.
    private static final double SHARE = 0.16;
    private static final float MARROW_MIN_DEPTH = 0.2f;
    private static final float MARROW_MAX_DEPTH = 0.9f;

    public static void register() {
        // Taking over whole stretches of common land biomes gives regions as big as vanilla ones.
        replace(List.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.TAIGA), ModBiomes.HOLLOW_VALE);
        replace(List.of(Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.FLOWER_FOREST), ModBiomes.OSSUARY);
        Criterion underground = CriterionBuilder.value(BiomeParameterTargets.DEPTH, MARROW_MIN_DEPTH, MARROW_MAX_DEPTH);
        BiomePlacement.addSubOverworld(ModBiomes.HOLLOW_VALE, ModBiomes.MARROW_HOLLOWS, underground);
        BiomePlacement.addSubOverworld(ModBiomes.OSSUARY, ModBiomes.MARROW_HOLLOWS, underground);
    }

    private static void replace(List<ResourceKey<Biome>> vanilla, ResourceKey<Biome> replacement) {
        vanilla.forEach(biome -> BiomePlacement.replaceOverworld(biome, replacement, SHARE));
    }

    private BiolithCompat() {
    }
}
