package net.conczin.hollowvale.entity;

import net.conczin.hollowvale.Common;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;

final class ScavengerTrades {
    private static final float PRICE_MULTIPLIER = 0.05f;

    private static final ResourceKey<TradeSet> SELLS = ResourceKey.create(Registries.TRADE_SET, Common.locate("scavenger/sells"));

    private ScavengerTrades() {
    }

    static void fill(Scavenger scavenger, MerchantOffers offers) {
        offers.add(buy(net.minecraft.world.item.Items.BONE, 12, 1, 16));
        offers.add(buy(net.minecraft.world.item.Items.SKELETON_SKULL, 1, 2, 12));
        scavenger.addTradeSet(offers, SELLS);
    }

    private static MerchantOffer buy(Item item, int count, int emeralds, int maxUses) {
        return new MerchantOffer(new ItemCost(item, count), new ItemStack(net.minecraft.world.item.Items.EMERALD, emeralds), maxUses, 2, PRICE_MULTIPLIER);
    }
}
