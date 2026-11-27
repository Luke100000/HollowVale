package net.conczin.hollowvale.mixin;

import net.conczin.hollowvale.ModAdvancements;
import net.conczin.hollowvale.block.MarrowLampBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TeleportRandomlyConsumeEffect.class)
public abstract class TeleportRandomlyConsumeEffectMixin {
    // A nearby Marrow Lamp replaces the random destination
    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void hollowvale$teleportToLamp(Level level, ItemStack stack, LivingEntity user, CallbackInfoReturnable<Boolean> cir) {
        BlockPos lamp = MarrowLampBlock.findNearest(level, user.blockPosition(), MarrowLampBlock.RECALL_RANGE).orElse(null);
        if (lamp == null) {
            return;
        }
        if (user.isPassenger()) {
            user.stopRiding();
        }
        Vec3 oldPos = user.position();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos spot = lamp.relative(direction);
            if (!user.randomTeleport(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, true, BlockTags.CONSUMABLE_DOES_NOT_TELEPORT_TO)) {
                continue;
            }
            level.gameEvent(GameEvent.TELEPORT, oldPos, GameEvent.Context.of(user));
            boolean fox = user instanceof Fox;
            SoundEvent sound = fox ? SoundEvents.FOX_TELEPORT : SoundEvents.CHORUS_FRUIT_TELEPORT;
            level.playSound(null, user.getX(), user.getY(), user.getZ(), sound, fox ? SoundSource.NEUTRAL : SoundSource.PLAYERS);
            user.resetFallDistance();
            user.resetCurrentImpulseContext();
            if (user instanceof ServerPlayer player) {
                ModAdvancements.award(player, "marrow_lamp", "recalled");
            }
            cir.setReturnValue(true);
            return;
        }
    }
}
