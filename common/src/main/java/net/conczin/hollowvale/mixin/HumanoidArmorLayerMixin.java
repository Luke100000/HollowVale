package net.conczin.hollowvale.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.conczin.hollowvale.item.Items;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> {
    @Unique
    private static final int WORN_MODEL = 1;

    // NeoForge renders every piece through a second overload that takes the animation values, the vanilla one has only the first. Each is optional.
    @Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void hollowvale$renderMask(PoseStack pose, MultiBufferSource buffer, T entity, EquipmentSlot slot, int light, A model, CallbackInfo ci) {
        hollowvale$drawMask(pose, buffer, entity, slot, light, ci);
    }

    @Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void hollowvale$renderMaskAnimated(PoseStack pose, MultiBufferSource buffer, T entity, EquipmentSlot slot, int light, A model, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        hollowvale$drawMask(pose, buffer, entity, slot, light, ci);
    }

    // The mask has no armor texture, it is drawn like a head item with the worn model its item model switches to.
    @SuppressWarnings("unchecked")
    @Unique
    private void hollowvale$drawMask(PoseStack pose, MultiBufferSource buffer, T entity, EquipmentSlot slot, int light, CallbackInfo ci) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (slot != EquipmentSlot.HEAD || !stack.is(Items.BONE_MASK)) {
            return;
        }

        ItemStack worn = stack.copy();
        worn.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(WORN_MODEL));

        pose.pushPose();
        ((RenderLayer<T, M>) (Object) this).getParentModel().getHead().translateAndRotate(pose);
        CustomHeadLayer.translateToHead(pose, false);
        Minecraft.getInstance().getItemRenderer().renderStatic(entity, worn, ItemDisplayContext.HEAD, false, pose, buffer, entity.level(), light, OverlayTexture.NO_OVERLAY, entity.getId());
        pose.popPose();
        ci.cancel();
    }
}
