package net.conczin.hollowvale.entity.goal;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Wanders at an altitude above the heightmap.
 */
public class SkyWanderGoal extends Goal {
    private static final int ATTEMPTS = 10;

    private final PathfinderMob mob;
    private final double speed;
    private final double minDistance;
    private final double maxDistance;
    private final double minAltitude;
    private final double maxAltitude;
    private final int minPause;
    private final int maxPause;
    private int nextMoveTick;

    public SkyWanderGoal(PathfinderMob mob, double speed, double minDistance, double maxDistance,
                         double minAltitude, double maxAltitude, int minPause, int maxPause) {
        this.mob = mob;
        this.speed = speed;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.minAltitude = minAltitude;
        this.maxAltitude = maxAltitude;
        this.minPause = minPause;
        this.maxPause = maxPause;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return mob.tickCount >= nextMoveTick && mob.getNavigation().isDone();
    }

    @Override
    public boolean canContinueToUse() {
        return !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        Vec3 target = findTarget();
        if (target != null) {
            mob.getNavigation().moveTo(target.x, target.y, target.z, speed);
        }
    }

    @Override
    public void stop() {
        nextMoveTick = mob.tickCount + Mth.nextInt(mob.getRandom(), minPause, maxPause);
    }

    private @Nullable Vec3 findTarget() {
        Level level = mob.level();
        RandomSource random = mob.getRandom();
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = Mth.nextDouble(random, minDistance, maxDistance);
            double x = mob.getX() + Math.cos(angle) * distance;
            double z = mob.getZ() + Math.sin(angle) * distance;

            // Unloaded chunks and the void report the bottom of the world.
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
            if (ground <= level.getMinBuildHeight()) {
                continue;
            }
            double y = ground + Mth.nextDouble(random, minAltitude, maxAltitude);
            return new Vec3(x, Math.min(y, level.getMaxBuildHeight() - mob.getBbHeight() - 1.0), z);
        }
        return null;
    }
}
