package net.conczin.hollowvale.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.conczin.hollowvale.entity.BoneWhale;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilMixin {
    // The crosshair skips the rider's own vehicle, but whale riders right-click the whale to dismount.
    @WrapOperation(method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getRootVehicle()Lnet/minecraft/world/entity/Entity;", ordinal = 0))
    private static Entity hollowvale$pickRiddenWhale(Entity candidate, Operation<Entity> original) {
        return candidate instanceof BoneWhale ? null : original.call(candidate);
    }
}
