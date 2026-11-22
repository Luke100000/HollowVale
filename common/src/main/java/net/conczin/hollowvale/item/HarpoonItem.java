package net.conczin.hollowvale.item;

import net.conczin.hollowvale.entity.HarpoonBolt;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

/**
 * A tethered harpoon. Using it fires a line, using it again while hooked yanks once and lets go, and using it in
 * flight or while sneaking cancels the line. It works from either hand, so it can be held next to a sword.
 */
public class HarpoonItem extends Item {
    private static final int DURABILITY = 250;
    private static final int FIRE_COOLDOWN = 6;
    // Matches the custom_model_data override in the item model that switches from the sprite to the 3D model.
    public static final int IN_HAND_MODEL = 1;

    public HarpoonItem() {
        super(new Item.Properties()
                .durability(DURABILITY)
                .rarity(Rarity.UNCOMMON)
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 2.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .build()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            HarpoonBolt line = HarpoonBolt.find(player);
            if (line == null) {
                fire(level, player, hand, stack);
            } else if (!line.isHooked() || player.isSecondaryUseActive()) {
                line.release();
            } else {
                line.pluck(player);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void fire(Level level, Player player, InteractionHand hand, ItemStack stack) {
        int lineLevel = ModEnchantments.level(level, stack, ModEnchantments.LONG_LINE);
        int stringLevel = ModEnchantments.level(level, stack, ModEnchantments.STRONG_STRING);
        level.addFreshEntity(new HarpoonBolt(level, player, hand, lineLevel, stringLevel));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0f, 0.7f);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        player.getCooldowns().addCooldown(this, FIRE_COOLDOWN);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(net.minecraft.world.item.Items.BONE) || super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public int getEnchantmentValue() {
        return 10;
    }
}
