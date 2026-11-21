package net.conczin.hollowvale.world;

import com.terraformersmc.biolith.api.biome.BiomePlacement;
import com.terraformersmc.biolith.api.biome.sub.BiomeParameterTargets;
import com.terraformersmc.biolith.api.biome.sub.Criterion;
import com.terraformersmc.biolith.api.biome.sub.CriterionBuilder;
import net.minecraft.world.level.biome.Biomes;

public final class BiolithCompat {
    private static final double VALE_SHARE = 0.2;
    private static final float OSSUARY_DEVIATION = 0.1f;
    private static final float MARROW_MIN_DEPTH = 0.2f;
    private static final float MARROW_MAX_DEPTH = 0.9f;

    public static void register() {
        // Taking over whole stretches of common land biomes gives regions as big as vanilla ones.
        BiomePlacement.replaceOverworld(Biomes.PLAINS, ModBiomes.HOLLOW_VALE, VALE_SHARE);
        BiomePlacement.replaceOverworld(Biomes.FOREST, ModBiomes.HOLLOW_VALE, VALE_SHARE);
        BiomePlacement.replaceOverworld(Biomes.TAIGA, ModBiomes.HOLLOW_VALE, VALE_SHARE);
        Criterion cellCenter = CriterionBuilder.allOf(
                CriterionBuilder.deviation(BiomeParameterTargets.TEMPERATURE, -OSSUARY_DEVIATION, OSSUARY_DEVIATION),
                CriterionBuilder.deviation(BiomeParameterTargets.HUMIDITY, -OSSUARY_DEVIATION, OSSUARY_DEVIATION));
        BiomePlacement.addSubOverworld(ModBiomes.HOLLOW_VALE, ModBiomes.OSSUARY, cellCenter);
        Criterion underground = CriterionBuilder.value(BiomeParameterTargets.DEPTH, MARROW_MIN_DEPTH, MARROW_MAX_DEPTH);
        BiomePlacement.addSubOverworld(ModBiomes.HOLLOW_VALE, ModBiomes.MARROW_HOLLOWS, underground);
        BiomePlacement.addSubOverworld(ModBiomes.OSSUARY, ModBiomes.MARROW_HOLLOWS, underground);
    }
}
