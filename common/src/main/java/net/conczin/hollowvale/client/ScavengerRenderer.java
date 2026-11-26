package net.conczin.hollowvale.client;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.entity.Scavenger;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ScavengerRenderer extends GeoEntityRenderer<Scavenger, LivingEntityRenderState> {
    private static final String HAND = "hand_item";
    private static final float MAX_HEAD_YAW = 75.0f;
    private static final float SWING_SPEED = 0.6662f;
    private static final float SEATED_LEG_X = -1.4137167f;
    private static final float SEATED_LEG_SPLAY = Mth.PI / 10.0f;
    private static final float SEATED_LEG_ROLL = 0.07853982f;
    private static final float SEATED_ARM_X = -Mth.PI / 5.0f;

    private static final DataTicket<Boolean> SEATED = DataTicket.create("hollowvale_seated", Boolean.class);
    private static final DataTicket<Boolean> AGGRESSIVE = DataTicket.create("hollowvale_aggressive", Boolean.class);
    private static final DataTicket<Boolean> CHARGING = DataTicket.create("hollowvale_charging", Boolean.class);
    private static final DataTicket<Boolean> KEYCHAIN = DataTicket.create("hollowvale_keychain", Boolean.class);
    private static final DataTicket<Float> CHARGE = DataTicket.create("hollowvale_charge", Float.class);

    public ScavengerRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(Common.locate("scavenger")));

        shadowRadius = 0.5f;
        withRenderLayer(renderer -> new HandLayer(context, renderer));
    }

    @Override
    public void addRenderData(Scavenger scavenger, Void unused, LivingEntityRenderState state, float partialTick) {
        state.addGeckolibData(SEATED, scavenger.isPassenger());
        state.addGeckolibData(AGGRESSIVE, scavenger.isAggressive());
        state.addGeckolibData(CHARGING, scavenger.getCharge() > 0);
        state.addGeckolibData(KEYCHAIN, scavenger.hasKeychain());
        state.addGeckolibData(CHARGE, Mth.clamp(scavenger.getCharge() / (float) Scavenger.CHARGE_TICKS, 0.0f, 1.0f));
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
        LivingEntityRenderState state = info.renderState();
        boolean seated = info.getOrDefaultGeckolibData(SEATED, false);
        float swing = seated ? 0.0f : state.walkAnimationPos;
        float amount = seated ? 0.0f : Math.min(state.walkAnimationSpeed, 1.0f);

        bones.ifPresent("sirben", keychain -> {
            keychain.skipRender(!info.getOrDefaultGeckolibData(KEYCHAIN, false));
            float sway = 0.12f + 0.5f * amount;
            keychain.setRotX(Mth.sin(state.ageInTicks * 0.2f) * sway);
            keychain.setRotZ(Mth.sin(state.ageInTicks * 0.17f + 1.0f) * sway);
        });

        float headYaw = Mth.clamp(state.yRot, -MAX_HEAD_YAW, MAX_HEAD_YAW) * Mth.DEG_TO_RAD;
        float headPitch = state.xRot * Mth.DEG_TO_RAD;
        bones.ifPresent("head", head -> pose(head, headPitch, headYaw, 0.0f));

        float rightLegX = Mth.cos(swing * SWING_SPEED) * 1.4f * amount;
        float leftLegX = Mth.cos(swing * SWING_SPEED + Mth.PI) * 1.4f * amount;
        bones.ifPresent("right_leg", leg -> {
            if (seated) {
                pose(leg, SEATED_LEG_X, SEATED_LEG_SPLAY, SEATED_LEG_ROLL);
            } else {
                pose(leg, rightLegX, 0.0f, 0.0f);
            }
        });
        bones.ifPresent("left_leg", leg -> {
            if (seated) {
                pose(leg, SEATED_LEG_X, -SEATED_LEG_SPLAY, -SEATED_LEG_ROLL);
            } else {
                pose(leg, leftLegX, 0.0f, 0.0f);
            }
        });

        if (info.getOrDefaultGeckolibData(AGGRESSIVE, false)) {
            if (info.getOrDefaultGeckolibData(CHARGING, false)) {
                float charge = info.getOrDefaultGeckolibData(CHARGE, 0.0f);
                bones.ifPresent("right_arm", arm -> pose(arm, -0.97079635f, -0.8f, 0.0f));
                bones.ifPresent("left_arm", arm -> pose(arm, Mth.lerp(charge, -0.97079635f, -Mth.HALF_PI), Mth.lerp(charge, 0.4f, 0.85f), 0.0f));
            } else {
                bones.ifPresent("right_arm", arm -> pose(arm, -Mth.HALF_PI + headPitch + 0.1f, -0.3f + headYaw, 0.0f));
                bones.ifPresent("left_arm", arm -> pose(arm, -1.5f + headPitch, 0.6f + headYaw, 0.0f));
            }
            return;
        }

        float rightArmX = Mth.cos(swing * SWING_SPEED + Mth.PI) * amount + (seated ? SEATED_ARM_X : 0.0f);
        float leftArmX = Mth.cos(swing * SWING_SPEED) * amount + (seated ? SEATED_ARM_X : 0.0f);
        bones.ifPresent("right_arm", arm -> pose(arm, rightArmX, 0.0f, 0.0f));
        bones.ifPresent("left_arm", arm -> pose(arm, leftArmX, 0.0f, 0.0f));
    }

    // Vanilla angles to GeckoLib's: X and Y rotate the other way.
    private static void pose(BoneSnapshot bone, float vanillaX, float vanillaY, float z) {
        bone.setRotX(-vanillaX);
        bone.setRotY(-vanillaY);
        bone.setRotZ(z);
    }

    private static class HandLayer extends BlockAndItemGeoLayer<Scavenger, Void, LivingEntityRenderState> {
        HandLayer(EntityRendererProvider.Context context, GeoRenderer<Scavenger, Void, LivingEntityRenderState> renderer) {
            super(context, renderer);
        }

        @Override
        protected List<RenderData> getRelevantBones(Scavenger scavenger, Void unused, LivingEntityRenderState state, float partialTick) {
            ItemStack stack = scavenger.getMainHandItem();
            if (stack.isEmpty()) {
                return List.of();
            }
            ItemStackRenderState item = RenderUtil.createRenderStateForItem(stack, itemModelResolver, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, scavenger);
            return List.of(RenderData.item(HAND, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, item));
        }

        @Override
        public void addRenderData(Scavenger scavenger, Void unused, LivingEntityRenderState state, float partialTick) {
            List<RenderData> contents = getRelevantBones(scavenger, unused, state, partialTick);
            if (!contents.isEmpty()) {
                state.addGeckolibData(CONTENTS, contents);
            }
        }

        @Override
        protected void submitItemStackRender(PoseStack pose, GeoBone bone, ItemStackRenderState item, ItemDisplayContext context, LivingEntityRenderState state, SubmitNodeCollector collector, int light) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(90.0f));
            pose.mulPose(Axis.YP.rotationDegrees(180.0f));
            pose.translate(0.0, -0.125, 0.0);
            super.submitItemStackRender(pose, bone, item, context, state, collector, light);
            pose.popPose();
        }
    }
}
