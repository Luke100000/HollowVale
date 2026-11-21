package net.conczin.hollowvale.mixin;

import net.conczin.hollowvale.Sounds;
import net.conczin.hollowvale.client.AmbientAdditions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleSoundInstance.class)
public abstract class SimpleSoundInstanceMixin {
    // Vanilla plays biome additions without a position, which sounds like they are right behind the player.
    @Inject(method = "forAmbientAddition", at = @At("HEAD"), cancellable = true)
    private static void hollowvale$placeAddition(SoundEvent sound, CallbackInfoReturnable<SimpleSoundInstance> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !sound.getLocation().equals(Sounds.AMBIENT_ADDITIONS.getLocation())) {
            return;
        }
        SimpleSoundInstance placed = AmbientAdditions.create(player);
        if (placed != null) {
            cir.setReturnValue(placed);
        }
    }
}
