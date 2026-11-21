package net.conczin.hollowvale.mixin;

import net.conczin.hollowvale.entity.BoneWhale;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    // Sneak descends on a whale, it only dismounts once landed.
    @Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
    private void hollowvale$keepRidingAirborneWhale(CallbackInfoReturnable<Boolean> cir) {
        if (((Player) (Object) this).getVehicle() instanceof BoneWhale whale && !whale.onGround()) {
            cir.setReturnValue(false);
        }
    }

    // Sneaking is meaningless on a whale, so the camera stays put instead of dropping with the crouch pose.
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void hollowvale$noCrouchOnWhale(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.getVehicle() instanceof BoneWhale) {
            player.setPose(Pose.STANDING);
            ci.cancel();
        }
    }
}
