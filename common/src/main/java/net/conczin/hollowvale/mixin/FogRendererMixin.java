package net.conczin.hollowvale.mixin;

import net.conczin.hollowvale.world.ModBiomes;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Unique
    private static final float OSSUARY_FOG_SHARE = 0.5f;
    @Unique
    private static final float VALE_FOG_SHARE = 0.8f;
    @Unique
    private static final float MARROW_FOG_SHARE = 0.25f;
    @Unique
    private static final float NEUTRAL_SHARE = 0.999f;
    @Unique
    private float hollowvale$share = 1.0f;

    // Thickens the terrain fog in the Hollow Vale biomes as a share of the render distance, fading in and out when crossing their border.
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void hollowvale$thickenFog(Camera camera, int renderDistance, DeltaTracker deltaTracker, float darkenWorldAmount, ClientLevel level, CallbackInfoReturnable<FogData> cir) {
        if (camera.getFluidInCamera() != FogType.NONE || camera.entity() == null) {
            return;
        }

        Holder<Biome> biome = level.getBiome(camera.blockPosition());
        float target = biome.is(ModBiomes.OSSUARY) ? OSSUARY_FOG_SHARE : biome.is(ModBiomes.HOLLOW_VALE) ? VALE_FOG_SHARE : biome.is(ModBiomes.MARROW_HOLLOWS) ? MARROW_FOG_SHARE : 1.0f;
        hollowvale$share = Mth.lerp(0.004f, hollowvale$share, target);
        if (hollowvale$share > NEUTRAL_SHARE) {
            return;
        }

        // Blends from vanilla's own fog, so it is untouched outside the biomes and only thickens as the share drops.
        FogData fog = cir.getReturnValue();
        float thickness = Mth.clamp((1.0f - hollowvale$share) / (1.0f - MARROW_FOG_SHARE), 0.0f, 1.0f);
        float environmentalEnd = fog.environmentalEnd * hollowvale$share;
        float renderDistanceEnd = fog.renderDistanceEnd * hollowvale$share;
        fog.environmentalStart = Mth.lerp(thickness, fog.environmentalStart, environmentalEnd * 0.25f);
        fog.renderDistanceStart = Mth.lerp(thickness, fog.renderDistanceStart, renderDistanceEnd * 0.25f);
        fog.environmentalEnd = environmentalEnd;
        fog.renderDistanceEnd = renderDistanceEnd;
    }
}
