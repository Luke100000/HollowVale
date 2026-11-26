package net.conczin.hollowvale.entity;

import net.minecraft.server.level.ServerLevel;
import net.conczin.hollowvale.entity.goal.*;
import net.conczin.hollowvale.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

public class UndeadHighlandCattle extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> STAND_TICKS = SynchedEntityData.defineId(UndeadHighlandCattle.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RUNNING = SynchedEntityData.defineId(UndeadHighlandCattle.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TILT = SynchedEntityData.defineId(UndeadHighlandCattle.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.undead_highland_cattle.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.undead_highland_cattle.walk");
    private static final RawAnimation STAND = RawAnimation.begin().thenPlay("animation.undead_highland_cattle.stand");

    private static final int STAND_TICKS_TOTAL = 160;
    private static final long DAY_TICKS = 24000L;
    private static final long MIDNIGHT = 18000L;
    private static final long STAND_WINDOW = 100L;
    private static final double STAND_RANGE = 64.0;
    private static final float TILT_SMOOTHING = 0.12f;
    private static final float VOICE_PITCH = 0.8f;
    private static final double COMBAT_SPEED = 1.5;
    private static final float BUCKET_DAMAGE = 2.0f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long lastStandDay = -1L;
    private float tilt;
    private float tiltO;

    public UndeadHighlandCattle(EntityType<? extends UndeadHighlandCattle> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CattleStandGoal(this));
        goalSelector.addGoal(2, new CattleFleeGoal(this, COMBAT_SPEED));
        goalSelector.addGoal(3, new CattleAttackGoal(this, COMBAT_SPEED));
        goalSelector.addGoal(4, new CattleStareGoal(this));
        goalSelector.addGoal(5, new HerdGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(STAND_TICKS, 0);
        builder.define(TILT, 0);
        builder.define(RUNNING, false);
    }

    public boolean isStanding() {
        return entityData.get(STAND_TICKS) > 0;
    }

    public boolean isHealthy() {
        return getHealth() > getMaxHealth() / 2.0f;
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(serverLevel, source, amount);
        if (hurt && !level().isClientSide() && isHealthy() && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            setTarget(attacker);
        }
        return hurt;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).is(net.minecraft.world.item.Items.BUCKET)) {
            return super.mobInteract(player, hand);
        }
        if (level() instanceof ServerLevel serverLevel && hurtServer(serverLevel, damageSources().playerAttack(player), BUCKET_DAMAGE)) {
            playSound(SoundEvents.BUCKET_FILL);
            spawnAtLocation(serverLevel, Items.ENDER_MARROW);
        }
        return InteractionResult.SUCCESS;
    }

    public void setTilt(int direction) {
        entityData.set(TILT, direction);
    }

    public float getTilt(float partialTick) {
        return Mth.lerp(partialTick, tiltO, tilt);
    }

    public void faceTowards(Entity target, float maxStep) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        float wanted = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
        float yaw = getYRot() + Mth.clamp(Mth.wrapDegrees(wanted - getYRot()), -maxStep, maxStep);
        setYRot(yaw);
        yBodyRot = yaw;
    }

    @Override
    public void tick() {
        super.tick();

        tiltO = tilt;
        float target = isStanding() ? 0.0f : entityData.get(TILT);
        tilt += (target - tilt) * TILT_SMOOTHING;
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);

        if (getTarget() != null && !isHealthy()) {
            setTarget(null);
        }

        entityData.set(RUNNING, goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.isRunning() && (wrapped.getGoal() instanceof CattleAttackGoal || wrapped.getGoal() instanceof CattleFleeGoal)));

        int remaining = entityData.get(STAND_TICKS);
        if (remaining > 0) {
            entityData.set(STAND_TICKS, remaining - 1);
        } else if (isFullMoonMidnight()) {
            lastStandDay = level().getDefaultClockTime() / DAY_TICKS;
            entityData.set(STAND_TICKS, STAND_TICKS_TOTAL);
            getNavigation().stop();
        }
    }

    private boolean isFullMoonMidnight() {
        long time = level().getDefaultClockTime();
        long timeOfDay = time % DAY_TICKS;
        return level().dimensionType().hasSkyLight()
               && level().environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.MOON_PHASE, blockPosition()) == net.minecraft.world.level.MoonPhase.FULL_MOON
               && timeOfDay >= MIDNIGHT && timeOfDay < MIDNIGHT + STAND_WINDOW
               && lastStandDay != time / DAY_TICKS
               && level().getNearestPlayer(this, STAND_RANGE) != null;
    }

    public @Nullable Player getStandTarget() {
        return level().getNearestPlayer(this, STAND_RANGE);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SKELETON_STEP, 0.15f, VOICE_PITCH);
    }

    @Override
    public float getVoicePitch() {
        return VOICE_PITCH + (random.nextFloat() - random.nextFloat()) * 0.1f;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        tag.putLong("LastStandDay", lastStandDay);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);

        tag.getLong("LastStandDay").ifPresent(v -> lastStandDay = v);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, state -> {
            boolean running = entityData.get(RUNNING) && !isStanding();
            state.setControllerSpeed(running ? (float) COMBAT_SPEED : 1.0f);
            if (isStanding()) {
                return state.setAndContinue(STAND);
            }
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
