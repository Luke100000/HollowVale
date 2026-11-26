package net.conczin.hollowvale.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.entity.UndeadHighlandCattle;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * Turns the head toward whatever the cattle looks at and tilts it while it stares.
 */
public class UndeadHighlandCattleRenderer extends GeoEntityRenderer<UndeadHighlandCattle, LivingEntityRenderState> {
    private static final float MAX_HEAD_YAW = 75.0f;
    private static final float TILT = 25.0f * Mth.DEG_TO_RAD;

    private static final DataTicket<Boolean> STANDING = DataTicket.create("hollowvale_standing", Boolean.class);
    private static final DataTicket<Float> HEAD_TILT = DataTicket.create("hollowvale_tilt", Float.class);

    public UndeadHighlandCattleRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(Common.locate("undead_highland_cattle")));

        shadowRadius = 0.7f;
    }

    @Override
    public void addRenderData(UndeadHighlandCattle cattle, Void unused, LivingEntityRenderState state, float partialTick) {
        state.addGeckolibData(STANDING, cattle.isStanding());
        state.addGeckolibData(HEAD_TILT, cattle.getTilt(partialTick));
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
        if (info.getOrDefaultGeckolibData(STANDING, false)) {
            return;
        }
        LivingEntityRenderState state = info.renderState();
        bones.ifPresent("head", head -> {
            head.setRotY(head.getRotY() - Mth.clamp(state.yRot, -MAX_HEAD_YAW, MAX_HEAD_YAW) * Mth.DEG_TO_RAD);
            head.setRotX(head.getRotX() - state.xRot * Mth.DEG_TO_RAD);
            head.setRotZ(head.getRotZ() + info.getOrDefaultGeckolibData(HEAD_TILT, 0.0f) * TILT);
        });
    }
}
