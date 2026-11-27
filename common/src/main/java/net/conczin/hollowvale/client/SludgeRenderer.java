package net.conczin.hollowvale.client;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.entity.BoneSludge;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * Breathing wobble, faster while moving, a yaw sway and the bone sludge pounce squish.
 */
public class SludgeRenderer<T extends Mob & GeoAnimatable> extends GeoEntityRenderer<T, LivingEntityRenderState> {
    private static final float BREATH_RATE = 0.08f;
    private static final float STRIDE_RATE = 0.9f;
    private static final float BREATH_DEPTH = 0.04f;
    private static final float SWAY_DEGREES = 6.0f;

    private static final DataTicket<Float> BREATH = DataTicket.create("hollowvale_breath", Float.class);
    private static final DataTicket<Float> SQUISH = DataTicket.create("hollowvale_squish", Float.class);
    private static final DataTicket<Float> SWAY = DataTicket.create("hollowvale_sway", Float.class);

    public SludgeRenderer(EntityRendererProvider.Context context, String name) {
        super(context, new DefaultedEntityGeoModel<>(Common.locate(name)));
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState state, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    @Override
    public void addRenderData(T animatable, Void unused, LivingEntityRenderState state, float partialTick) {
        float age = animatable.tickCount + partialTick;
        float phase = age * BREATH_RATE + animatable.walkAnimation.position(partialTick) * STRIDE_RATE + animatable.getId();
        float depth = BREATH_DEPTH * (1.0f + animatable.walkAnimation.speed(partialTick));
        state.addGeckolibData(BREATH, depth * Mth.sin(phase));
        state.addGeckolibData(SQUISH, animatable instanceof BoneSludge sludge ? sludge.getSquish(partialTick) : 0.0f);

        float offset = animatable.getId() * 1.7f;
        state.addGeckolibData(SWAY, SWAY_DEGREES * (Mth.sin(age * 0.031f + offset) * 0.7f + Mth.sin(age * 0.074f + offset * 2.0f) * 0.3f));
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
        float breath = info.getOrDefaultGeckolibData(BREATH, 0.0f);
        float squish = info.getOrDefaultGeckolibData(SQUISH, 0.0f);
        super.scaleModelForRender(info, widthScale * (1.0f + breath) * (1.0f + 0.4f * squish), heightScale * (1.0f - breath) * (1.0f - 0.45f * squish));
    }

    @Override
    protected void applyRotations(RenderPassInfo<LivingEntityRenderState> info, PoseStack poseStack, float nativeScale) {
        poseStack.rotate(Axis.YP.rotationDegrees(-info.getOrDefaultGeckolibData(SWAY, 0.0f)));
        super.applyRotations(info, poseStack, nativeScale);
    }
}
