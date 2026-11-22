package net.conczin.hollowvale.client;

import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.block.Blocks;
import net.conczin.hollowvale.entity.EnderSkull;
import net.conczin.hollowvale.entity.Entities;
import net.conczin.hollowvale.entity.HoppingSkull;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class Renderers {
    public interface Registrar {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
    }

    public interface BlockEntityRegistrar {
        <T extends BlockEntity> void register(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T> provider);
    }

    public static void registerBlockEntities(BlockEntityRegistrar registrar) {
        registrar.register(Blocks.WIND_CHIME_ENTITY, WindChimeRenderer::new);
    }

    public static void register(Registrar registrar) {
        registrar.register(Entities.BONE_WHALE, BoneWhaleRenderer::new);
        registrar.register(Entities.FRY, ctx -> new GeoEntityRenderer<>(ctx, new DefaultedEntityGeoModel<>(Common.locate("bone_fry"))));
        registrar.register(Entities.HOPPING_SKULL, ctx -> new GeoEntityRenderer<>(ctx, new DefaultedEntityGeoModel<HoppingSkull>(Common.locate("hopping_skull"))
                .withAltTexture(ResourceLocation.withDefaultNamespace("skeleton/skeleton"))));
        registrar.register(Entities.BONE_SLUDGE, ctx -> new SludgeRenderer<>(ctx, "bone_sludge"));
        registrar.register(Entities.SKULL_SLUDGE, ctx -> new SludgeRenderer<>(ctx, "skull_sludge"));
        registrar.register(Entities.ENDER_SKULL, ctx -> {
            GeoEntityRenderer<EnderSkull> renderer = new GeoEntityRenderer<>(ctx, new EnderSkullModel());
            renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
            return renderer;
        });
        registrar.register(Entities.UNDEAD_HIGHLAND_CATTLE, UndeadHighlandCattleRenderer::new);
        registrar.register(Entities.SLUDGE_BALL, ThrownItemRenderer::new);
    }

    static class TranslucentModel<T extends GeoAnimatable> extends DefaultedEntityGeoModel<T> {
        TranslucentModel(ResourceLocation assetSubpath) {
            super(assetSubpath);
        }

        @Override
        public RenderType getRenderType(T animatable, ResourceLocation texture) {
            return RenderType.entityTranslucent(texture);
        }
    }
}
