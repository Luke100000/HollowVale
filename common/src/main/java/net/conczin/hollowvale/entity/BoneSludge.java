package net.conczin.hollowvale.entity;

import net.conczin.hollowvale.Sounds;
import net.conczin.hollowvale.entity.goal.PounceGoal;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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

public class BoneSludge extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Boolean> WINDING_UP = SynchedEntityData.defineId(BoneSludge.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.bone_sludge.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float squish;
    private float previousSquish;

    public BoneSludge(EntityType<? extends BoneSludge> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WINDING_UP, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new PounceGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            previousSquish = squish;
            squish += ((entityData.get(WINDING_UP) ? 1.0f : 0.0f) - squish) * 0.3f;
        }
    }

    public void setWindingUp(boolean windingUp) {
        entityData.set(WINDING_UP, windingUp);
    }

    public float getSquish(float partialTick) {
        return Mth.lerp(partialTick, previousSquish, squish);
    }

    @Override
    public void jumpFromGround() {
        super.jumpFromGround();
        playSound(Sounds.BONE_SLUDGE_JUMP);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            playSound(Sounds.BONE_SLUDGE_ATTACK);
        }
        return hurt;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Sounds.BONE_SLUDGE_SQUISH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return Sounds.BONE_SLUDGE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Sounds.BONE_SLUDGE_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void updateNoActionTime() {
    }
}
