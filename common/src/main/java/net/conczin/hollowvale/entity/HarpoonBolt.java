package net.conczin.hollowvale.entity;

import net.conczin.hollowvale.effect.ModEffects;
import net.conczin.hollowvale.item.HarpoonItem;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The tethered tip of a harpoon. It flies in an arc until it hits something and then keeps the line taut: mobs, items and
 * players are reeled toward the wielder, blocks and heavy mobs pull the wielder toward them.
 */
public class HarpoonBolt extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> HOOKED_ID = SynchedEntityData.defineId(HarpoonBolt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ANCHORED = SynchedEntityData.defineId(HarpoonBolt.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> OFF_HAND = SynchedEntityData.defineId(HarpoonBolt.class, EntityDataSerializers.BOOLEAN);

    private static final float SPEED = 2.5f;
    private static final float LONG_LINE_SPEED_BONUS = 0.5f;
    private static final int MAX_FLIGHT_TICKS = 100;
    private static final double MAX_LINE = 64.0;
    private static final double STRONG_STRING_BONUS = 0.3;
    private static final double SEARCH_RADIUS = 72.0;
    private static final double LOOT_REACH = 0.7;
    private static final float HIT_DAMAGE = 3.0f;
    private static final float PLUCK_DAMAGE = 2.0f;
    private static final double REEL_SPEED = 0.7;
    private static final double PLUCK_SPEED = 1.6;
    private static final double GRAPPLE_ACCELERATION = 0.25;
    private static final double GRAPPLE_MAX_SPEED = 1.1;
    private static final double ARRIVE_DISTANCE = 2.0;
    private static final double GRAPPLE_ARRIVE_DISTANCE = 1.5;
    private static final double HEAVY_VOLUME = 6.0;
    private static final int HOOKED_EFFECT_TICKS = 60;

    // Only known where the hit happened, the client gets the position from the server instead.
    private @Nullable Vec3 anchorPos;
    private float anchorXRot;
    private float anchorYRot;
    private double strength = 1.0;
    private int stringLevel;

    public HarpoonBolt(EntityType<? extends HarpoonBolt> type, Level level) {
        super(type, level);
    }

    public HarpoonBolt(Level level, Player owner, InteractionHand hand, int lineLevel, int stringLevel) {
        this(Entities.HARPOON_BOLT, level);
        strength = 1.0 + STRONG_STRING_BONUS * stringLevel;
        this.stringLevel = stringLevel;
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        entityData.set(OFF_HAND, hand == InteractionHand.OFF_HAND);
        shootFromRotation(owner, owner.getXRot(), owner.getYRot(), 0.0f, SPEED + LONG_LINE_SPEED_BONUS * lineLevel, 0.3f);
    }

    public static @Nullable HarpoonBolt find(Player owner) {
        List<HarpoonBolt> bolts = owner.level().getEntitiesOfClass(HarpoonBolt.class, owner.getBoundingBox().inflate(SEARCH_RADIUS),
                bolt -> bolt.getOwner() == owner);
        return bolts.isEmpty() ? null : bolts.getFirst();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HOOKED_ID, -1);
        builder.define(ANCHORED, false);
        builder.define(OFF_HAND, false);
    }

    public boolean isOffHand() {
        return entityData.get(OFF_HAND);
    }

    public boolean isAnchored() {
        return entityData.get(ANCHORED);
    }

    public boolean isHooked() {
        return isAnchored() || entityData.get(HOOKED_ID) >= 0;
    }

    public @Nullable Entity getHookedEntity() {
        int id = entityData.get(HOOKED_ID);
        return id >= 0 ? level().getEntity(id) : null;
    }

    public Vec3 getHookPoint(float partialTick) {
        Entity hooked = getHookedEntity();
        return hooked != null ? hooked.getPosition(partialTick).add(0.0, hooked.getBbHeight() * 0.5, 0.0) : getPosition(partialTick);
    }

    public InteractionHand getHand() {
        return isOffHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !isHooked() && entity != getOwner() && super.canHitEntity(entity);
    }

    @Override
    public void tick() {
        if (isHooked()) {
            baseTick();
        } else {
            super.tick();
        }
        // The projectile movement carries on with its last velocity after a hit and turns the bolt toward it.
        if (isAnchored() && anchorPos != null) {
            setPos(anchorPos);
            setRot(anchorYRot, anchorXRot);
            xRotO = anchorXRot;
            yRotO = anchorYRot;
        }
        if (!level().isClientSide) {
            tickServer();
        }
    }

    private void tickServer() {
        if (!(getOwner() instanceof Player owner) || isLineBroken(owner)) {
            release();
        } else if (isHooked()) {
            tickHooked(owner);
        } else if (tickCount > MAX_FLIGHT_TICKS) {
            release();
        } else {
            Entity loot = findLoot();
            if (loot != null) {
                hook(loot);
            }
        }
    }

    private boolean isLineBroken(Player owner) {
        return !owner.isAlive() || owner.level() != level() || distanceTo(owner) > MAX_LINE
                || !(owner.getItemInHand(getHand()).getItem() instanceof HarpoonItem);
    }

    // Items and experience cannot be hit like other entities.
    private @Nullable Entity findLoot() {
        var box = getBoundingBox().inflate(LOOT_REACH).expandTowards(getDeltaMovement());
        List<Entity> loot = level().getEntities(this, box, entity -> entity instanceof ItemEntity || entity instanceof ExperienceOrb);
        return loot.isEmpty() ? null : loot.getFirst();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        if (target instanceof LivingEntity living && getOwner() instanceof Player owner) {
            damage(living, owner, HIT_DAMAGE);
        }
        hook(target);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        anchorPos = result.getLocation();
        anchorXRot = getXRot();
        anchorYRot = getYRot();
        setPos(anchorPos);
        setDeltaMovement(Vec3.ZERO);
        entityData.set(ANCHORED, true);
        playSound(SoundEvents.TRIDENT_HIT_GROUND, 1.0f, 1.4f);
    }

    private void hook(Entity target) {
        setDeltaMovement(Vec3.ZERO);
        entityData.set(HOOKED_ID, target.getId());
        playSound(SoundEvents.TRIDENT_HIT, 1.0f, 1.4f);
    }

    private void tickHooked(Player owner) {
        Entity target = getHookedEntity();
        if (target != null) {
            if (!target.isAlive()) {
                release();
                return;
            }
            setPos(target.getX(), target.getY(0.5), target.getZ());
        }
        reel(owner, false);
    }

    /**
     * Using the harpoon while hooked yanks once, hurting what it is hooked to, and lets go.
     */
    public void pluck(Player owner) {
        if (!isHooked()) {
            return;
        }
        Entity target = getHookedEntity();
        if (target instanceof LivingEntity living && !isHeavy(target)) {
            damage(living, owner, PLUCK_DAMAGE + stringLevel);
            applyHooked(living, owner);
        }
        playSound(SoundEvents.TRIDENT_RETURN, 1.0f, 1.2f);
        reel(owner, true);
        release();
    }

    public void release() {
        if (!isRemoved()) {
            playSound(SoundEvents.CHAIN_BREAK, 1.0f, 1.2f);
            discard();
        }
    }

    // Blocks and heavy mobs pull the wielder, everything else is pulled to the wielder.
    private void reel(Player owner, boolean pluck) {
        Entity target = getHookedEntity();
        if (target == null) {
            pullOwner(owner, position(), pluck);
        } else if (isHeavy(target)) {
            pullOwner(owner, center(target), pluck);
        } else {
            pullTarget(owner, target, pluck);
        }
    }

    private void pullTarget(Player owner, Entity target, boolean pluck) {
        Vec3 toOwner = center(owner).subtract(center(target));
        double distance = toOwner.length();
        if (distance < ARRIVE_DISTANCE) {
            if (target instanceof LivingEntity living) {
                applyHooked(living, owner);
            }
            release();
            return;
        }
        double speed = pluck ? PLUCK_SPEED * strength : Math.min(REEL_SPEED * strength, distance * 0.5);
        target.setDeltaMovement(toOwner.normalize().scale(speed).add(0.0, 0.04, 0.0));
        target.hurtMarked = true;
    }

    // Measured from the feet, so the wielder is pulled up onto a ledge instead of against its face.
    private void pullOwner(Player owner, Vec3 anchor, boolean pluck) {
        Vec3 toAnchor = anchor.subtract(owner.position());
        if (toAnchor.length() < GRAPPLE_ARRIVE_DISTANCE) {
            release();
            return;
        }
        Vec3 motion;
        if (pluck) {
            motion = toAnchor.normalize().scale(PLUCK_SPEED * strength);
        } else {
            motion = owner.getDeltaMovement().add(toAnchor.normalize().scale(GRAPPLE_ACCELERATION * strength));
            double maxSpeed = GRAPPLE_MAX_SPEED * strength;
            if (motion.length() > maxSpeed) {
                motion = motion.normalize().scale(maxSpeed);
            }
        }
        owner.setDeltaMovement(motion);
        owner.hurtMarked = true;
        owner.fallDistance = 0.0f;
    }

    private static void applyHooked(LivingEntity target, Player owner) {
        target.addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.HOOKED), HOOKED_EFFECT_TICKS), owner);
    }

    // Goes through the enchantment helpers like a thrown trident does, so Sharpness, Smite and Fire Aspect on the harpoon count.
    private void damage(LivingEntity target, Player owner, float amount) {
        if (isPetOf(target, owner) || !(level() instanceof ServerLevel server)) {
            return;
        }
        ItemStack weapon = owner.getItemInHand(getHand());
        DamageSource source = damageSources().thrown(this, owner);
        if (target.hurt(source, EnchantmentHelper.modifyDamage(server, weapon, target, source, amount))) {
            EnchantmentHelper.doPostAttackEffectsWithItemSource(server, target, source, weapon);
        }
    }

    private static boolean isPetOf(Entity entity, Player owner) {
        return entity instanceof OwnableEntity pet && pet.getOwner() == owner;
    }

    private static boolean isHeavy(Entity entity) {
        return entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight() > HEAVY_VOLUME;
    }

    private static Vec3 center(Entity entity) {
        return entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
    }
}
