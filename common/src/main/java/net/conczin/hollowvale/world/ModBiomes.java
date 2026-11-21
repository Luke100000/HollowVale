package net.conczin.hollowvale.world;

import net.conczin.hollowvale.Common;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public final class ModBiomes {
    public static final ResourceKey<Biome> HOLLOW_VALE = key("hollow_vale");
    public static final ResourceKey<Biome> OSSUARY = key("ossuary");
    public static final ResourceKey<Biome> MARROW_HOLLOWS = key("marrow_hollows");

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, Common.locate(name));
    }
}
