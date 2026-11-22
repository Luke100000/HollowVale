package net.conczin.hollowvale.entity;

import net.conczin.hollowvale.item.Items;
import net.conczin.hollowvale.item.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.Optional;
import java.util.function.Supplier;

final class ScavengerTrades {
    private static final int SELLS_PER_TRADER = 5;
    private static final float PRICE_MULTIPLIER = 0.05f;
    private static final int VILLAGE_SEARCH_RADIUS = 100;

    private static final VillagerTrades.ItemListing[] SELLS = {
            sale(() -> new ItemStack(Items.ENDER_MARROW, 2), 3, 12, 5),
            sale(() -> new ItemStack(net.minecraft.world.item.Items.CHORUS_FRUIT, 4), 1, 16, 2),
            sale(() -> new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL), 5, 12, 5),
            sale(() -> new ItemStack(Items.MARROW_LAMP), 2, 12, 2),
            book(ModEnchantments.LONG_LINE),
            book(ModEnchantments.STRONG_STRING),
            sale(() -> new ItemStack(Items.BONE_HARPOON), 12, 3, 10),
            sale(() -> new ItemStack(Items.WHALE_EGG), 20, 1, 15),
            ScavengerTrades::villageMap,
    };

    private ScavengerTrades() {
    }

    static void fill(Scavenger scavenger, MerchantOffers offers) {
        offers.add(buy(net.minecraft.world.item.Items.BONE, 12, 1, 16));
        offers.add(buy(net.minecraft.world.item.Items.SKELETON_SKULL, 1, 2, 12));
        scavenger.addTradeListings(offers, SELLS, SELLS_PER_TRADER);
    }

    private static MerchantOffer buy(Item item, int count, int emeralds, int maxUses) {
        return new MerchantOffer(new ItemCost(item, count), new ItemStack(net.minecraft.world.item.Items.EMERALD, emeralds), maxUses, 2, PRICE_MULTIPLIER);
    }

    private static VillagerTrades.ItemListing sale(Supplier<ItemStack> result, int emeralds, int maxUses, int xp) {
        return (Entity trader, RandomSource random) -> new MerchantOffer(new ItemCost(net.minecraft.world.item.Items.EMERALD, emeralds), result.get(), maxUses, xp, PRICE_MULTIPLIER);
    }

    // Vanilla's treasure map trade is not accessible, this does the same for the nearest village.
    private static MerchantOffer villageMap(Entity trader, RandomSource random) {
        if (!(trader.level() instanceof ServerLevel level)) {
            return null;
        }
        BlockPos village = level.findNearestMapStructure(StructureTags.VILLAGE, trader.blockPosition(), VILLAGE_SEARCH_RADIUS, true);
        if (village == null) {
            return null;
        }
        ItemStack map = MapItem.create(level, village.getX(), village.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(level, map);
        MapItemSavedData.addTargetDecoration(map, village, "+", MapDecorationTypes.TARGET_POINT);
        map.set(DataComponents.ITEM_NAME, Component.translatable("filled_map.hollowvale.village"));
        return new MerchantOffer(new ItemCost(net.minecraft.world.item.Items.EMERALD, 4), Optional.of(new ItemCost(net.minecraft.world.item.Items.COMPASS)), map, 3, 8, 0.2f);
    }

    private static VillagerTrades.ItemListing book(ResourceKey<Enchantment> key) {
        return (Entity trader, RandomSource random) -> {
            Holder<Enchantment> enchantment = trader.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
            int level = 1 + random.nextInt(enchantment.value().getMaxLevel());
            ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level));
            return new MerchantOffer(new ItemCost(net.minecraft.world.item.Items.EMERALD, Math.min(64, 2 + random.nextInt(5 + level * 10) + 3 * level)), book, 1, 10, PRICE_MULTIPLIER);
        };
    }
}
