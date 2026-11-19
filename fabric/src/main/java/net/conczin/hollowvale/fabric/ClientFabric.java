package net.conczin.hollowvale.fabric;

import net.conczin.hollowvale.client.Renderers;
import net.fabricmc.api.ClientModInitializer;
import net.conczin.hollowvale.block.Blocks;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class ClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Renderers.register(EntityRendererRegistry::register);
        Renderers.registerBlockEntities(BlockEntityRenderers::register);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(), Blocks.MARROW_LAMP, Blocks.WIND_CHIME);
    }
}
