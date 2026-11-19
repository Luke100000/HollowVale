package net.conczin.hollowvale.entity;

import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.Sounds;
import net.conczin.hollowvale.entity.goal.CircleHostGoal;
import net.conczin.hollowvale.entity.goal.SkyWanderGoal;
import net.conczin.hollowvale.item.Items;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class Fry extends Monster implements GeoEntity {
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.bone_fry.swim");
    private static final double AGGRO_RANGE = 5.0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID hostId;

    public Fry(EntityType<? extends Fry> type, Level level) {
        super(type, level);
        moveControl = new TurningFlyingMoveControl(this, 20, 20.0f, 4.0f);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Cat.class, 6.0f, 1.0, 1.2));
        goalSelector.addGoal(2, new TemptGoal(this, 1.0, stack -> stack.is(net.minecraft.world.item.Items.CHORUS_FRUIT), false));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(4, new CircleHostGoal(this, 0.6));
        goalSelector.addGoal(5, new SkyWanderGoal(this, 0.6, 3.0, 8.0, 1.0, 4.0, 20, 60));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::isPrey));
    }

    private boolean isPrey(LivingEntity entity) {
        return distanceToSqr(entity) <= AGGRO_RANGE * AGGRO_RANGE
               && !entity.getType().is(EntityTypeTags.UNDEAD)
               && !BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals(Common.MOD_ID)
               && !isHoldingChorus(entity);
    }

    private static boolean isHoldingChorus(LivingEntity entity) {
        return entity.isHolding(net.minecraft.world.item.Items.CHORUS_FRUIT);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (getTarget() != null && isHoldingChorus(getTarget())) {
            setTarget(null);
        }
    }

    public void setHost(BoneWhale host) {
        hostId = host.getUUID();
    }

    public boolean isHostedBy(BoneWhale whale) {
        return whale.getUUID().equals(hostId);
    }

    public @Nullable BoneWhale getHost() {
        if (hostId != null && level() instanceof ServerLevel level && level.getEntity(hostId) instanceof BoneWhale whale && whale.isAlive()) {
            return whale;
        }
        return null;
    }

    public void popAway() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.PORTAL, getX(), getY(0.5), getZ(), 16, 0.25, 0.25, 0.25, 0.4);
        }
        playSound(SoundEvents.CHORUS_FRUIT_TELEPORT);
        discard();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isAlive()) {
            return super.mobInteract(player, hand);
        }

        if (stack.is(net.minecraft.world.item.Items.CHORUS_FRUIT)) {
            if (!level().isClientSide) {
                stack.consume(1, player);
                playSound(Sounds.FRY_EAT);
                BoneWhale host = getHost();
                popAway();
                if (host != null) {
                    host.onFryFed(player);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (stack.is(net.minecraft.world.item.Items.BUCKET)) {
            playSound(Sounds.BUCKET_FILL_FRY);
            ItemStack bucket = new ItemStack(Items.FRY_BUCKET);
            Bucketable.saveDefaultDataToBucketTag(this, bucket);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, bucket, false));
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.FILLED_BUCKET.trigger(serverPlayer, bucket);
            }
            discard();
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return Flight.navigation(this, level);
    }

    @Override
    public void travel(Vec3 input) {
        Flight.travel(this, input);
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
        if (hostId != null) {
            tag.putUUID("Host", hostId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        hostId = tag.hasUUID("Host") ? tag.getUUID("Host") : null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Sounds.FRY_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return Sounds.FRY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Sounds.FRY_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, state -> state.setAndContinue(SWIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
