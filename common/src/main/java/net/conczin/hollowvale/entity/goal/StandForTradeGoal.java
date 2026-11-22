package net.conczin.hollowvale.entity.goal;

import net.conczin.hollowvale.entity.Scavenger;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class StandForTradeGoal extends Goal {
    private final Scavenger scavenger;
    private final double rangeSq;

    public StandForTradeGoal(Scavenger scavenger, double rangeSq) {
        this.scavenger = scavenger;
        this.rangeSq = rangeSq;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        Player player = scavenger.getTradingPlayer();
        return scavenger.isAlive() && player != null && scavenger.distanceToSqr(player) < rangeSq;
    }

    @Override
    public void start() {
        scavenger.getNavigation().stop();
    }

    @Override
    public void stop() {
        scavenger.setTradingPlayer(null);
    }
}
