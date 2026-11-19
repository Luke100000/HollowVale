package net.conczin.hollowvale.client;

import net.conczin.hollowvale.entity.BoneWhale;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BoneWhaleRenderer extends GeoEntityRenderer<BoneWhale> {
    public BoneWhaleRenderer(EntityRendererProvider.Context context) {
        super(context, new BoneWhaleModel());
    }

    @Override
    protected float getDeathMaxRotation(BoneWhale whale) {
        return 0.0f;
    }
}
