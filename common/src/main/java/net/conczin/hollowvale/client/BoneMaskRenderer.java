package net.conczin.hollowvale.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.conczin.hollowvale.item.Items;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

public final class BoneMaskRenderer {
    private static final int WORN_MODEL = 1;

    // The mask has no armor texture, it is drawn like a head item with the worn model its item model switches to. Returns whether it was drawn.
    public static <T extends LivingEntity, M extends HumanoidModel<T>> boolean draw(RenderLayer<T, M> layer, PoseStack pose, MultiBufferSource buffer, T entity, EquipmentSlot slot, int light) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (slot != EquipmentSlot.HEAD || !stack.is(Items.BONE_MASK)) {
            return false;
        }

        ItemStack worn = stack.copy();
        worn.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(WORN_MODEL));

        pose.pushPose();
        layer.getParentModel().getHead().translateAndRotate(pose);
        CustomHeadLayer.translateToHead(pose, false);
        Minecraft.getInstance().getItemRenderer().renderStatic(entity, worn, ItemDisplayContext.HEAD, false, pose, buffer, entity.level(), light, OverlayTexture.NO_OVERLAY, entity.getId());
        pose.popPose();
        return true;
    }
}
