package net.conczin.hollowvale.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.conczin.hollowvale.block.MarrowLampBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ChorusFruitItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(ChorusFruitItem.class)
public abstract class ChorusFruitItemMixin {
    // A nearby Marrow Lamp replaces the random destination
    @WrapOperation(method = "finishUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;randomTeleport(DDDZ)Z"))
    private boolean hollowvale$teleportToLamp(LivingEntity entity, double x, double y, double z, boolean particles, Operation<Boolean> original) {
        BlockPos lamp = MarrowLampBlock.findNearest(entity.level(), entity.blockPosition(), MarrowLampBlock.RECALL_RANGE).orElse(null);
        if (lamp != null) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos spot = lamp.relative(direction);
                if (entity.randomTeleport(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, particles)) {
                    return true;
                }
            }
        }
        return original.call(entity, x, y, z, particles);
    }
}
