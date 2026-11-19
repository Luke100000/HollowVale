package net.conczin.hollowvale.entity;

import net.minecraft.util.Mth;

public class YawDamper {
    private final float maxSpeed;
    private final float acceleration;
    private float speed;

    public YawDamper(float maxSpeed, float acceleration) {
        this.maxSpeed = maxSpeed;
        this.acceleration = acceleration;
    }

    public float turn(float yaw, float targetYaw) {
        // Brakes in time to stop at the target instead of overshooting it.
        float delta = Mth.wrapDegrees(targetYaw - yaw);
        float braking = Mth.sqrt(2.0f * acceleration * Math.abs(delta)) * 0.9f;
        float wanted = Math.copySign(Math.min(maxSpeed, Math.min(braking, Math.abs(delta) * 0.3f)), delta);
        speed += Mth.clamp(wanted - speed, -acceleration, acceleration);
        return yaw + speed;
    }
}
