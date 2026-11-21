package net.conczin.hollowvale.world;

import com.terraformersmc.biolith.api.biome.BiomePlacement;
import com.terraformersmc.biolith.api.biome.sub.BiomeParameterTargets;
import com.terraformersmc.biolith.api.biome.sub.Criterion;
import com.terraformersmc.biolith.api.biome.sub.CriterionBuilder;
import com.terraformersmc.biolith.api.biome.sub.RatioTargets;
import net.minecraft.world.level.biome.Biomes;

public final class BiolithCompat {
    private static final double VALE_SHARE = 0.2;
    // How far from the center of a Vale region the Ossuary reaches, 0 is the center and 1 the border.
    private static final float OSSUARY_REACH = 0.6f;
    private static final float MARROW_MIN_DEPTH = 0.2f;
    private static final float MARROW_MAX_DEPTH = 0.9f;

    public static void register() {
        // Taking over whole stretches of common land biomes gives regions as big as vanilla ones.
        BiomePlacement.replaceOverworld(Biomes.PLAINS, ModBiomes.HOLLOW_VALE, VALE_SHARE);
        BiomePlacement.replaceOverworld(Biomes.FOREST, ModBiomes.HOLLOW_VALE, VALE_SHARE);
        BiomePlacement.replaceOverworld(Biomes.TAIGA, ModBiomes.HOLLOW_VALE, VALE_SHARE);
        BiomePlacement.addSubOverworld(ModBiomes.HOLLOW_VALE, ModBiomes.OSSUARY, CriterionBuilder.ratioMax(RatioTargets.CENTER, OSSUARY_REACH));
        Criterion underground = CriterionBuilder.value(BiomeParameterTargets.DEPTH, MARROW_MIN_DEPTH, MARROW_MAX_DEPTH);
        BiomePlacement.addSubOverworld(ModBiomes.HOLLOW_VALE, ModBiomes.MARROW_HOLLOWS, underground);
        BiomePlacement.addSubOverworld(ModBiomes.OSSUARY, ModBiomes.MARROW_HOLLOWS, underground);
    }
}
