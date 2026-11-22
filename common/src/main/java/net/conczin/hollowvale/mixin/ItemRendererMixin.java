package net.conczin.hollowvale.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.conczin.hollowvale.entity.HarpoonBolt;
import net.conczin.hollowvale.item.HarpoonItem;
import net.conczin.hollowvale.item.Items;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    // The inventory shows the sprite, hands and the thrown bolt show the 3D model, which the item model switches to through custom model data.
    @Inject(method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
            at = @At("HEAD"), cancellable = true)
    private void hollowvale$harpoonInHand(LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack pose, MultiBufferSource buffer, Level level, int light, int overlay, int seed, CallbackInfo ci) {
        if ((context.firstPerson() || hollowVale$isThirdPersonHand(context)) && hollowVale$isThrown(entity, stack)) {
            ci.cancel();
            return;
        }
        if (!context.firstPerson() && !hollowVale$isThirdPersonHand(context) || !stack.is(Items.BONE_HARPOON) || stack.has(DataComponents.CUSTOM_MODEL_DATA)) {
            return;
        }
        ItemStack held = stack.copy();
        held.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(HarpoonItem.IN_HAND_MODEL));
        ((ItemRenderer) (Object) this).renderStatic(entity, held, context, leftHand, pose, buffer, level, light, overlay, seed);
        ci.cancel();
    }

    // The harpoon is out on its line, so the hand it left is empty.
    @Unique
    private static boolean hollowVale$isThrown(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player) || !stack.is(Items.BONE_HARPOON)) {
            return false;
        }
        HarpoonBolt bolt = HarpoonBolt.find(player);
        return bolt != null && player.getItemInHand(bolt.getHand()) == stack;
    }

    @Unique
    private static boolean hollowVale$isThirdPersonHand(ItemDisplayContext context) {
        return context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }
}
