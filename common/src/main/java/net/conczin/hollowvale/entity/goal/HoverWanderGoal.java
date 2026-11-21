package net.conczin.hollowvale.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Flies to random nearby spots a random height above the ground, with short pauses in between.
 */
public class HoverWanderGoal extends Goal {
    private static final int RANGE = 8;
    private static final int MAX_DROP = 8;
    private static final int ATTEMPTS = 10;

    private final PathfinderMob mob;
    private final double speed;
    private final double minHeight;
    private final double maxHeight;
    private int nextMoveTick;

    public HoverWanderGoal(PathfinderMob mob, double speed, double hoverHeight) {
        this(mob, speed, hoverHeight, hoverHeight);
    }

    public HoverWanderGoal(PathfinderMob mob, double speed, double minHeight, double maxHeight) {
        this.mob = mob;
        this.speed = speed;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
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
        Vec3 spot = findSpot();
        if (spot != null) {
            mob.getNavigation().moveTo(spot.x, spot.y, spot.z, speed);
        }
    }

    @Override
    public void stop() {
        nextMoveTick = mob.tickCount + 10 + mob.getRandom().nextInt(30);
    }

    private @Nullable Vec3 findSpot() {
        Level level = mob.level();
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int x = mob.getBlockX() + mob.getRandom().nextInt(RANGE * 2 + 1) - RANGE;
            int z = mob.getBlockZ() + mob.getRandom().nextInt(RANGE * 2 + 1) - RANGE;
            int top = mob.getBlockY() + mob.getRandom().nextInt(5) - 1;

            // Walk down from the candidate height until there is ground below an empty block.
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
            for (int drop = 0; drop < MAX_DROP; drop++, pos.move(0, -1, 0)) {
                if (level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above()) && level.getBlockState(pos.below()).isSolid()) {
                    return new Vec3(x + 0.5, pos.getY() + Mth.nextDouble(mob.getRandom(), minHeight, maxHeight), z + 0.5);
                }
            }
        }
        return null;
    }
}
