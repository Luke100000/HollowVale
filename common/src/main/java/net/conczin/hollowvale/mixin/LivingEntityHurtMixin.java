package net.conczin.hollowvale.mixin;

import net.conczin.hollowvale.effect.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHurtMixin {
    @Unique
    private static final float HOOKED_DAMAGE_FACTOR = 1.5f;

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float hollowvale$hookedDamage(float amount) {
        LivingEntity self = (LivingEntity) (Object) this;
        return self.hasEffect(ModEffects.holder(ModEffects.HOOKED)) ? amount * HOOKED_DAMAGE_FACTOR : amount;
    }
}
