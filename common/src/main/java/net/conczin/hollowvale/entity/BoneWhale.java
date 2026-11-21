package net.conczin.hollowvale.entity;

import net.conczin.hollowvale.Sounds;
import net.conczin.hollowvale.client.RiderInput;
import net.conczin.hollowvale.entity.goal.FarTemptGoal;
import net.conczin.hollowvale.entity.goal.SkyWanderGoal;
import net.conczin.hollowvale.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class BoneWhale extends TamableAnimal implements GeoEntity, Saddleable {
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.bone_whale.death");
    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(BoneWhale.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> YOUNG = SynchedEntityData.defineId(BoneWhale.class, EntityDataSerializers.BOOLEAN);
    private static final float YOUNG_SCALE = 0.9f;
    private static final float SWIM_CYCLE_TICKS = 3 * 20;

    private static final int MAX_FRY = 5;
    private static final int FRY_INTERVAL = 5 * 20;
    private static final double FRY_RANGE = 32.0;
    private static final int FRY_TO_TAME = 5;

    private static final float RIDE_SPEED = 0.4f;
    private static final float SPRINT_FACTOR = 1.5f;
    private static final float RIDDEN_TURN = 6.0f;

    // Saddle bone pivot in the adult model, in blocks.
    private static final double SADDLE_Y = 17.0 / 16.0;
    private static final double SADDLE_FORWARD = -1.0 / 16.0;
    private static final double SEAT_HEIGHT = 2.0 / 16.0;

    // Holding the use key repeats the interaction, which would dismount right after mounting.
    private static final int DISMOUNT_DELAY = 10;

    // Below the 32 ticks after which the rider would eat it.
    private static final int CHORUS_FEED_TICKS = 16;
    private static final int TELEPORT_COOLDOWN = 5 * 20;
    private static final int DEATH_TICKS = 48;

    // Overworld coordinates are multiplied by this in the End, the reverse of the Nether.
    private static final double END_SCALE = 8.0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final YawDamper riddenTurn = new YawDamper(RIDDEN_TURN, 0.6f);
    private int fryFed;
    private int teleportCooldown;
    private int mountedTick;

    private float turnRate;
    private float previousTurnRate;
    private float swimSpeed = 1.0f;
    private float swimPhase;
    private float previousSwimPhase;
    private Vec3 seatOffset = Vec3.ZERO;

    public BoneWhale(EntityType<? extends BoneWhale> type, Level level) {
        super(type, level);
        moveControl = new TurningFlyingMoveControl(this, 3, 3.0f, 0.3f);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.1)
                .add(Attributes.FLYING_SPEED, 0.1)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SADDLED, false);
        builder.define(YOUNG, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new FarTemptGoal(this, 1.2, stack -> stack.is(Items.ENDER_MARROW) || stack.is(net.minecraft.world.item.Items.CHORUS_FRUIT), 48.0, 5.0));
        goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.5, 16.0f, 6.0f));
        goalSelector.addGoal(4, new SkyWanderGoal(this, 1.0, 16.0, 48.0, 5.0, 20.0, 40, 200) {
            @Override
            public boolean canUse() {
                return !isTame() && super.canUse();
            }
        });
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!isTame() && !isBaby() && tickCount % FRY_INTERVAL == 0 && level().getDifficulty() != Difficulty.PEACEFUL) {
            spawnFry();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            previousTurnRate = turnRate;
            float moved = (float) new Vec3(getX() - xo, getY() - yo, getZ() - zo).length();
            swimSpeed += (Mth.clamp(0.75f + moved * 4.0f, 0.75f, 2.5f) - swimSpeed) * 0.1f;
            if (swimPhase > Mth.TWO_PI) {
                swimPhase -= Mth.TWO_PI;
            }
            previousSwimPhase = swimPhase;
            swimPhase += swimSpeed * Mth.TWO_PI / SWIM_CYCLE_TICKS;
            turnRate += (Mth.wrapDegrees(yBodyRot - yBodyRotO) - turnRate) * 0.1f;
        } else {
            boolean young = isBaby() && getAge() >= AgeableMob.BABY_START_AGE / 2;
            if (young != isYoung()) {
                entityData.set(YOUNG, young);
            }
            if (teleportCooldown > 0) {
                teleportCooldown--;
            }
            checkChorusFeeding();
        }
    }

    private List<Fry> getFry() {
        return level().getEntitiesOfClass(Fry.class, getBoundingBox().inflate(FRY_RANGE), fry -> fry.isHostedBy(this));
    }

    private void spawnFry() {
        if (getFry().size() >= MAX_FRY) {
            return;
        }
        Fry fry = Entities.FRY.create(level());
        if (fry == null) {
            return;
        }
        fry.setHost(this);
        fry.moveTo(getX() + random.nextGaussian(), getY(0.5), getZ() + random.nextGaussian(), random.nextFloat() * 360.0f, 0.0f);
        level().addFreshEntity(fry);
    }

    public void onFryFed(Player player) {
        if (isTame() || ++fryFed < FRY_TO_TAME) {
            return;
        }
        tame(player);
        getFry().forEach(Fry::popAway);
        level().broadcastEntityEvent(this, (byte) 7);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && hasPassenger(source.getEntity())) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            getFry().forEach(fry -> fry.setTarget(attacker));
        }
        return hurt;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hasPassenger(player)) {
            // Chorus fruit passes through to eating, which feeds the whale.
            if (stack.is(net.minecraft.world.item.Items.CHORUS_FRUIT) || tickCount - mountedTick < DISMOUNT_DELAY) {
                return InteractionResult.PASS;
            }
            if (!level().isClientSide) {
                player.stopRiding();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!isTame() || !isOwnedBy(player)) {
            return super.mobInteract(player, hand);
        }

        if (stack.is(Items.ENDER_MARROW) && (getHealth() < getMaxHealth() || isBaby())) {
            if (!level().isClientSide) {
                heal(10.0f);
                if (isBaby()) {
                    ageUp(AgeableMob.getSpeedUpSecondsWhenFeeding(-getAge()), true);
                }
                stack.consume(1, player);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide) {
                setOrderedToSit(!isOrderedToSit());
                getNavigation().stop();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (isSaddled() && !isVehicle() && !stack.is(net.minecraft.world.item.Items.SADDLE)) {
            if (!level().isClientSide) {
                setOrderedToSit(false);
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    public boolean isYoung() {
        return entityData.get(YOUNG);
    }

    @Override
    public float getAgeScale() {
        return isYoung() ? YOUNG_SCALE : super.getAgeScale();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (YOUNG.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public boolean isSaddleable() {
        return isAlive() && isTame() && !isBaby();
    }

    @Override
    public void equipSaddle(ItemStack stack, @Nullable SoundSource source) {
        entityData.set(SADDLED, true);
        if (source != null) {
            level().playSound(null, this, getSaddleSoundEvent(), source, 0.5f, 1.0f);
        }
    }

    @Override
    public boolean isSaddled() {
        return entityData.get(SADDLED);
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        if (isSaddled()) {
            spawnAtLocation(net.minecraft.world.item.Items.SADDLE);
        }
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return isSaddled() && getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger();
    }

    public float getSwimPhase(float partialTick) {
        return Mth.lerp(partialTick, previousSwimPhase, swimPhase);
    }

    public float getTurnRate(float partialTick) {
        return Mth.lerp(partialTick, previousTurnRate, turnRate);
    }

    public void setSeatOffset(Vec3 offset) {
        seatOffset = offset;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        // The model faces -z, so its offset is turned around. Only the client animates it.
        Vec3 seat = new Vec3(-seatOffset.x, SADDLE_Y + SEAT_HEIGHT + seatOffset.y, SADDLE_FORWARD - seatOffset.z);
        return seat.yRot(-yBodyRot * Mth.DEG_TO_RAD);
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        mountedTick = tickCount;
    }

    @Override
    public boolean canSprint() {
        return true;
    }

    @Override
    protected void tickRidden(Player rider, Vec3 input) {
        super.tickRidden(rider, input);
        setRot(riddenTurn.turn(getYRot(), rider.getYRot()), 0.0f);
        yBodyRot = yHeadRot = getYRot();
    }

    @Override
    public void lerpHeadTo(float yaw, int steps) {
        // The rider's client turns the whale itself; stale server head yaw would drag the body back.
        if (!isControlledByLocalInstance()) {
            super.lerpHeadTo(yaw, steps);
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (isAlive() && getControllingPassenger() instanceof Player rider) {
            if (isControlledByLocalInstance()) {
                travelRidden(rider);
            }
            return;
        }
        Flight.travel(this, input);
    }

    private void travelRidden(Player rider) {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 thrust = forward.scale(rider.zza);
        if (level().isClientSide) {
            if (RiderInput.isAscending(rider) && getY() < level().getMaxBuildHeight()) {
                thrust = thrust.add(0.0, 1.0, 0.0);
            }
            if (RiderInput.isDescending(rider)) {
                thrust = thrust.add(0.0, -1.0, 0.0);
            }
        }
        if (thrust.lengthSqr() > 1.0) {
            thrust = thrust.normalize();
        }
        Flight.thrust(this, thrust, RIDE_SPEED * (rider.isSprinting() ? SPRINT_FACTOR : 1.0f));
    }

    // Intercepts the rider eating chorus fruit before it would teleport them off the whale.
    private void checkChorusFeeding() {
        if (!(getControllingPassenger() instanceof ServerPlayer rider) || !rider.isUsingItem()) {
            return;
        }
        ItemStack stack = rider.getUseItem();
        if (!stack.is(net.minecraft.world.item.Items.CHORUS_FRUIT) || rider.getTicksUsingItem() < CHORUS_FEED_TICKS) {
            return;
        }
        rider.stopUsingItem();
        if (teleportCooldown == 0 && teleportAcrossDimensions()) {
            stack.consume(1, rider);
        }
    }

    private boolean teleportAcrossDimensions() {
        if (!(level() instanceof ServerLevel from)) {
            return false;
        }
        ResourceKey<Level> destination;
        double scale;
        if (from.dimension() == Level.OVERWORLD) {
            destination = Level.END;
            scale = END_SCALE;
        } else if (from.dimension() == Level.END) {
            destination = Level.OVERWORLD;
            scale = 1.0 / END_SCALE;
        } else {
            return false;
        }
        ServerLevel to = from.getServer().getLevel(destination);
        if (to == null) {
            return false;
        }

        Vec3 target = findTeleportTarget(to, scale);
        teleportEffects(from, position());
        teleportCooldown = TELEPORT_COOLDOWN;
        changeDimension(new DimensionTransition(to, target, Vec3.ZERO, getYRot(), getXRot(), whale -> teleportEffects(to, whale.position())));
        return true;
    }

    private Vec3 findTeleportTarget(ServerLevel to, double scale) {
        WorldBorder border = to.getWorldBorder();
        double x = Mth.clamp(getX() * scale, border.getMinX() + 16.0, border.getMaxX() - 16.0);
        double z = Mth.clamp(getZ() * scale, border.getMinZ() + 16.0, border.getMaxZ() - 16.0);
        double y = Mth.clamp(getY(), to.getMinBuildHeight() + 8.0, to.getMaxBuildHeight() - 8.0);

        AABB box = getDimensions(getPose()).makeBoundingBox(x, y, z).expandTowards(0.0, 2.0, 0.0);
        for (int step = 0; step < 64 && !to.noCollision(box); step++) {
            box = box.move(0.0, 1.0, 0.0);
            y++;
        }
        return new Vec3(x, y, z);
    }

    private void teleportEffects(ServerLevel level, Vec3 position) {
        level.sendParticles(ParticleTypes.PORTAL, position.x, position.y + getBbHeight() * 0.5, position.z, 80, getBbWidth() * 0.5, getBbHeight() * 0.5, getBbWidth() * 0.5, 0.5);
        level.playSound(null, position.x, position.y, position.z, Sounds.BONE_WHALE_TELEPORT, getSoundSource(), 2.0f, 1.0f);
    }

    @Override
    public boolean shouldTryTeleportToOwner() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return Flight.navigation(this, level);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= DEATH_TICKS && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Saddled", isSaddled());
        tag.putInt("FryFed", fryFed);
        tag.putInt("TeleportCooldown", teleportCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(SADDLED, tag.getBoolean("Saddled"));
        fryFed = tag.getInt("FryFed");
        teleportCooldown = tag.getInt("TeleportCooldown");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Sounds.BONE_WHALE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return Sounds.BONE_WHALE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Sounds.BONE_WHALE_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Swimming is procedural, see BoneWhaleModel.
        controllers.add(new AnimationController<>(this, state -> isDeadOrDying() ? state.setAndContinue(DEATH) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
