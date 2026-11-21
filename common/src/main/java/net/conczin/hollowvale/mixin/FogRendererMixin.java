package net.conczin.hollowvale.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.conczin.hollowvale.world.ModBiomes;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    private static float hollowvale$share = 1.0f;

    // Thickens the terrain fog in the Hollow Vale biomes as a share of the render distance, fading in and out when crossing their border.
    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void hollowvale$thickenFog(Camera camera, FogRenderer.FogMode mode, float farPlane, boolean foggy, float partialTick, CallbackInfo ci) {
        if (mode != FogRenderer.FogMode.FOG_TERRAIN || camera.getFluidInCamera() != FogType.NONE) {
            return;
        }

        Holder<Biome> biome = camera.getEntity().level().getBiome(camera.getBlockPosition());
        float target = biome.is(ModBiomes.OSSUARY) ? OSSUARY_FOG_SHARE : biome.is(ModBiomes.HOLLOW_VALE) ? VALE_FOG_SHARE : biome.is(ModBiomes.MARROW_HOLLOWS) ? MARROW_FOG_SHARE : 1.0f;
        hollowvale$share = Mth.lerp(0.004f, hollowvale$share, target);
        if (hollowvale$share > NEUTRAL_SHARE) {
            return;
        }

        // Blends from vanilla's own fog, so it is untouched outside the biomes and only thickens as the share drops.
        float vanillaStart = RenderSystem.getShaderFogStart();
        float end = RenderSystem.getShaderFogEnd() * hollowvale$share;
        float thickness = Mth.clamp((1.0f - hollowvale$share) / (1.0f - MARROW_FOG_SHARE), 0.0f, 1.0f);
        RenderSystem.setShaderFogStart(Mth.lerp(thickness, vanillaStart, end * 0.25f));
        RenderSystem.setShaderFogEnd(end);
    }
}
