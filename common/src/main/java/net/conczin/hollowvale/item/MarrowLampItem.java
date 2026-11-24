package net.conczin.hollowvale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class MarrowLampItem extends BlockItem {
    public MarrowLampItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hollowvale.marrow_lamp.repels").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.hollowvale.marrow_lamp.recall").withStyle(ChatFormatting.GRAY));
    }
}
