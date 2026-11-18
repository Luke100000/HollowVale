package net.conczin.hollowvale.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.entity.BoneSludge;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Procedural sludge motion on top of the baked animations: a volume-preserving breathing wobble that
 * speeds up and deepens while moving, and a slow yaw sway so the sludge never stares rigidly.
 * The bone sludge additionally flattens while winding up a pounce.
 */
public class SludgeRenderer<T extends Mob & GeoAnimatable> extends GeoEntityRenderer<T> {
    private static final float BREATH_RATE = 0.08f;
    private static final float STRIDE_RATE = 0.9f;
    private static final float BREATH_DEPTH = 0.04f;
    private static final float SWAY_DEGREES = 6.0f;

    public SludgeRenderer(EntityRendererProvider.Context context, String name) {
        super(context, new Renderers.TranslucentModel<>(Common.locate(name)));
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, T animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        float age = animatable.tickCount + partialTick;
        float phase = age * BREATH_RATE + animatable.walkAnimation.position(partialTick) * STRIDE_RATE + animatable.getId();
        float depth = BREATH_DEPTH * (1.0f + animatable.walkAnimation.speed(partialTick));
        float breath = depth * Mth.sin(phase);

        float squish = animatable instanceof BoneSludge sludge ? sludge.getSquish(partialTick) : 0.0f;
        float width = widthScale * (1.0f + breath) * (1.0f + 0.4f * squish);
        float height = heightScale * (1.0f - breath) * (1.0f - 0.45f * squish);
        super.scaleModelForRender(width, height, poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        // Two incommensurate sines, so the sway does not visibly repeat.
        float offset = animatable.getId() * 1.7f;
        float sway = SWAY_DEGREES * (Mth.sin(ageInTicks * 0.031f + offset) * 0.7f + Mth.sin(ageInTicks * 0.074f + offset * 2.0f) * 0.3f);
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw + sway, partialTick, nativeScale);
    }
}
