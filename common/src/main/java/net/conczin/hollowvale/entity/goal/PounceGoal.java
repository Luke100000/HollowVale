package net.conczin.hollowvale.entity.goal;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.conczin.hollowvale.Sounds;
import net.conczin.hollowvale.entity.BoneSludge;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Stops near the target, squishes down as a visible wind-up, then launches itself at it and hits on contact.
 */
public class PounceGoal extends Goal {
    private static final int WIND_UP_TICKS = 15;
    private static final int MAX_FLIGHT_TICKS = 30;
    private static final double MIN_RANGE = 1.5;
    private static final double MAX_RANGE = 4.0;

    private final BoneSludge sludge;
    private int ticks;
    private int nextPounceTick;
    private boolean launched;
    private boolean hit;

    public PounceGoal(BoneSludge sludge) {
        this.sludge = sludge;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = sludge.getTarget();
        if (target == null || !target.isAlive() || sludge.tickCount < nextPounceTick || !sludge.onGround()) {
            return false;
        }
        double distance = sludge.distanceToSqr(target);
        return distance > MIN_RANGE * MIN_RANGE && distance < MAX_RANGE * MAX_RANGE && sludge.hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = sludge.getTarget();
        if (target == null || !target.isAlive() || ticks > WIND_UP_TICKS + MAX_FLIGHT_TICKS) {
            return false;
        }
        // Ends once it lands again after the jump.
        return !launched || ticks < WIND_UP_TICKS + 3 || !sludge.onGround();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        ticks = 0;
        launched = false;
        hit = false;
        sludge.getNavigation().stop();
        sludge.setWindingUp(true);
        sludge.playSound(Sounds.BONE_SLUDGE_SQUISH);
    }

    @Override
    public void stop() {
        sludge.setWindingUp(false);
        nextPounceTick = sludge.tickCount + 40 + sludge.getRandom().nextInt(20);
    }

    @Override
    public void tick() {
        LivingEntity target = sludge.getTarget();
        if (target == null) {
            return;
        }
        ticks++;
        sludge.getLookControl().setLookAt(target, 30.0f, 30.0f);

        if (!launched) {
            if (ticks >= WIND_UP_TICKS) {
                launch(target);
            }
        } else if (!hit && sludge.getBoundingBox().inflate(0.2).intersects(target.getBoundingBox())) {
            hit = true;
            sludge.doHurtTarget((ServerLevel) sludge.level(), target);
        }
    }

    private void launch(LivingEntity target) {
        launched = true;
        sludge.setWindingUp(false);
        Vec3 direction = target.position().subtract(sludge.position()).multiply(1.0, 0.0, 1.0).normalize();
        sludge.setYRot((float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0f);
        sludge.yBodyRot = sludge.getYRot();
        sludge.jumpFromGround();
        sludge.setDeltaMovement(sludge.getDeltaMovement().add(direction.scale(0.8)));
    }
}
