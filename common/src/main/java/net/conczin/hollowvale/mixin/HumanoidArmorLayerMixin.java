package net.conczin.hollowvale.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.conczin.hollowvale.client.BoneMaskRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> {
    @SuppressWarnings("unchecked")
    @Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V",
            at = @At("HEAD"), cancellable = true)
    private void hollowvale$renderMask(PoseStack pose, MultiBufferSource buffer, T entity, EquipmentSlot slot, int light, A model, CallbackInfo ci) {
        if (BoneMaskRenderer.draw((RenderLayer<T, M>) (Object) this, pose, buffer, entity, slot, light)) {
            ci.cancel();
        }
    }
}
