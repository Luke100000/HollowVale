package net.conczin.hollowvale.entity.goal;

import net.conczin.hollowvale.entity.UndeadHighlandCattle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class HerdGoal extends Goal {
    private static final double SEARCH_RANGE = 32.0;
    private static final double MIN_DISTANCE = 5.0;
    private static final double ARRIVED_DISTANCE = 3.0;
    private static final int MIN_COOLDOWN = 200;
    private static final int MAX_COOLDOWN = 600;
    private static final int MAX_TICKS = 400;
    private static final double SPEED = 0.9;

    private final UndeadHighlandCattle cattle;
    private @Nullable UndeadHighlandCattle target;
    private int nextCheck;
    private int running;

    public HerdGoal(UndeadHighlandCattle cattle) {
        this.cattle = cattle;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cattle.tickCount < nextCheck) {
            return false;
        }
        nextCheck = cattle.tickCount + Mth.nextInt(cattle.getRandom(), MIN_COOLDOWN, MAX_COOLDOWN);

        List<UndeadHighlandCattle> others = cattle.level().getEntitiesOfClass(UndeadHighlandCattle.class,
                cattle.getBoundingBox().inflate(SEARCH_RANGE),
                other -> other != cattle && other.isAlive() && cattle.distanceToSqr(other) > MIN_DISTANCE * MIN_DISTANCE);
        if (others.isEmpty()) {
            return false;
        }
        target = others.get(cattle.getRandom().nextInt(others.size()));
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && running < MAX_TICKS && cattle.distanceToSqr(target) > ARRIVED_DISTANCE * ARRIVED_DISTANCE;
    }

    @Override
    public void start() {
        running = 0;
        moveToTarget();
    }

    @Override
    public void stop() {
        target = null;
        cattle.getNavigation().stop();
    }

    @Override
    public void tick() {
        running++;
        if (running % 20 == 0 || cattle.getNavigation().isDone()) {
            moveToTarget();
        }
    }

    private void moveToTarget() {
        if (target != null) {
            cattle.getNavigation().moveTo(target, SPEED);
        }
    }
}
