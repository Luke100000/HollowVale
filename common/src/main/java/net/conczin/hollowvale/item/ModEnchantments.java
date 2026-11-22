package net.conczin.hollowvale.item;

import net.conczin.hollowvale.Common;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public final class ModEnchantments {
    public static final ResourceKey<Enchantment> LONG_LINE = key("long_line");
    public static final ResourceKey<Enchantment> STRONG_STRING = key("strong_string");

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, Common.locate(name));
    }

    public static int level(Level level, ItemStack stack, ResourceKey<Enchantment> key) {
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key)
                .map(enchantment -> EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack))
                .orElse(0);
    }
}
