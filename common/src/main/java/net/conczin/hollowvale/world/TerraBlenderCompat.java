package net.conczin.hollowvale.world;

import com.mojang.datafixers.util.Pair;
import net.conczin.hollowvale.Common;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.Regions;
import terrablender.api.VanillaParameterOverlayBuilder;

import java.util.function.Consumer;

public final class TerraBlenderCompat {
    private static final int REGION_WEIGHT = 10;

    public static void register() {
        Regions.register(new Region(Common.locate("overworld"), RegionType.OVERWORLD, REGION_WEIGHT) {
            @Override
            public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
                VanillaParameterOverlayBuilder overlay = new VanillaParameterOverlayBuilder();
                for (ModBiomes.Placement placement : ModBiomes.PLACEMENTS) {
                    overlay.add(placement.point(), placement.biome());
                }
                overlay.build().forEach(mapper);
            }
        });
    }

    private TerraBlenderCompat() {
    }
}
