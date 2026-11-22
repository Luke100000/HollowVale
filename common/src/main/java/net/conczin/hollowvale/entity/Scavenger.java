package net.conczin.hollowvale.entity;

import net.conczin.hollowvale.entity.goal.ScavengerCrossbowGoal;
import net.conczin.hollowvale.entity.goal.StandForTradeGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scavenger extends AbstractVillager implements GeoEntity {
    private static final EntityDataAccessor<Boolean> HOSTILE = SynchedEntityData.defineId(Scavenger.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(Scavenger.class, EntityDataSerializers.INT);

    public static final int CHARGE_TICKS = 25;
    private static final int KEYCHAIN_PERCENT = 15;
    private static final int LIFETIME_TICKS = 5 * 60 * 20;
    private static final double TRADE_RANGE_SQ = 16.0 * 16.0;
    private static final int MOUNT_REVENGE_TICKS = 20;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int lifetime = LIFETIME_TICKS;

    public Scavenger(EntityType<? extends Scavenger> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ScavengerCrossbowGoal(this));
        goalSelector.addGoal(1, new StandForTradeGoal(this, TRADE_RANGE_SQ));
        goalSelector.addGoal(2, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0f, 1.0f));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0f));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, player -> isHostile()));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HOSTILE, false);
        builder.define(CHARGE, 0);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    public boolean isHostile() {
        return entityData.get(HOSTILE);
    }

    // Derived from the UUID so it needs neither saving nor syncing.
    public boolean hasKeychain() {
        return Math.floorMod(getUUID().getLeastSignificantBits(), 100L) < KEYCHAIN_PERCENT;
    }

    public int getCharge() {
        return entityData.get(CHARGE);
    }

    public void setCharge(int ticks) {
        entityData.set(CHARGE, ticks);
    }

    private void becomeHostile(LivingEntity attacker) {
        entityData.set(HOSTILE, true);
        setTarget(attacker);
        if (getTradingPlayer() instanceof ServerPlayer trading) {
            trading.closeContainer();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && isAlive()) {
            if (source.getEntity() instanceof LivingEntity attacker && attacker != this) {
                becomeHostile(attacker);
            }
            if (isHostile() && getHealth() <= getMaxHealth() / 2.0f) {
                vanish();
            }
        }
        return hurt;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!isHostile() && getVehicle() instanceof LivingEntity mount) {
            LivingEntity attacker = mount.getLastHurtByMob();
            if (attacker != null && mount.tickCount - mount.getLastHurtByMobTimestamp() < MOUNT_REVENGE_TICKS) {
                becomeHostile(attacker);
            }
        }
        if (!isTrading() && --lifetime <= 0) {
            vanish();
        }
    }

    // Eats a chorus fruit and takes the horse along, nothing is dropped.
    private void vanish() {
        if (level() instanceof ServerLevel server) {
            Entity mount = getVehicle();
            poof(server, this);
            if (mount != null) {
                poof(server, mount);
                mount.discard();
            }
        }
        playSound(SoundEvents.GENERIC_EAT);
        playSound(SoundEvents.CHORUS_FRUIT_TELEPORT);
        discard();
    }

    private static void poof(ServerLevel level, Entity entity) {
        level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY(0.5), entity.getZ(), 40, entity.getBbWidth() * 0.5, entity.getBbHeight() * 0.5, entity.getBbWidth() * 0.5, 0.5);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || isTrading() || isHostile() || player.getItemInHand(hand).is(Items.VILLAGER_SPAWN_EGG)) {
            return super.mobInteract(player, hand);
        }
        if (hand == InteractionHand.MAIN_HAND) {
            player.awardStat(Stats.TALKED_TO_VILLAGER);
        }
        if (!level().isClientSide) {
            if (getOffers().isEmpty()) {
                return InteractionResult.CONSUME;
            }
            setTradingPlayer(player);
            openTradingScreen(player, getDisplayName(), 1);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void updateTrades() {
        ScavengerTrades.fill(this, getOffers());
    }

    public void addTradeListings(MerchantOffers offers, VillagerTrades.ItemListing[] listings, int count) {
        addOffersFromItemListings(offers, listings, count);
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (offer.shouldRewardExp()) {
            level().addFreshEntity(new ExperienceOrb(level(), getX(), getY() + 0.5, getZ(), 3 + random.nextInt(4)));
        }
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return isTrading() ? SoundEvents.VILLAGER_TRADE : SoundEvents.PILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PILLAGER_DEATH;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Hostile", isHostile());
        tag.putInt("Lifetime", lifetime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(HOSTILE, tag.getBoolean("Hostile"));
        if (tag.contains("Lifetime")) {
            lifetime = tag.getInt("Lifetime");
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Posed procedurally, see ScavengerModel.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
