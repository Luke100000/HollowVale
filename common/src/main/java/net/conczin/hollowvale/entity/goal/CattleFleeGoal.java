package net.conczin.hollowvale.entity.goal;

import net.conczin.hollowvale.entity.UndeadHighlandCattle;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.player.Player;

public class CattleFleeGoal extends AvoidEntityGoal<Player> {
    private static final float RANGE = 12.0f;

    private final UndeadHighlandCattle cattle;

    public CattleFleeGoal(UndeadHighlandCattle cattle, double speed) {
        super(cattle, Player.class, player -> !player.isSpectator(), RANGE, speed, speed, player -> true);
        this.cattle = cattle;
    }

    @Override
    public boolean canUse() {
        return !cattle.isHealthy() && !cattle.isStanding() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !cattle.isHealthy() && !cattle.isStanding() && super.canContinueToUse();
    }
}
