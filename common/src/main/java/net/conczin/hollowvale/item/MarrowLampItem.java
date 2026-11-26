package net.conczin.hollowvale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public class MarrowLampItem extends BlockItem {
    public MarrowLampItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.hollowvale.marrow_lamp.repels").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hollowvale.marrow_lamp.recall").withStyle(ChatFormatting.GRAY));
    }
}
