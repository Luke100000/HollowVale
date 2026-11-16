package net.conczin.hollowvale.client;

import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.entity.Entities;
import net.conczin.hollowvale.entity.HoppingSkull;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class Renderers {
    public interface Registrar {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
    }

    public static void register(Registrar registrar) {
        registrar.register(Entities.BONE_WHALE, ctx -> new GeoEntityRenderer<>(ctx, new BoneWhaleModel()));
        registrar.register(Entities.FRY, ctx -> new GeoEntityRenderer<>(ctx, new DefaultedEntityGeoModel<>(Common.locate("bone_fry"))));
        // Placeholder until the hopping skull has its own model, renders the vanilla skeleton head.
        registrar.register(Entities.HOPPING_SKULL, ctx -> new GeoEntityRenderer<>(ctx, new DefaultedEntityGeoModel<HoppingSkull>(Common.locate("hopping_skull"))
                .withAltTexture(ResourceLocation.withDefaultNamespace("skeleton/skeleton"))));
        registrar.register(Entities.BONE_SLUDGE, ctx -> new GeoEntityRenderer<>(ctx, new TranslucentModel<>(Common.locate("bone_sludge"))));
        registrar.register(Entities.SKULL_SLUDGE, ctx -> new GeoEntityRenderer<>(ctx, new TranslucentModel<>(Common.locate("skull_sludge"))));
        registrar.register(Entities.ENDER_SKULL, ctx -> new GeoEntityRenderer<>(ctx, new DefaultedEntityGeoModel<>(Common.locate("enderskull"))));
    }

    private static class TranslucentModel<T extends GeoAnimatable> extends DefaultedEntityGeoModel<T> {
        TranslucentModel(ResourceLocation assetSubpath) {
            super(assetSubpath);
        }

        @Override
        public RenderType getRenderType(T animatable, ResourceLocation texture) {
            return RenderType.entityTranslucent(texture);
        }
    }
}
