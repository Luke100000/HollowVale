package net.conczin.hollowvale.entity.goal;

import net.conczin.hollowvale.Sounds;
import net.conczin.hollowvale.entity.SludgeBall;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Throws sludge balls while the target is out of melee reach. Claims no goal flags, so it runs alongside the chase.
 */
public class SpitGoal extends Goal {
    private static final double MIN_RANGE = 4.0;
    private static final double MAX_RANGE = 16.0;
    private static final int INTERVAL = 60;

    private final Mob mob;
    private int cooldown;

    public SpitGoal(Mob mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distance = mob.distanceToSqr(target);
        return distance > MIN_RANGE * MIN_RANGE && distance < MAX_RANGE * MAX_RANGE;
    }

    @Override
    public void start() {
        cooldown = INTERVAL / 2;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null || --cooldown > 0 || !mob.hasLineOfSight(target)) {
            return;
        }
        cooldown = INTERVAL + mob.getRandom().nextInt(20);

        SludgeBall ball = new SludgeBall(mob.level(), mob);
        double dx = target.getX() - mob.getX();
        double dy = target.getY(0.33) - ball.getY();
        double dz = target.getZ() - mob.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        ball.shoot(dx, dy + horizontal * 0.2, dz, 1.2f, 6.0f);
        mob.playSound(Sounds.SKULL_SLUDGE_SPIT);
        mob.level().addFreshEntity(ball);
    }
}
