package net.conczin.hollowvale.mixin;

import net.conczin.hollowvale.item.BoneMaskItem;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderMan.class)
public abstract class EnderManMixin {
    @Inject(method = "isLookingAtMe", at = @At("HEAD"), cancellable = true)
    private void hollowvale$boneMask(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (BoneMaskItem.isWornBy(player)) {
            cir.setReturnValue(false);
        }
    }
}
