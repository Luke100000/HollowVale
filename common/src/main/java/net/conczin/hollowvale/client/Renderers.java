package net.conczin.hollowvale.client;

import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.block.Blocks;
import net.conczin.hollowvale.entity.Entities;
import net.conczin.hollowvale.entity.Fry;
import net.conczin.hollowvale.entity.HoppingSkull;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

public final class Renderers {
    public interface Registrar {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
    }

    public interface BlockEntityRegistrar {
        <T extends BlockEntity, S extends BlockEntityRenderState> void register(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider);
    }

    public static void registerBlockEntities(BlockEntityRegistrar registrar) {
        registrar.register(Blocks.WIND_CHIME_ENTITY, WindChimeRenderer::new);
    }

    public static void register(Registrar registrar) {
        registrar.register(Entities.BONE_WHALE, BoneWhaleRenderer::new);
        registrar.register(Entities.FRY, ctx -> new GeoEntityRenderer<Fry, LivingEntityRenderState>(ctx, new DefaultedEntityGeoModel<>(Common.locate("bone_fry"))));
        registrar.register(Entities.HOPPING_SKULL, ctx -> new GeoEntityRenderer<HoppingSkull, LivingEntityRenderState>(ctx, new DefaultedEntityGeoModel<HoppingSkull>(Common.locate("hopping_skull"))
                .withAltTexture(Identifier.withDefaultNamespace("skeleton/skeleton"))));
        registrar.register(Entities.BONE_SLUDGE, ctx -> new SludgeRenderer<>(ctx, "bone_sludge"));
        registrar.register(Entities.SKULL_SLUDGE, ctx -> new SludgeRenderer<>(ctx, "skull_sludge"));
        registrar.register(Entities.ENDER_SKULL, EnderSkullRenderer::new);
        registrar.register(Entities.UNDEAD_HIGHLAND_CATTLE, UndeadHighlandCattleRenderer::new);
        registrar.register(Entities.SCAVENGER, ScavengerRenderer::new);
        registrar.register(Entities.HARPOON_BOLT, HarpoonBoltRenderer::new);
        registrar.register(Entities.SLUDGE_BALL, ThrownItemRenderer::new);
    }
}
