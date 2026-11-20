package net.conczin.hollowvale.fabric;

import net.conczin.hollowvale.world.TerraBlenderCompat;
import net.fabricmc.loader.api.FabricLoader;
import terrablender.api.TerraBlenderApi;

public final class TerraBlenderFabric implements TerraBlenderApi {
    @Override
    public void onTerraBlenderInitialized() {
        if (!FabricLoader.getInstance().isModLoaded("biolith")) {
            TerraBlenderCompat.register();
        }
    }
}
