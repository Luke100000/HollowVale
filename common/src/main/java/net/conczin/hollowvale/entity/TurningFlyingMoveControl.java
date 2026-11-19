package net.conczin.hollowvale.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;

public class TurningFlyingMoveControl extends FlyingMoveControl {
    private final YawDamper damper;

    public TurningFlyingMoveControl(Mob mob, int maxPitchPerTick, float maxYawPerTick, float yawAcceleration) {
        super(mob, maxPitchPerTick, true);
        this.damper = new YawDamper(maxYawPerTick, yawAcceleration);
    }

    @Override
    public void tick() {
        float yaw = mob.getYRot();
        super.tick();
        mob.setYRot(damper.turn(yaw, mob.getYRot()));
    }
}
