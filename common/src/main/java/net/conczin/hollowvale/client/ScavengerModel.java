package net.conczin.hollowvale.client;

import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.entity.Scavenger;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class ScavengerModel extends DefaultedEntityGeoModel<Scavenger> {
    private static final float MAX_HEAD_YAW = 75.0f;
    private static final float SWING_SPEED = 0.6662f;
    private static final float SEATED_LEG_X = -1.4137167f;
    private static final float SEATED_LEG_SPLAY = Mth.PI / 10.0f;
    private static final float SEATED_LEG_ROLL = 0.07853982f;
    private static final float SEATED_ARM_X = -Mth.PI / 5.0f;

    public ScavengerModel() {
        super(Common.locate("scavenger"));
    }

    @Override
    public void setCustomAnimations(Scavenger scavenger, long instanceId, AnimationState<Scavenger> state) {
        GeoBone head = getAnimationProcessor().getBone("head");
        GeoBone rightArm = getAnimationProcessor().getBone("right_arm");
        GeoBone leftArm = getAnimationProcessor().getBone("left_arm");
        GeoBone rightLeg = getAnimationProcessor().getBone("right_leg");
        GeoBone leftLeg = getAnimationProcessor().getBone("left_leg");
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head == null || rightArm == null || leftArm == null || rightLeg == null || leftLeg == null || data == null) {
            return;
        }

        GeoBone keychain = getAnimationProcessor().getBone("sirben");
        if (keychain != null) {
            keychain.setHidden(!scavenger.hasKeychain());
            swingKeychain(keychain, scavenger.tickCount + state.getPartialTick(), state.getLimbSwingAmount());
        }

        boolean seated = scavenger.isPassenger();
        float swing = seated ? 0.0f : state.getLimbSwing();
        float amount = seated ? 0.0f : state.getLimbSwingAmount();

        // Vanilla's head angles, GeckoLib hands them over negated.
        float headYaw = -Mth.clamp(data.netHeadYaw(), -MAX_HEAD_YAW, MAX_HEAD_YAW) * Mth.DEG_TO_RAD;
        float headPitch = -data.headPitch() * Mth.DEG_TO_RAD;
        pose(head, headPitch, headYaw, 0.0f);

        float rightLegX = Mth.cos(swing * SWING_SPEED) * 1.4f * amount;
        float leftLegX = Mth.cos(swing * SWING_SPEED + Mth.PI) * 1.4f * amount;
        if (seated) {
            pose(rightLeg, SEATED_LEG_X, SEATED_LEG_SPLAY, SEATED_LEG_ROLL);
            pose(leftLeg, SEATED_LEG_X, -SEATED_LEG_SPLAY, -SEATED_LEG_ROLL);
        } else {
            pose(rightLeg, rightLegX, 0.0f, 0.0f);
            pose(leftLeg, leftLegX, 0.0f, 0.0f);
        }

        if (scavenger.isAggressive()) {
            float charge = Mth.clamp(scavenger.getCharge() / (float) Scavenger.CHARGE_TICKS, 0.0f, 1.0f);
            if (scavenger.getCharge() > 0) {
                poseCrossbowCharge(rightArm, leftArm, charge);
            } else {
                poseCrossbowHold(rightArm, leftArm, headPitch, headYaw);
            }
            return;
        }

        float rightArmX = Mth.cos(swing * SWING_SPEED + Mth.PI) * amount;
        float leftArmX = Mth.cos(swing * SWING_SPEED) * amount;
        if (seated) {
            rightArmX += SEATED_ARM_X;
            leftArmX += SEATED_ARM_X;
        }
        pose(rightArm, rightArmX, 0.0f, 0.0f);
        pose(leftArm, leftArmX, 0.0f, 0.0f);
    }

    private static void swingKeychain(GeoBone keychain, float age, float walkAmount) {
        float sway = 0.12f + 0.5f * walkAmount;
        keychain.setRotX(Mth.sin(age * 0.2f) * sway);
        keychain.setRotZ(Mth.sin(age * 0.17f + 1.0f) * sway);
    }

    private static void poseCrossbowHold(GeoBone rightArm, GeoBone leftArm, float headPitch, float headYaw) {
        pose(rightArm, -Mth.HALF_PI + headPitch + 0.1f, -0.3f + headYaw, 0.0f);
        pose(leftArm, -1.5f + headPitch, 0.6f + headYaw, 0.0f);
    }

    private static void poseCrossbowCharge(GeoBone rightArm, GeoBone leftArm, float charge) {
        pose(rightArm, -0.97079635f, -0.8f, 0.0f);
        pose(leftArm, Mth.lerp(charge, -0.97079635f, -Mth.HALF_PI), Mth.lerp(charge, 0.4f, 0.85f), 0.0f);
    }

    // Vanilla angles to GeckoLib's: X and Y rotate the other way.
    private static void pose(GeoBone bone, float vanillaX, float vanillaY, float z) {
        bone.setRotX(-vanillaX);
        bone.setRotY(-vanillaY);
        bone.setRotZ(z);
    }
}
