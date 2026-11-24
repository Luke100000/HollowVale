package net.conczin.hollowvale.entity.goal;

import net.conczin.hollowvale.entity.Scavenger;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class ScavengerCrossbowGoal extends Goal {
    private static final double TOO_CLOSE = 8.0;
    private static final double TOO_FAR = 14.0;
    private static final double MAX_RANGE = 24.0;
    private static final int SHOT_INTERVAL = 30;
    private static final float ARROW_SPEED = 1.6f;
    private static final float BASE_INACCURACY = 14.0f;
    private static final float INACCURACY_PER_DIFFICULTY = 4.0f;

    private static final double FOOT_RETREAT_SPEED = 1.15;
    private static final double FOOT_CHASE_SPEED = 0.9;
    private static final double MOUNTED_RETREAT_SPEED = 2.6;
    private static final double MOUNTED_CHASE_SPEED = 2.0;

    private final Scavenger scavenger;
    private int cooldown;

    public ScavengerCrossbowGoal(Scavenger scavenger) {
        this.scavenger = scavenger;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = scavenger.getTarget();
        return scavenger.isHostile() && target != null && target.isAlive() && scavenger.getMainHandItem().is(Items.CROSSBOW);
    }

    @Override
    public void start() {
        scavenger.setAggressive(true);
        cooldown = SHOT_INTERVAL / 2;
    }

    @Override
    public void stop() {
        scavenger.setAggressive(false);
        scavenger.setCharge(0);
        scavenger.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = scavenger.getTarget();
        if (target == null) {
            return;
        }
        double distance = scavenger.distanceTo(target);
        boolean visible = scavenger.getSensing().hasLineOfSight(target);
        scavenger.getLookControl().setLookAt(target, 30.0f, 30.0f);

        if (distance < TOO_CLOSE) {
            retreat(target);
        } else if (distance > TOO_FAR || !visible) {
            scavenger.getNavigation().moveTo(target, scavenger.isPassenger() ? MOUNTED_CHASE_SPEED : FOOT_CHASE_SPEED);
        } else {
            scavenger.getNavigation().stop();
        }

        int charge = scavenger.getCharge();
        if (charge > 0) {
            if (charge < Scavenger.CHARGE_TICKS) {
                scavenger.setCharge(charge + 1);
            } else if (visible) {
                shoot(target);
                scavenger.setCharge(0);
                cooldown = SHOT_INTERVAL;
            }
        } else if (--cooldown <= 0 && visible && distance <= MAX_RANGE) {
            scavenger.setCharge(1);
            scavenger.playSound(SoundEvents.CROSSBOW_LOADING_START.value());
        }
    }

    private void retreat(LivingEntity target) {
        Vec3 away = DefaultRandomPos.getPosAway(scavenger, 16, 7, target.position());
        if (away != null) {
            scavenger.getNavigation().moveTo(away.x, away.y, away.z, scavenger.isPassenger() ? MOUNTED_RETREAT_SPEED : FOOT_RETREAT_SPEED);
        }
    }

    private void shoot(LivingEntity target) {
        ItemStack crossbow = scavenger.getMainHandItem();
        AbstractArrow arrow = ProjectileUtil.getMobArrow(scavenger, new ItemStack(Items.ARROW), 1.0f, crossbow);
        double dx = target.getX() - scavenger.getX();
        double dy = target.getY(0.3333) - arrow.getY();
        double dz = target.getZ() - scavenger.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float inaccuracy = BASE_INACCURACY - scavenger.level().getDifficulty().getId() * INACCURACY_PER_DIFFICULTY;
        arrow.shoot(dx, dy + horizontal * 0.2, dz, ARROW_SPEED, inaccuracy);
        scavenger.level().addFreshEntity(arrow);
        scavenger.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0f, 1.0f / (scavenger.getRandom().nextFloat() * 0.4f + 0.8f));
    }
}
