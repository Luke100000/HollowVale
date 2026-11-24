package net.conczin.hollowvale.world;

import com.mojang.datafixers.util.Pair;
import net.conczin.hollowvale.Common;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.ModifiedVanillaOverworldBuilder;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.Regions;

import java.util.List;
import java.util.function.Consumer;

public final class TerraBlenderCompat {
    private static final int REGION_WEIGHT = 2;

    private static final Climate.Parameter UNDERGROUND = Climate.Parameter.span(0.2f, 0.9f);

    public static void register() {
        Regions.register(new Region(Common.locate("overworld"), RegionType.OVERWORLD, REGION_WEIGHT) {
            @Override
            public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
                ModifiedVanillaOverworldBuilder builder = new ModifiedVanillaOverworldBuilder();
                for (ResourceKey<Biome> vanilla : List.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA)) {
                    builder.replaceBiome(vanilla, ModBiomes.HOLLOW_VALE);
                }
                for (ResourceKey<Biome> vanilla : List.of(Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.FLOWER_FOREST)) {
                    builder.replaceBiome(vanilla, ModBiomes.OSSUARY);
                }
                builder.build().forEach(mapper);

                // Underground of the climate those biomes cover.
                mapper.accept(Pair.of(Climate.parameters(
                        Climate.Parameter.span(-0.45f, 0.2f), Climate.Parameter.span(-0.35f, 0.3f), Climate.Parameter.span(-0.05f, 1.0f),
                        Climate.Parameter.span(-0.1f, 1.0f), UNDERGROUND, Climate.Parameter.span(-1.0f, 1.0f), 0.0f), ModBiomes.MARROW_HOLLOWS));
            }
        });
    }

    private TerraBlenderCompat() {
    }
}
