package net.conczin.hollowvale.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.GeoBone;
import net.conczin.hollowvale.entity.BoneWhale;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class BoneWhaleRenderer extends GeoEntityRenderer<BoneWhale, LivingEntityRenderState> {
    static final DataTicket<Integer> STAGE = DataTicket.create("hollowvale_stage", Integer.class);
    private static final DataTicket<Float> SWIM_PHASE = DataTicket.create("hollowvale_swim_phase", Float.class);
    private static final DataTicket<Float> TURN_RATE = DataTicket.create("hollowvale_turn_rate", Float.class);
    private static final DataTicket<Boolean> DYING = DataTicket.create("hollowvale_dying", Boolean.class);
    private static final DataTicket<BoneWhale> WHALE = DataTicket.create("hollowvale_whale", BoneWhale.class);
    private static final DataTicket<Boolean> SADDLED = DataTicket.create("hollowvale_saddled", Boolean.class);

    private static final float SPINE_BEND = 6.0f;
    private static final float MAX_BEND = 30.0f;
    private static final float CALF_BOB = 1.0f;
    private static final float YOUNG_BOB = 1.67f;
    private static final float ADULT_BOB = 1.825f;

    public BoneWhaleRenderer(EntityRendererProvider.Context context) {
        super(context, new BoneWhaleModel());
    }

    @Override
    protected float getDeathMaxRotation(GeoRenderState state) {
        return 0.0f;
    }

    @Override
    public void addRenderData(BoneWhale whale, Void unused, LivingEntityRenderState state, float partialTick) {
        state.addGeckolibData(STAGE, whale.isYoung() ? BoneWhaleModel.YOUNG : whale.isBaby() ? BoneWhaleModel.CALF : BoneWhaleModel.ADULT);
        state.addGeckolibData(SWIM_PHASE, whale.getSwimPhase(partialTick));
        state.addGeckolibData(TURN_RATE, whale.getTurnRate(partialTick));
        state.addGeckolibData(DYING, whale.isDeadOrDying());
        state.addGeckolibData(SADDLED, whale.isSaddled());
        state.addGeckolibData(WHALE, whale);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
        if (!info.getOrDefaultGeckolibData(DYING, false)) {
            swim(info, bones);
        }

        bones.ifPresent("saddle", saddle -> {
            saddle.skipRender(!info.getOrDefaultGeckolibData(SADDLED, false));
            BoneWhale whale = info.getGeckolibData(WHALE);
            if (whale != null) {
                whale.setSeatOffset(animatedOffset(saddle.getBone(), bones));
            }
        });
    }

    // A travelling sine wave from head to fluke, with phases in degrees and amplitudes in Blockbench units.
    private static void swim(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
        float phase = info.getOrDefaultGeckolibData(SWIM_PHASE, 0.0f);
        int stage = info.getOrDefaultGeckolibData(STAGE, BoneWhaleModel.ADULT);
        float bob = stage == BoneWhaleModel.YOUNG ? YOUNG_BOB : stage == BoneWhaleModel.CALF ? CALF_BOB : ADULT_BOB;
        bones.ifPresent("whale", root -> root.setTranslateY(bob * Mth.sin(phase)));

        // Head and tail lean the same amount in opposite directions so the body stays centered.
        float bend = Mth.clamp(info.getOrDefaultGeckolibData(TURN_RATE, 0.0f) * SPINE_BEND, -MAX_BEND, MAX_BEND) * Mth.DEG_TO_RAD;
        pitch(bones, "spine_1", wave(phase, 2.0f, -180.0f), 0.0f);
        pitch(bones, "head", wave(phase, 2.0f, -145.6f), -bend);
        pitch(bones, "jaw", wave(phase, 4.0f, 157.1f) - 4.0f, 0.0f);
        pitch(bones, "spine_2", wave(phase, 5.0f, 134.2f), bend / 3.0f);
        pitch(bones, "tail", wave(phase, 10.0f, 88.3f), bend / 3.0f);
        pitch(bones, "fluke", wave(phase, 22.0f, 42.5f), bend / 3.0f);
        roll(bones, "fin_l", wave(phase, 15.0f, -28.6f));
        roll(bones, "fin_r", -wave(phase, 15.0f, -28.6f));
    }

    private static float wave(float phase, float amplitude, float offsetDegrees) {
        return amplitude * Mth.sin(phase + offsetDegrees * Mth.DEG_TO_RAD);
    }

    private static void pitch(BoneSnapshots bones, String name, float degrees, float yaw) {
        bones.ifPresent(name, bone -> {
            bone.setRotX(-degrees * Mth.DEG_TO_RAD);
            bone.setRotY(yaw);
        });
    }

    private static void roll(BoneSnapshots bones, String name, float degrees) {
        bones.ifPresent(name, bone -> bone.setRotZ(degrees * Mth.DEG_TO_RAD));
    }

    // How far the animation moved the bone's pivot, in model space blocks.
    private static Vec3 animatedOffset(GeoBone bone, BoneSnapshots bones) {
        Vector3f pivot = new Vector3f(bone.pivotX(), bone.pivotY(), bone.pivotZ()).div(16f);
        Vector3f moved = boneTransform(bone, bones).transformPosition(pivot, new Vector3f());
        return new Vec3(moved.x - pivot.x, moved.y - pivot.y, moved.z - pivot.z);
    }

    // Mirrors GeckoLib's RenderUtil.prepMatrixForBone down the hierarchy.
    private static Matrix4f boneTransform(GeoBone bone, BoneSnapshots bones) {
        Matrix4f matrix = bone.parent() == null ? new Matrix4f() : boneTransform(bone.parent(), bones);
        BoneSnapshot snapshot = bones.get(bone);
        float x = bone.pivotX() / 16f, y = bone.pivotY() / 16f, z = bone.pivotZ() / 16f;
        return matrix.translate(-snapshot.getTranslateX() / 16f, snapshot.getTranslateY() / 16f, snapshot.getTranslateZ() / 16f)
                .translate(x, y, z)
                .rotateZ(bone.baseRotZ() + snapshot.getRotZ()).rotateY(bone.baseRotY() + snapshot.getRotY()).rotateX(bone.baseRotX() + snapshot.getRotX())
                .scale(snapshot.getScaleX(), snapshot.getScaleY(), snapshot.getScaleZ())
                .translate(-x, -y, -z);
    }
}
