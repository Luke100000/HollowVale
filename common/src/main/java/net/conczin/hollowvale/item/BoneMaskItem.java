package net.conczin.hollowvale.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import net.conczin.hollowvale.Common;

public class BoneMaskItem extends Item {
    public BoneMaskItem(Properties properties) {
        super(properties
                .durability(165)
                .rarity(Rarity.UNCOMMON)
                .repairable(net.minecraft.world.item.Items.BONE)
                .enchantable(9)
                .component(net.minecraft.core.component.DataComponents.EQUIPPABLE,
                        Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_IRON).build())
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR, new AttributeModifier(Common.locate("armor.helmet"), 2.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                        .build()));
    }

    public static boolean isWornBy(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(Items.BONE_MASK);
    }
}
