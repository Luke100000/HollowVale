package net.conczin.hollowvale.client;

import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.entity.BoneWhale;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class BoneWhaleModel extends DefaultedEntityGeoModel<BoneWhale> {
    private final DefaultedEntityGeoModel<BoneWhale> calf = new DefaultedEntityGeoModel<>(Common.locate("bone_whale_calf"));

    public BoneWhaleModel() {
        super(Common.locate("bone_whale"));
    }

    @Override
    public ResourceLocation getModelResource(BoneWhale whale) {
        return whale.isBaby() ? calf.getModelResource(whale) : super.getModelResource(whale);
    }

    @Override
    public ResourceLocation getTextureResource(BoneWhale whale) {
        return whale.isBaby() ? calf.getTextureResource(whale) : super.getTextureResource(whale);
    }

    @Override
    public ResourceLocation getAnimationResource(BoneWhale whale) {
        return whale.isBaby() ? calf.getAnimationResource(whale) : super.getAnimationResource(whale);
    }
}
