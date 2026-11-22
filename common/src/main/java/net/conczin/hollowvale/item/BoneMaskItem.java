package net.conczin.hollowvale.item;

import net.conczin.hollowvale.Common;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;

public class BoneMaskItem extends ArmorItem {
    private static final ArmorMaterial MATERIAL = new ArmorMaterial(
            Map.of(Type.HELMET, 2),
            9,
            SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(net.minecraft.world.item.Items.BONE),
            List.of(new ArmorMaterial.Layer(Common.locate("bone_mask"))),
            0.0f,
            0.0f);

    public BoneMaskItem() {
        super(Holder.direct(MATERIAL), Type.HELMET, new Properties().durability(Type.HELMET.getDurability(15)).rarity(Rarity.UNCOMMON));
    }

    public static boolean isWornBy(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(Items.BONE_MASK);
    }
}
