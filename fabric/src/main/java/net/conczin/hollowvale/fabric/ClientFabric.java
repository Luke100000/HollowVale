package net.conczin.hollowvale.fabric;

import net.conczin.hollowvale.client.Renderers;
import net.fabricmc.api.ClientModInitializer;
import net.conczin.hollowvale.block.Blocks;
import net.conczin.hollowvale.client.BlockTints;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class ClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Renderers.register(EntityRendererRegistry::register);
        Renderers.registerBlockEntities(BlockEntityRenderers::register);

        BlockColorRegistry.register(BlockTints.GRASS, Blocks.ENDER_INFUSED_GRASS);
    }
}
