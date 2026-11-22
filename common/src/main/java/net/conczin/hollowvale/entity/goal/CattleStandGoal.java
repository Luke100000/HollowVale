package net.conczin.hollowvale.entity.goal;

import net.conczin.hollowvale.entity.UndeadHighlandCattle;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class CattleStandGoal extends Goal {
    private static final float TURN_SPEED = 6.0f;

    private final UndeadHighlandCattle cattle;

    public CattleStandGoal(UndeadHighlandCattle cattle) {
        this.cattle = cattle;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return cattle.isStanding();
    }

    @Override
    public boolean canContinueToUse() {
        return cattle.isStanding();
    }

    @Override
    public void start() {
        cattle.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Player player = cattle.getStandTarget();
        if (player != null) {
            cattle.faceTowards(player, TURN_SPEED);
        }
    }
}
