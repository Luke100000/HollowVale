package net.conczin.hollowvale.mixin;

import net.conczin.hollowvale.entity.HarpoonBolt;
import net.conczin.hollowvale.item.Items;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {
    // The harpoon is out on its line, so the hand it left is empty.
    @Inject(method = "updateForLiving", at = @At("RETURN"))
    private void hollowvale$hideThrownHarpoon(ItemStackRenderState output, ItemStack stack, ItemDisplayContext context, LivingEntity entity, CallbackInfo ci) {
        if ((context.firstPerson() || hollowVale$isThirdPersonHand(context)) && hollowVale$isThrown(entity, stack)) {
            output.clear();
        }
    }

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
