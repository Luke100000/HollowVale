package net.conczin.hollowvale.client;

import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.entity.EnderSkull;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * All ender skull motion is procedural since it depends on its mode: a hovering bob, loosely connected jaws and eye
 * that wobble independently, the jaw opening, the searching eye and a head shake on ambient sounds.
 */
public class EnderSkullModel extends DefaultedEntityGeoModel<EnderSkull> {
    // The jaw cubes are modelled open (upper 15°, lower -25° around X). The angles below are negated when applied. Closed leaves each jaw ajar by 5° so the eye shows,
    // and fully open goes past the Blockbench pose to upper -30°, lower +45°.
    private static final float UPPER_CLOSED = 10.0f * Mth.DEG_TO_RAD;
    private static final float UPPER_OPEN = -15.0f * Mth.DEG_TO_RAD;
    private static final float LOWER_CLOSED = -20.0f * Mth.DEG_TO_RAD;
    private static final float LOWER_OPEN = 20.0f * Mth.DEG_TO_RAD;
    private static final float LOOSE = 3.0f * Mth.DEG_TO_RAD;
    private static final float SHAKE_YAW = 20.0f * Mth.DEG_TO_RAD;
    private static final float SHAKE_ROLL = 8.0f * Mth.DEG_TO_RAD;

    public EnderSkullModel() {
        super(Common.locate("enderskull"));
    }

    @Override
    public void setCustomAnimations(EnderSkull skull, long instanceId, AnimationState<EnderSkull> state) {
        GeoBone root = getAnimationProcessor().getBone("root");
        GeoBone upper = getAnimationProcessor().getBone("upper");
        GeoBone lower = getAnimationProcessor().getBone("lower");
        GeoBone eye = getAnimationProcessor().getBone("eye");
        if (root == null || upper == null || lower == null || eye == null) {
            return;
        }

        float partialTick = state.getPartialTick();
        float age = skull.tickCount + partialTick;
        float seed = skull.getId() * 1.3f;

        float shake = skull.getShake(partialTick);
        root.setRotY(shake * SHAKE_YAW * Mth.sin(age * 1.8f));
        root.setRotZ(shake * SHAKE_ROLL * Mth.sin(age * 2.3f));
        root.setPosY(skull.getBob(partialTick));

        float jaw = skull.getJaw(partialTick);
        upper.setRotX(-Mth.lerp(jaw, UPPER_CLOSED, UPPER_OPEN) + wobble(age, seed, 0.13f));
        upper.setRotZ(wobble(age, seed + 1.0f, 0.11f));
        lower.setRotX(-Mth.lerp(jaw, LOWER_CLOSED, LOWER_OPEN) + wobble(age, seed + 2.0f, 0.15f));
        lower.setRotZ(wobble(age, seed + 3.0f, 0.12f));

        // Rotate the eye around its own center rather than the exported pivot.
        eye.updatePivot(0.0f, 4.0f, 0.0f);
        eye.setRotY(skull.getEyeYaw(partialTick) * Mth.DEG_TO_RAD);
        eye.setRotX(-skull.getEyePitch(partialTick) * Mth.DEG_TO_RAD);
        eye.setPosZ(Mth.sin(age * 0.17f + seed) * 0.3f);
        eye.setPosY(Mth.sin(age * 0.19f + seed + 4.0f) * 0.3f);
    }

    private static float wobble(float age, float phase, float rate) {
        return LOOSE * Mth.sin(age * rate + phase);
    }
}
