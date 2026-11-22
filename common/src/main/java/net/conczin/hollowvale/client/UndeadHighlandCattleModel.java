package net.conczin.hollowvale.client;

import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.entity.UndeadHighlandCattle;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * The body group is modelled along X and turned 90° to face -Z, so the head's local Z axis is pitch and its local X axis is roll.
 */
public class UndeadHighlandCattleModel extends DefaultedEntityGeoModel<UndeadHighlandCattle> {
    private static final float MAX_HEAD_YAW = 50.0f;
    private static final float TILT = 22.0f * Mth.DEG_TO_RAD;

    public UndeadHighlandCattleModel() {
        super(Common.locate("undead_highland_cattle"));
    }

    @Override
    public void setCustomAnimations(UndeadHighlandCattle cattle, long instanceId, AnimationState<UndeadHighlandCattle> state) {
        GeoBone head = getAnimationProcessor().getBone("head");
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head == null || data == null || cattle.isStanding()) {
            return;
        }

        head.setRotY(head.getRotY() + Mth.clamp(data.netHeadYaw(), -MAX_HEAD_YAW, MAX_HEAD_YAW) * Mth.DEG_TO_RAD);
        head.setRotZ(head.getRotZ() - data.headPitch() * Mth.DEG_TO_RAD);
        head.setRotX(head.getRotX() + cattle.getTilt(state.getPartialTick()) * TILT);
    }
}
