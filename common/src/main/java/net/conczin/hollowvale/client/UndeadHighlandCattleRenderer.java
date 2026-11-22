package net.conczin.hollowvale.client;

import net.conczin.hollowvale.entity.UndeadHighlandCattle;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class UndeadHighlandCattleRenderer extends GeoEntityRenderer<UndeadHighlandCattle> {
    public UndeadHighlandCattleRenderer(EntityRendererProvider.Context context) {
        super(context, new UndeadHighlandCattleModel());

        shadowRadius = 0.7f;
    }
}
