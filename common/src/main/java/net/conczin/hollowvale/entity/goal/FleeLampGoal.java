package net.conczin.hollowvale.entity.goal;

import net.conczin.hollowvale.block.MarrowLampBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class FleeLampGoal extends Goal {
    private static final int CHECK_INTERVAL = 20;
    private static final double MARGIN = 4.0;

    private final PathfinderMob mob;
    private final double speed;
    private @Nullable BlockPos lamp;

    public FleeLampGoal(PathfinderMob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.tickCount % CHECK_INTERVAL != 0) {
            return false;
        }
        lamp = MarrowLampBlock.findNearest(mob.level(), mob.blockPosition(), MarrowLampBlock.SAFE_RANGE).orElse(null);
        return lamp != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        mob.setTarget(null);
        if (lamp == null) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(lamp);
        Vec3 away = mob.position().subtract(center).multiply(1.0, 0.0, 1.0);
        if (away.lengthSqr() < 1.0e-4) {
            away = new Vec3(1.0, 0.0, 0.0);
        }
        Vec3 target = center.add(away.normalize().scale(MarrowLampBlock.SAFE_RANGE + MARGIN));
        mob.getNavigation().moveTo(target.x, mob.getY(), target.z, speed);
    }
}
