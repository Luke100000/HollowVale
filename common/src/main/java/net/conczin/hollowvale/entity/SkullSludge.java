package net.conczin.hollowvale.entity;

import net.conczin.hollowvale.Sounds;
import net.conczin.hollowvale.entity.goal.SpitGoal;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SkullSludge extends Monster implements GeoEntity {
    private static final int SPLIT_COUNT = 3;
    private static final double SPLIT_SPEED = 0.5;
    private static final double SPLIT_LIFT = 0.35;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.skull_sludge.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SkullSludge(EntityType<? extends SkullSludge> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new SpitGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason == RemovalReason.KILLED && isDeadOrDying()) {
            splitIntoBoneSludges();
        }
        super.remove(reason);
    }

    /**
     * Shoots the bone sludges apart at 120° to each other, one straight away from the killer and two to its sides.
     */
    private void splitIntoBoneSludges() {
        double away = awayAngle();
        for (int i = 0; i < SPLIT_COUNT; i++) {
            BoneSludge sludge = Entities.BONE_SLUDGE.create(level());
            if (sludge == null) {
                continue;
            }
            if (isPersistenceRequired()) {
                sludge.setPersistenceRequired();
            }
            sludge.setNoAi(isNoAi());
            sludge.setInvulnerable(isInvulnerable());

            double angle = away + i * Math.PI * 2.0 / SPLIT_COUNT;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            float yaw = (float) (angle * Mth.RAD_TO_DEG) - 90.0f;
            sludge.moveTo(getX() + dx * 0.6, getY() + 0.5, getZ() + dz * 0.6, yaw, 0.0f);
            sludge.setDeltaMovement(dx * SPLIT_SPEED, SPLIT_LIFT, dz * SPLIT_SPEED);
            level().addFreshEntity(sludge);
        }
    }

    private double awayAngle() {
        LivingEntity killer = getKillCredit();
        if (killer == null) {
            killer = level().getNearestPlayer(this, 16.0);
        }
        if (killer == null) {
            return random.nextDouble() * Math.PI * 2.0;
        }
        return Math.atan2(getZ() - killer.getZ(), getX() - killer.getX());
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            playSound(Sounds.SKULL_SLUDGE_ATTACK);
        }
        return hurt;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Sounds.SKULL_SLUDGE_SQUISH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return Sounds.SKULL_SLUDGE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Sounds.SKULL_SLUDGE_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
