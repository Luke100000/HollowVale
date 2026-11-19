package net.conczin.hollowvale.client;

import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.entity.BoneWhale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class BoneWhaleModel extends DefaultedEntityGeoModel<BoneWhale> {
    private static final float SPINE_BEND = 6.0f;
    private static final float MAX_BEND = 30.0f;
    private static final float CALF_BOB = 1.0f;
    private static final float YOUNG_BOB = 1.67f;
    private static final float ADULT_BOB = 1.825f;

    private final DefaultedEntityGeoModel<BoneWhale> calf = new DefaultedEntityGeoModel<>(Common.locate("bone_whale_calf"));
    private final DefaultedEntityGeoModel<BoneWhale> young = new DefaultedEntityGeoModel<>(Common.locate("bone_whale_young"));

    public BoneWhaleModel() {
        super(Common.locate("bone_whale"));
    }

    @Override
    public ResourceLocation getModelResource(BoneWhale whale) {
        DefaultedEntityGeoModel<BoneWhale> stage = stage(whale);
        return stage == null ? super.getModelResource(whale) : stage.getModelResource(whale);
    }

    @Override
    public ResourceLocation getTextureResource(BoneWhale whale) {
        DefaultedEntityGeoModel<BoneWhale> stage = stage(whale);
        return stage == null ? super.getTextureResource(whale) : stage.getTextureResource(whale);
    }

    @Override
    public ResourceLocation getAnimationResource(BoneWhale whale) {
        DefaultedEntityGeoModel<BoneWhale> stage = stage(whale);
        return stage == null ? super.getAnimationResource(whale) : stage.getAnimationResource(whale);
    }

    private @Nullable DefaultedEntityGeoModel<BoneWhale> stage(BoneWhale whale) {
        if (whale.isYoung()) {
            return young;
        }
        return whale.isBaby() ? calf : null;
    }

    @Override
    public void setCustomAnimations(BoneWhale whale, long instanceId, AnimationState<BoneWhale> state) {
        if (!whale.isDeadOrDying()) {
            swim(whale, state.getPartialTick());
        }

        GeoBone saddle = getAnimationProcessor().getBone("saddle");
        if (saddle != null) {
            saddle.setHidden(!whale.isSaddled());
            whale.setSeatOffset(animatedOffset(saddle));
        }
    }

    // A travelling sine wave from head to fluke, with phases in degrees and amplitudes in Blockbench units.
    private void swim(BoneWhale whale, float partialTick) {
        float phase = whale.getSwimPhase(partialTick);
        float bob = whale.isYoung() ? YOUNG_BOB : whale.isBaby() ? CALF_BOB : ADULT_BOB;
        GeoBone root = getAnimationProcessor().getBone("whale");
        if (root != null) {
            root.setPosY(root.getInitialSnapshot().getOffsetY() + bob * Mth.sin(phase));
        }

        // Head and tail lean the same amount in opposite directions so the body stays centered.
        float bend = Mth.clamp(whale.getTurnRate(partialTick) * SPINE_BEND, -MAX_BEND, MAX_BEND) * Mth.DEG_TO_RAD;
        pitch("spine_1", wave(phase, 2.0f, -180.0f), 0.0f);
        pitch("head", wave(phase, 2.0f, -145.6f), -bend);
        pitch("jaw", wave(phase, 4.0f, 157.1f) - 4.0f, 0.0f);
        pitch("spine_2", wave(phase, 5.0f, 134.2f), bend / 3.0f);
        pitch("tail", wave(phase, 10.0f, 88.3f), bend / 3.0f);
        pitch("fluke", wave(phase, 22.0f, 42.5f), bend / 3.0f);
        roll("fin_l", wave(phase, 15.0f, -28.6f));
        roll("fin_r", -wave(phase, 15.0f, -28.6f));
    }

    private static float wave(float phase, float amplitude, float offsetDegrees) {
        return amplitude * Mth.sin(phase + offsetDegrees * Mth.DEG_TO_RAD);
    }

    // Absolute values, since GeckoLib doesn't reset bones that were changed in code the frame before.
    private void pitch(String name, float degrees, float yaw) {
        GeoBone bone = getAnimationProcessor().getBone(name);
        if (bone != null) {
            bone.setRotX(bone.getInitialSnapshot().getRotX() - degrees * Mth.DEG_TO_RAD);
            bone.setRotY(bone.getInitialSnapshot().getRotY() + yaw);
        }
    }

    private void roll(String name, float degrees) {
        GeoBone bone = getAnimationProcessor().getBone(name);
        if (bone != null) {
            bone.setRotZ(bone.getInitialSnapshot().getRotZ() + degrees * Mth.DEG_TO_RAD);
        }
    }

    // How far the animation moved the bone's pivot, in model space blocks.
    private static Vec3 animatedOffset(GeoBone bone) {
        Vector3f pivot = new Vector3f(bone.getPivotX(), bone.getPivotY(), bone.getPivotZ()).div(16f);
        Vector3f moved = boneTransform(bone).transformPosition(pivot, new Vector3f());
        return new Vec3(moved.x - pivot.x, moved.y - pivot.y, moved.z - pivot.z);
    }

    // Mirrors GeckoLib's RenderUtil.prepMatrixForBone down the hierarchy.
    private static Matrix4f boneTransform(GeoBone bone) {
        Matrix4f matrix = bone.getParent() == null ? new Matrix4f() : boneTransform(bone.getParent());
        float x = bone.getPivotX() / 16f, y = bone.getPivotY() / 16f, z = bone.getPivotZ() / 16f;
        return matrix.translate(-bone.getPosX() / 16f, bone.getPosY() / 16f, bone.getPosZ() / 16f)
                .translate(x, y, z)
                .rotateZ(bone.getRotZ()).rotateY(bone.getRotY()).rotateX(bone.getRotX())
                .scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ())
                .translate(-x, -y, -z);
    }
}
