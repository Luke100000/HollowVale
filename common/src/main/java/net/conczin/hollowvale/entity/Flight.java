package net.conczin.hollowvale.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class Flight {
    public static final float AIR_DRAG = 0.91f;

    private Flight() {
    }

    public static void travel(LivingEntity entity, Vec3 input) {
        if (!entity.isControlledByLocalInstance()) {
            return;
        }
        if (entity.isInWater() || entity.isInLava()) {
            entity.moveRelative(0.02f, input);
            entity.move(MoverType.SELF, entity.getDeltaMovement());
            entity.setDeltaMovement(entity.getDeltaMovement().scale(entity.isInWater() ? 0.8 : 0.5));
        } else {
            entity.moveRelative(1.0f - AIR_DRAG, input);
            entity.move(MoverType.SELF, entity.getDeltaMovement());
            entity.setDeltaMovement(entity.getDeltaMovement().scale(AIR_DRAG));
        }
        entity.calculateEntityAnimation(false);
    }

    public static void thrust(LivingEntity entity, Vec3 thrust, float speed) {
        entity.setDeltaMovement(entity.getDeltaMovement().add(thrust.scale(speed * (1.0f - AIR_DRAG))));
        entity.move(MoverType.SELF, entity.getDeltaMovement());
        entity.setDeltaMovement(entity.getDeltaMovement().scale(AIR_DRAG));
        entity.calculateEntityAnimation(false);
    }

    public static PathNavigation navigation(Mob mob, Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(mob, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }
}
