package net.conczin.hollowvale.mixin;

import net.conczin.hollowvale.entity.BoneWhale;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    // Whale riders keep their body on the saddle instead of twisting toward where they look.
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void hollowvale$lockBodyToWhale(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (entity.getVehicle() instanceof BoneWhale whale) {
            float whaleRot = Mth.rotLerp(partialTick, whale.yBodyRotO, whale.yBodyRot);
            float shift = Mth.wrapDegrees(whaleRot - state.bodyRot);
            state.bodyRot = whaleRot;
            state.yRot = Mth.clamp(Mth.wrapDegrees(state.yRot - shift), -85.0f, 85.0f);
        }
    }
}
