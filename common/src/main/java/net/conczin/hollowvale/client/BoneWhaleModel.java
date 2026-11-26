package net.conczin.hollowvale.client;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.entity.BoneWhale;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class BoneWhaleModel extends DefaultedEntityGeoModel<BoneWhale> {
    static final int ADULT = 0;
    static final int YOUNG = 1;
    static final int CALF = 2;

    private final DefaultedEntityGeoModel<BoneWhale> calf = new DefaultedEntityGeoModel<>(Common.locate("bone_whale_calf"));
    private final DefaultedEntityGeoModel<BoneWhale> young = new DefaultedEntityGeoModel<>(Common.locate("bone_whale_young"));

    public BoneWhaleModel() {
        super(Common.locate("bone_whale"));
    }

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        DefaultedEntityGeoModel<BoneWhale> stage = stage(state.getOrDefaultGeckolibData(BoneWhaleRenderer.STAGE, ADULT));
        return stage == null ? super.getModelResource(state) : stage.getModelResource(state);
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        DefaultedEntityGeoModel<BoneWhale> stage = stage(state.getOrDefaultGeckolibData(BoneWhaleRenderer.STAGE, ADULT));
        return stage == null ? super.getTextureResource(state) : stage.getTextureResource(state);
    }

    @Override
    public Identifier getAnimationResource(BoneWhale whale) {
        DefaultedEntityGeoModel<BoneWhale> stage = stage(whale.isYoung() ? YOUNG : whale.isBaby() ? CALF : ADULT);
        return stage == null ? super.getAnimationResource(whale) : stage.getAnimationResource(whale);
    }

    private @Nullable DefaultedEntityGeoModel<BoneWhale> stage(int stage) {
        return switch (stage) {
            case YOUNG -> young;
            case CALF -> calf;
            default -> null;
        };
    }
}
