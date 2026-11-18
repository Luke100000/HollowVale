package net.conczin.hollowvale.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class SludgeBall extends ThrowableItemProjectile {
    private static final byte SPLASH_EVENT = 3;

    public SludgeBall(EntityType<? extends SludgeBall> type, Level level) {
        super(type, level);
    }

    public SludgeBall(Level level, LivingEntity owner) {
        super(Entities.SLUDGE_BALL, owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SLIME_BALL;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        result.getEntity().hurt(damageSources().thrown(this, getOwner()), 1.0f);
        if (result.getEntity() instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3 * 20, 1), getEffectSource());
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().broadcastEntityEvent(this, SPLASH_EVENT);
            discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id != SPLASH_EVENT) {
            super.handleEntityEvent(id);
            return;
        }
        for (int i = 0; i < 8; i++) {
            level().addParticle(ParticleTypes.ITEM_SLIME, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
        }
    }
}
