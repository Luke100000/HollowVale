package net.conczin.hollowvale.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.conczin.hollowvale.entity.Scavenger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class ScavengerRenderer extends GeoEntityRenderer<Scavenger> {
    private static final String RIGHT_ARM = "right_arm";

    public ScavengerRenderer(EntityRendererProvider.Context context) {
        super(context, new ScavengerModel());

        shadowRadius = 0.5f;
        addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Override
            protected ItemStack getStackForBone(GeoBone bone, Scavenger scavenger) {
                return RIGHT_ARM.equals(bone.getName()) ? scavenger.getMainHandItem() : null;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, Scavenger scavenger) {
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }

            @Override
            protected void renderStackForBone(PoseStack pose, GeoBone bone, ItemStack stack, Scavenger scavenger, MultiBufferSource buffer, float partialTick, int light, int overlay) {
                pose.pushPose();
                pose.mulPose(Axis.XP.rotationDegrees(90.0f));
                pose.mulPose(Axis.YP.rotationDegrees(180.0f));
                pose.translate(-1.0 / 16.0, -0.125, -0.625);
                Minecraft.getInstance().getItemRenderer().renderStatic(scavenger, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, pose, buffer, scavenger.level(), light, overlay, scavenger.getId());
                pose.popPose();
            }
        });
    }
}
