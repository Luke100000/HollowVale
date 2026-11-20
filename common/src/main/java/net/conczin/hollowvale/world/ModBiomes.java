package net.conczin.hollowvale.world;

import net.conczin.hollowvale.Common;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

import java.util.ArrayList;
import java.util.List;

public final class ModBiomes {
    public static final ResourceKey<Biome> HOLLOW_VALE = key("hollow_vale");
    public static final ResourceKey<Biome> OSSUARY = key("ossuary");
    public static final ResourceKey<Biome> MARROW_HOLLOWS = key("marrow_hollows");

    public record Placement(ResourceKey<Biome> biome, Climate.ParameterPoint point) {
    }

    public static final List<Placement> PLACEMENTS = createPlacements();

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, Common.locate(name));
    }

    private static List<Placement> createPlacements() {
        Climate.Parameter temperature = Climate.Parameter.span(-0.7f, 0.2f);
        Climate.Parameter erosion = Climate.Parameter.span(-0.1f, 0.8f);
        Climate.Parameter weirdness = Climate.Parameter.span(-0.8f, 0.8f);
        Climate.Parameter inland = Climate.Parameter.span(-0.05f, 0.85f);
        Climate.Parameter dry = Climate.Parameter.span(-1.0f, -0.35f);
        Climate.Parameter damp = Climate.Parameter.span(-0.35f, 0.3f);
        Climate.Parameter caves = Climate.Parameter.span(0.2f, 0.9f);

        List<Placement> placements = new ArrayList<>();
        for (float depth : new float[]{0.0f, 1.0f}) {
            Climate.Parameter surface = Climate.Parameter.point(depth);
            placements.add(new Placement(HOLLOW_VALE, Climate.parameters(temperature, damp, inland, erosion, surface, weirdness, 0.0f)));
            placements.add(new Placement(OSSUARY, Climate.parameters(temperature, dry, inland, erosion, surface, weirdness, 0.0f)));
        }
        Climate.Parameter anyHumidity = Climate.Parameter.span(-1.0f, 0.3f);
        placements.add(new Placement(MARROW_HOLLOWS, Climate.parameters(temperature, anyHumidity, inland, erosion, caves, weirdness, 0.0f)));
        return List.copyOf(placements);
    }
}
