package net.conczin.hollowvale.item;

import net.conczin.hollowvale.Sounds;
import net.conczin.hollowvale.entity.Entities;
import net.conczin.hollowvale.entity.Fry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class FryBucketItem extends Item {
    public FryBucketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (level instanceof ServerLevel serverLevel) {
            Fry fry = Entities.FRY.spawn(serverLevel, stack, player, pos, EntitySpawnReason.BUCKET, true, false);
            if (fry != null) {
                Bucketable.loadDefaultDataFromBucketTag(fry, stack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag());
                fry.setPersistenceRequired();
            }
            level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        }
        level.playSound(player, pos, Sounds.BUCKET_EMPTY_FRY, SoundSource.NEUTRAL, 1.0f, 1.0f);

        if (player != null) {
            ItemStack empty = new ItemStack(net.minecraft.world.item.Items.BUCKET);
            player.setItemInHand(context.getHand(), ItemUtils.createFilledResult(stack, player, empty));
        }
        return InteractionResult.SUCCESS;
    }
}
