package net.conczin.hollowvale.entity.goal;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * A tempt goal with a custom range, and a stop distance that suits large mobs.
 */
public class FarTemptGoal extends TemptGoal {
    private final TargetingConditions conditions;
    private final double stopDistance;

    public FarTemptGoal(PathfinderMob mob, double speed, Predicate<ItemStack> items, double range, double stopDistance) {
        super(mob, speed, items, false);
        this.conditions = TargetingConditions.forNonCombat().range(range).ignoreLineOfSight()
                .selector(entity -> items.test(entity.getMainHandItem()) || items.test(entity.getOffhandItem()));
        this.stopDistance = stopDistance;
    }

    @Override
    public boolean canUse() {
        player = mob.level().getNearestPlayer(conditions, mob);
        return player != null;
    }

    @Override
    public void tick() {
        if (player != null && mob.distanceTo(player) < stopDistance) {
            mob.getLookControl().setLookAt(player, mob.getMaxHeadYRot() + 20, mob.getMaxHeadXRot());
            mob.getNavigation().stop();
        } else {
            super.tick();
        }
    }
}
