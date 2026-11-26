package net.conczin.hollowvale.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.entity.EnderSkull;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * All ender skull motion is procedural since it depends on its mode: a hovering bob, loosely connected jaws and eye
 * that wobble independently, the jaw opening, the searching eye and a head shake on ambient sounds.
 */
public class EnderSkullRenderer extends GeoEntityRenderer<EnderSkull, LivingEntityRenderState> {
    // The jaw cubes are modelled open (upper 15°, lower -25° around X). The angles below are negated when applied. Closed leaves each jaw ajar by 5° so the eye shows,
    // and fully open goes past the Blockbench pose to upper -30°, lower +45°.
    private static final float UPPER_CLOSED = 10.0f * Mth.DEG_TO_RAD;
    private static final float UPPER_OPEN = -15.0f * Mth.DEG_TO_RAD;
    private static final float LOWER_CLOSED = -20.0f * Mth.DEG_TO_RAD;
    private static final float LOWER_OPEN = 20.0f * Mth.DEG_TO_RAD;
    private static final float LOOSE = 3.0f * Mth.DEG_TO_RAD;
    private static final float SHAKE_YAW = 20.0f * Mth.DEG_TO_RAD;
    private static final float SHAKE_ROLL = 8.0f * Mth.DEG_TO_RAD;
    // The eye turns around its own center rather than the exported pivot.
    private static final Vector3f EYE_PIVOT = new Vector3f(0.0f, 4.0f, 0.0f);

    private static final DataTicket<Float> SHAKE = DataTicket.create("hollowvale_shake", Float.class);
    private static final DataTicket<Float> BOB = DataTicket.create("hollowvale_bob", Float.class);
    private static final DataTicket<Float> JAW = DataTicket.create("hollowvale_jaw", Float.class);
    private static final DataTicket<Float> EYE_YAW = DataTicket.create("hollowvale_eye_yaw", Float.class);
    private static final DataTicket<Float> EYE_PITCH = DataTicket.create("hollowvale_eye_pitch", Float.class);
    private static final DataTicket<Integer> SEED = DataTicket.create("hollowvale_seed", Integer.class);

    public EnderSkullRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(Common.locate("enderskull")));

        withRenderLayer(AutoGlowingGeoLayer::new);
    }

    @Override
    public void addRenderData(EnderSkull skull, Void unused, LivingEntityRenderState state, float partialTick) {
        state.addGeckolibData(SHAKE, skull.getShake(partialTick));
        state.addGeckolibData(BOB, skull.getBob(partialTick));
        state.addGeckolibData(JAW, skull.getJaw(partialTick));
        state.addGeckolibData(EYE_YAW, skull.getEyeYaw(partialTick));
        state.addGeckolibData(EYE_PITCH, skull.getEyePitch(partialTick));
        state.addGeckolibData(SEED, skull.getId());
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
        float age = info.renderState().ageInTicks;
        float seed = info.getOrDefaultGeckolibData(SEED, 0) * 1.3f;

        float shake = info.getOrDefaultGeckolibData(SHAKE, 0.0f);
        bones.ifPresent("root", root -> {
            root.setRotY(shake * SHAKE_YAW * Mth.sin(age * 1.8f));
            root.setRotZ(shake * SHAKE_ROLL * Mth.sin(age * 2.3f));
            root.setTranslateY(info.getOrDefaultGeckolibData(BOB, 0.0f));
        });

        float jaw = info.getOrDefaultGeckolibData(JAW, 0.0f);
        bones.ifPresent("upper", upper -> {
            upper.setRotX(-Mth.lerp(jaw, UPPER_CLOSED, UPPER_OPEN) + wobble(age, seed, 0.13f));
            upper.setRotZ(wobble(age, seed + 1.0f, 0.11f));
        });
        bones.ifPresent("lower", lower -> {
            lower.setRotX(-Mth.lerp(jaw, LOWER_CLOSED, LOWER_OPEN) + wobble(age, seed + 2.0f, 0.15f));
            lower.setRotZ(wobble(age, seed + 3.0f, 0.12f));
        });

        float eyeYaw = info.getOrDefaultGeckolibData(EYE_YAW, 0.0f) * Mth.DEG_TO_RAD;
        float eyePitch = -info.getOrDefaultGeckolibData(EYE_PITCH, 0.0f) * Mth.DEG_TO_RAD;
        bones.ifPresent("eye", eye -> {
            eye.setRotY(eyeYaw);
            eye.setRotX(eyePitch);

            // A bone's pivot is fixed, so turning around another point takes a matching shift of the bone.
            Vector3f shift = new Vector3f(EYE_PIVOT).sub(eye.getBone().pivotX(), eye.getBone().pivotY(), eye.getBone().pivotZ()).div(16.0f);
            Vector3f turned = new Matrix3f().rotationZ(eye.getRotZ()).rotateY(eyeYaw).rotateX(eyePitch).transform(new Vector3f(shift));
            shift.sub(turned);
            eye.setTranslation(-shift.x * 16.0f, shift.y * 16.0f + Mth.sin(age * 0.19f + seed + 4.0f) * 0.3f, shift.z * 16.0f + Mth.sin(age * 0.17f + seed) * 0.3f);
        });
    }

    private static float wobble(float age, float phase, float rate) {
        return LOOSE * Mth.sin(age * rate + phase);
    }
}
