package net.conczin.hollowvale.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.conczin.hollowvale.world.ModBiomes;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Unique
    private static final float OSSUARY_FOG_END = 100.0f;
    @Unique
    private static final float VALE_FOG_END = 140.0f;
    @Unique
    private static final float MARROW_FOG_END = 100.0f;
    @Unique
    private static float hollowvale$density;

    // Thickens the terrain fog in the Hollow Vale biomes, fading in and out when crossing their border.
    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void hollowvale$thickenFog(Camera camera, FogRenderer.FogMode mode, float farPlane, boolean foggy, float partialTick, CallbackInfo ci) {
        if (mode != FogRenderer.FogMode.FOG_TERRAIN || camera.getFluidInCamera() != FogType.NONE) {
            return;
        }
        var biome = camera.getEntity().level().getBiome(camera.getBlockPosition());
        float target = biome.is(ModBiomes.OSSUARY) ? OSSUARY_FOG_END : biome.is(ModBiomes.HOLLOW_VALE) ? VALE_FOG_END
                : biome.is(ModBiomes.MARROW_HOLLOWS) ? MARROW_FOG_END : farPlane;
        hollowvale$density = hollowvale$density == 0.0f ? target : Mth.lerp(0.004f, hollowvale$density, target);
        float end = Math.min(farPlane, hollowvale$density);
        RenderSystem.setShaderFogStart(end * 0.25f);
        RenderSystem.setShaderFogEnd(end);
    }
}
