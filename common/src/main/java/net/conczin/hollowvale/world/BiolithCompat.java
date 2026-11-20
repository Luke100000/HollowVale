package net.conczin.hollowvale.world;

import com.terraformersmc.biolith.api.biome.BiomePlacement;
import com.terraformersmc.biolith.api.biome.sub.CriterionBuilder;
import com.terraformersmc.biolith.api.biome.sub.RatioTargets;
import net.minecraft.world.level.biome.Biomes;

public final class BiolithCompat {
    private static final double VALE_SHARE = 0.7;
    // How far from the center of a Vale region the Ossuary reaches, 0 is the center and 1 the border.
    private static final float OSSUARY_REACH = 0.6f;

    public static void register() {
        // Taking over whole stretches of common land biomes gives regions as big as vanilla ones.
        BiomePlacement.replaceOverworld(Biomes.PLAINS, ModBiomes.HOLLOW_VALE, VALE_SHARE);
        BiomePlacement.replaceOverworld(Biomes.FOREST, ModBiomes.HOLLOW_VALE, VALE_SHARE);
        BiomePlacement.replaceOverworld(Biomes.TAIGA, ModBiomes.HOLLOW_VALE, VALE_SHARE);
        BiomePlacement.addSubOverworld(ModBiomes.HOLLOW_VALE, ModBiomes.OSSUARY, CriterionBuilder.ratioMax(RatioTargets.CENTER, OSSUARY_REACH));
        ModBiomes.PLACEMENTS.stream()
                .filter(placement -> placement.biome() == ModBiomes.MARROW_HOLLOWS)
                .forEach(placement -> BiomePlacement.addOverworld(placement.biome(), placement.point()));
    }
}
