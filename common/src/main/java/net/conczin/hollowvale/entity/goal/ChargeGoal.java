package net.conczin.hollowvale.entity.goal;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.conczin.hollowvale.entity.EnderSkull;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class ChargeGoal extends Goal {
    private static final int ATTACK_INTERVAL = 20;
    private static final int REPATH_INTERVAL = 5;

    private final EnderSkull skull;
    private final double speed;
    private int nextAttackTick;

    public ChargeGoal(EnderSkull skull, double speed) {
        this.skull = skull;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = skull.getTarget();
        return skull.isAttacking() && target != null && target.isAlive();
    }

    @Override
    public void stop() {
        skull.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = skull.getTarget();
        if (target == null) {
            return;
        }
        skull.getLookControl().setLookAt(target, 30.0f, 30.0f);
        if (skull.tickCount % REPATH_INTERVAL == 0) {
            skull.getNavigation().moveTo(target, speed);
        }

        if (skull.tickCount >= nextAttackTick && skull.isWithinMeleeAttackRange(target) && skull.hasLineOfSight(target)) {
            nextAttackTick = skull.tickCount + ATTACK_INTERVAL;
            skull.swing(InteractionHand.MAIN_HAND);
            skull.doHurtTarget((ServerLevel) skull.level(), target);
        }
    }
}
