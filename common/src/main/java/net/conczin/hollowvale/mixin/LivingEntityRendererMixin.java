package net.conczin.hollowvale.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.conczin.hollowvale.entity.BoneWhale;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Unique
    private float hollowvale$bodyShift;

    // Whale riders keep their body on the saddle instead of twisting toward where they look.
    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V"),
            index = 3)
    private float hollowvale$lockBodyToWhale(LivingEntity entity, PoseStack poseStack, float bob, float bodyRot, float partialTick, float scale) {
        if (entity.getVehicle() instanceof BoneWhale whale) {
            float whaleRot = Mth.rotLerp(partialTick, whale.yBodyRotO, whale.yBodyRot);
            hollowvale$bodyShift = Mth.wrapDegrees(whaleRot - bodyRot);
            return whaleRot;
        }
        hollowvale$bodyShift = 0.0f;
        return bodyRot;
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V"),
            index = 4)
    private float hollowvale$keepHeadYaw(float netHeadYaw) {
        if (hollowvale$bodyShift == 0.0f) {
            return netHeadYaw;
        }
        return Mth.clamp(Mth.wrapDegrees(netHeadYaw - hollowvale$bodyShift), -85.0f, 85.0f);
    }
}
