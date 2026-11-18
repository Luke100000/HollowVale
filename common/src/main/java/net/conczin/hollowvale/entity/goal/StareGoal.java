package net.conczin.hollowvale.entity.goal;

import net.conczin.hollowvale.entity.EnderSkull;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Freezes in place and stares at the target while the ender skull is alarmed but not yet attacking.
 */
public class StareGoal extends Goal {
    private final EnderSkull skull;

    public StareGoal(EnderSkull skull) {
        this.skull = skull;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return skull.isAlarmed() && !skull.isAttacking() && skull.getTarget() != null;
    }

    @Override
    public void start() {
        skull.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = skull.getTarget();
        if (target != null) {
            skull.getLookControl().setLookAt(target, 30.0f, 30.0f);
        }
    }
}
