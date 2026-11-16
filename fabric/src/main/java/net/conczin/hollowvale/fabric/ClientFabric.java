package net.conczin.hollowvale.fabric;

import net.conczin.hollowvale.client.Renderers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class ClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Renderers.register(EntityRendererRegistry::register);
    }
}
