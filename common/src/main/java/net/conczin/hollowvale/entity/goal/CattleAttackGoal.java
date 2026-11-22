package net.conczin.hollowvale.entity.goal;

import net.conczin.hollowvale.Sounds;
import net.conczin.hollowvale.entity.UndeadHighlandCattle;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Charges whoever hurt it, with an ender skull scream, but only while it is above half health.
 */
public class CattleAttackGoal extends MeleeAttackGoal {
    private static final float SCREAM_PITCH = 1.5f;

    private final UndeadHighlandCattle cattle;

    public CattleAttackGoal(UndeadHighlandCattle cattle, double speed) {
        super(cattle, speed, true);
        this.cattle = cattle;
    }

    @Override
    public boolean canUse() {
        return cattle.isHealthy() && !cattle.isStanding() && super.canUse();
    }

    @Override
    public void start() {
        super.start();
        cattle.playSound(Sounds.ENDER_SKULL_SCREAM, 1.0f, SCREAM_PITCH);
    }

    @Override
    public boolean canContinueToUse() {
        return cattle.isHealthy() && !cattle.isStanding() && super.canContinueToUse();
    }
}
