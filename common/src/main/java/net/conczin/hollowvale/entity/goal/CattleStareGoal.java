package net.conczin.hollowvale.entity.goal;

import net.conczin.hollowvale.entity.UndeadHighlandCattle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Stare at a nearby player, tilting its head from side to side.
 */
public class CattleStareGoal extends Goal {
    private static final int ODDS = 200;
    private static final double RANGE = 12.0;
    private static final int MIN_TICKS = 80;
    private static final int MAX_TICKS = 200;
    private static final int MIN_TILT_TICKS = 25;
    private static final int MAX_TILT_TICKS = 60;

    private final UndeadHighlandCattle cattle;
    private @Nullable Player player;
    private int remaining;
    private int nextTilt;

    public CattleStareGoal(UndeadHighlandCattle cattle) {
        this.cattle = cattle;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cattle.isStanding() || cattle.getRandom().nextInt(ODDS) != 0) {
            return false;
        }
        player = cattle.level().getNearestPlayer(TargetingConditions.forNonCombat().range(RANGE), cattle);
        return player != null;
    }

    @Override
    public boolean canContinueToUse() {
        return remaining > 0 && !cattle.isStanding() && player != null && player.isAlive() && cattle.distanceToSqr(player) < RANGE * RANGE * 1.5;
    }

    @Override
    public void start() {
        remaining = Mth.nextInt(cattle.getRandom(), MIN_TICKS, MAX_TICKS);
        nextTilt = 0;
        cattle.getNavigation().stop();
    }

    @Override
    public void stop() {
        player = null;
        cattle.setTilt(0);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        remaining--;
        if (player == null) {
            return;
        }
        cattle.getLookControl().setLookAt(player, 30.0f, 30.0f);

        if (--nextTilt <= 0) {
            nextTilt = Mth.nextInt(cattle.getRandom(), MIN_TILT_TICKS, MAX_TILT_TICKS);
            cattle.setTilt(cattle.getRandom().nextInt(3) - 1);
        }
    }
}
