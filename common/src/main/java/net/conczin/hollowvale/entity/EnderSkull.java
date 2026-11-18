package net.conczin.hollowvale.entity;

import net.conczin.hollowvale.Sounds;
import net.conczin.hollowvale.entity.goal.ChargeGoal;
import net.conczin.hollowvale.entity.goal.HoverWanderGoal;
import net.conczin.hollowvale.entity.goal.StareGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Hovers about a block above the ground and wanders. Spotting a player starts an alarm timer that rises while the
 * player stays visible and falls otherwise: back at zero it calms down, past the scream threshold it charges.
 */
public class EnderSkull extends Monster implements GeoEntity {
    private static final int SPOTTED_ALARM = 3 * 20;
    private static final int SCREAM_ALARM = 5 * 20;
    private static final int MAX_ALARM = 10 * 20;
    private static final double HOVER_HEIGHT = 1.0;
    private static final double CHARGE_SPEED = 2.0;

    private static final EntityDataAccessor<Byte> MODE = SynchedEntityData.defineId(EnderSkull.class, EntityDataSerializers.BYTE);
    private static final byte SHAKE_EVENT = 100;
    private static final int SHAKE_TICKS = 15;
    private static final float EYE_SEARCH_DEGREES = 30.0f;
    private static final float AIR_DRAG = 0.91f;
    private static final float CHOMP_RATE = 0.35f;

    public enum Mode {IDLE, STARING, ATTACKING}

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int alarm;
    private boolean attacking;

    // Client-side animation state
    private float jaw;
    private float previousJaw;
    private float eyeYaw;
    private float previousEyeYaw;
    private float eyePitch;
    private float previousEyePitch;
    private float eyeTargetYaw;
    private float eyeTargetPitch;
    private int eyeSearchTicks;
    private int shakeTicks;

    public EnderSkull(EntityType<? extends EnderSkull> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.125);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MODE, (byte) Mode.IDLE.ordinal());
    }

    public Mode getMode() {
        return Mode.values()[entityData.get(MODE)];
    }

    @Override
    public void travel(Vec3 input) {
        if (!isControlledByLocalInstance()) {
            return;
        }
        if (isInWater() || isInLava()) {
            moveRelative(0.02f, input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(isInWater() ? 0.8 : 0.5));
        } else {
            moveRelative(1.0f - AIR_DRAG, input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(AIR_DRAG));
        }
        calculateEntityAnimation(false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        // The skull is all head: it faces where it flies, and while alarmed the whole skull follows the gaze.
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                if (getMode() == Mode.IDLE) {
                    yHeadRot = getYRot();
                }
                yBodyRot = yHeadRot;
            }
        };
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new ChargeGoal(this, CHARGE_SPEED));
        goalSelector.addGoal(2, new StareGoal(this));
        goalSelector.addGoal(3, new HoverWanderGoal(this, 1.0, HOVER_HEIGHT));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (alarm == 0) {
            Player spotted = level().getNearestPlayer(TargetingConditions.forCombat().range(getAttributeValue(Attributes.FOLLOW_RANGE)), this);
            if (spotted != null) {
                setTarget(spotted);
                alarm = SPOTTED_ALARM;
                playSound(Sounds.ENDER_SKULL_AIM);
            }
            return;
        }

        alarm = Mth.clamp(alarm + (canSeeTarget() ? 1 : -1), 0, MAX_ALARM);
        if (!attacking && alarm >= SCREAM_ALARM) {
            attacking = true;
            playSound(Sounds.ENDER_SKULL_SCREAM);
        } else if (alarm == 0) {
            attacking = false;
            setTarget(null);
        }
        Mode mode = attacking ? Mode.ATTACKING : alarm > 0 ? Mode.STARING : Mode.IDLE;
        entityData.set(MODE, (byte) mode.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            tickAnimation();
        }
    }

    private void tickAnimation() {
        previousJaw = jaw;
        previousEyeYaw = eyeYaw;
        previousEyePitch = eyePitch;
        Mode mode = getMode();

        // Closed while flying, open while staring, chomping while attacking.
        float targetJaw = switch (mode) {
            case IDLE -> 0.0f;
            case STARING -> 0.7f;
            case ATTACKING -> Math.abs(Mth.sin(tickCount * CHOMP_RATE));
        };
        jaw += (targetJaw - jaw) * (mode == Mode.ATTACKING ? 0.8f : 0.2f);

        // The chomp closes whenever its phase passes a multiple of pi.
        if (mode == Mode.ATTACKING && Mth.floor(tickCount * CHOMP_RATE / Mth.PI) != Mth.floor((tickCount - 1) * CHOMP_RATE / Mth.PI)) {
            level().playLocalSound(getX(), getY(), getZ(), Sounds.ENDER_SKULL_CHOMP, getSoundSource(), 0.8f, getVoicePitch(), false);
        }

        // The eye darts between random points while searching, and locks forward onto the target otherwise.
        if (mode == Mode.IDLE) {
            if (--eyeSearchTicks <= 0) {
                eyeTargetYaw = (random.nextFloat() * 2.0f - 1.0f) * EYE_SEARCH_DEGREES;
                eyeTargetPitch = (random.nextFloat() * 2.0f - 1.0f) * EYE_SEARCH_DEGREES * 0.6f;
                eyeSearchTicks = 8 + random.nextInt(25);
            }
        } else {
            eyeTargetYaw = 0.0f;
            eyeTargetPitch = getXRot();
        }
        eyeYaw += (eyeTargetYaw - eyeYaw) * 0.4f;
        eyePitch += (eyeTargetPitch - eyePitch) * 0.4f;

        if (shakeTicks > 0) {
            shakeTicks--;
        }

        if (random.nextBoolean()) {
            double eyeY = getY() + (4.0 + getBob(0.0f)) / 16.0;
            level().addParticle(ParticleTypes.REVERSE_PORTAL,
                    getX() + jitter(0.15), eyeY + jitter(0.15), getZ() + jitter(0.15),
                    jitter(0.12), jitter(0.12), jitter(0.12));
        }
    }

    private double jitter(double spread) {
        return (random.nextDouble() - 0.5) * 2.0 * spread;
    }

    public float getJaw(float partialTick) {
        return Mth.lerp(partialTick, previousJaw, jaw);
    }

    public float getEyeYaw(float partialTick) {
        return Mth.lerp(partialTick, previousEyeYaw, eyeYaw);
    }

    public float getEyePitch(float partialTick) {
        return Mth.lerp(partialTick, previousEyePitch, eyePitch);
    }

    public float getShake(float partialTick) {
        return Math.max(0.0f, shakeTicks - partialTick) / SHAKE_TICKS;
    }

    public float getBob(float partialTick) {
        return Mth.sin((tickCount + partialTick) * 0.1f + getId() * 1.3f) * 0.75f;
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        if (!level().isClientSide) {
            level().broadcastEntityEvent(this, SHAKE_EVENT);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == SHAKE_EVENT) {
            shakeTicks = SHAKE_TICKS;
        } else {
            super.handleEntityEvent(id);
        }
    }

    private boolean canSeeTarget() {
        LivingEntity target = getTarget();
        return target != null
               && target.isAlive()
               && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)
               && distanceTo(target) <= getAttributeValue(Attributes.FOLLOW_RANGE)
               && hasLineOfSight(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        // Getting hit skips the staring phase.
        if (hurt && !level().isClientSide && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            setTarget(attacker);
            alarm = Math.max(alarm, SCREAM_ALARM);
        }
        return hurt;
    }

    public boolean isAlarmed() {
        return alarm > 0;
    }

    public boolean isAttacking() {
        return attacking;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            playSound(Sounds.ENDER_SKULL_ATTACK);
        }
        return hurt;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Alarm", alarm);
        tag.putBoolean("Attacking", attacking);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        alarm = tag.getInt("Alarm");
        attacking = tag.getBoolean("Attacking");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Sounds.ENDER_SKULL_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return Sounds.ENDER_SKULL_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Sounds.ENDER_SKULL_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
