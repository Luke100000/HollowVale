package net.conczin.hollowvale.neoforge;

import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.block.Blocks;
import net.conczin.hollowvale.client.BlockTints;
import net.conczin.hollowvale.client.Renderers;
import net.conczin.hollowvale.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@Mod(value = Common.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Common.MOD_ID, value = Dist.CLIENT)
public final class ClientNeoForge {
    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(BlockTints::grass, Blocks.ENDER_INFUSED_GRASS);
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(BlockTints::grassItem, Items.ENDER_INFUSED_GRASS);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        Renderers.register(event::registerEntityRenderer);
        Renderers.registerBlockEntities(event::registerBlockEntityRenderer);
    }
}
