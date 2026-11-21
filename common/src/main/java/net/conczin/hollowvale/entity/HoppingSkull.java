package net.conczin.hollowvale.entity;

import net.conczin.hollowvale.Sounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Lies on the floor like any other skull until a player comes close or it takes damage. Then it lights its fuse,
 * hops after the player in quick succession and explodes once the fuse sound ends.
 */
public class HoppingSkull extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(HoppingSkull.class, EntityDataSerializers.INT);
    private static final int DORMANT = -1;
    private static final int FUSE_TICKS = 72;
    private static final double WAKE_RANGE = 5.0;
    private static final int GROUND_TICKS = 3;
    private static final float HOP_POWER = 0.3f;
    private static final double HOP_SPEED = 0.3;
    private static final float HOP_JITTER_DEGREES = 25.0f;
    private static final float EXPLOSION_POWER = 1.0f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int landedTick;
    private boolean wasOnGround;

    public HoppingSkull(EntityType<? extends HoppingSkull> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(FUSE, DORMANT);
    }

    public boolean isFused() {
        return entityData.get(FUSE) != DORMANT;
    }

    private void lightFuse(@Nullable LivingEntity target) {
        if (isFused()) {
            return;
        }
        setTarget(target);
        entityData.set(FUSE, FUSE_TICKS);
        playSound(Sounds.HOPPING_SKULL_FUSE);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!isFused()) {
            LivingEntity player = level().getNearestPlayer(TargetingConditions.forCombat().range(WAKE_RANGE), this);
            if (player != null) {
                lightFuse(player);
            }
            return;
        }

        int fuse = entityData.get(FUSE) - 1;
        entityData.set(FUSE, fuse);
        if (fuse <= 0) {
            explode();
            return;
        }

        if (onGround() && !wasOnGround) {
            landedTick = tickCount;
        }
        wasOnGround = onGround();

        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && onGround() && tickCount - landedTick >= GROUND_TICKS) {
            hopTowards(target);
        }
    }

    private void hopTowards(LivingEntity target) {
        Vec3 direction = target.position().subtract(position()).multiply(1.0, 0.0, 1.0).normalize()
                .yRot((float) (Mth.clamp(random.nextGaussian(), -2.0, 2.0) * HOP_JITTER_DEGREES * Mth.DEG_TO_RAD));
        setYRot((float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0f);
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        jumpFromGround();
        setDeltaMovement(getDeltaMovement().add(direction.scale(HOP_SPEED)));
    }

    private void explode() {
        level().explode(
                this,
                Explosion.getDefaultDamageSource(level(), this),
                null,
                getX(),
                getY(),
                getZ(),
                EXPLOSION_POWER,
                false,
                Level.ExplosionInteraction.NONE,
                ParticleTypes.EXPLOSION,
                ParticleTypes.EXPLOSION,
                SoundEvents.GENERIC_EXPLODE
        );

        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(0.5), getZ(), 4, 0.5, 0.3, 0.5, 0.0);
            server.sendParticles(ParticleTypes.FLAME, getX(), getY(0.5), getZ(), 40, 0.4, 0.3, 0.4, 0.1);
            server.sendParticles(ParticleTypes.LAVA, getX(), getY(0.5), getZ(), 8, 0.2, 0.1, 0.2, 0.0);
            server.sendParticles(ParticleTypes.SMOKE, getX(), getY(0.5), getZ(), 50, 0.5, 0.3, 0.5, 0.06);
            server.sendParticles(ParticleTypes.DRAGON_BREATH, getX(), getY(0.5), getZ(), 30, 0.5, 0.3, 0.5, 0.05);
        }

        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
        cloud.setOwner(this);
        cloud.setParticle(ParticleTypes.DRAGON_BREATH);
        cloud.setRadius(2.0f);
        cloud.setDuration(40);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        level().addFreshEntity(cloud);

        // Removed rather than killed, so there is no loot.
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && isAlive()) {
            LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : level().getNearestPlayer(this, 16.0);
            lightFuse(attacker);
        }
        return hurt;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && isFused()) {
            level().addParticle(ParticleTypes.SMOKE, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0.0, 0.02, 0.0);
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.FLAME, getRandomX(0.4), getY(0.8), getRandomZ(0.4), 0.0, 0.01, 0.0);
            }
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        // A dormant skull never turns, so without this every one would face the same way.
        float yaw = random.nextFloat() * 360.0f;
        setYRot(yaw);
        yRotO = yaw;
        yBodyRot = yaw;
        yBodyRotO = yaw;
        yHeadRot = yaw;
        yHeadRotO = yaw;
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    protected float getJumpPower() {
        return HOP_POWER * getBlockJumpFactor() + getJumpBoostPower();
    }

    @Override
    public void jumpFromGround() {
        super.jumpFromGround();
        playSound(Sounds.HOPPING_SKULL_HOP);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("Fuse", entityData.get(FUSE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("Fuse")) {
            entityData.set(FUSE, tag.getInt("Fuse"));
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return isFused() ? Sounds.HOPPING_SKULL_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return Sounds.HOPPING_SKULL_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Sounds.HOPPING_SKULL_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
