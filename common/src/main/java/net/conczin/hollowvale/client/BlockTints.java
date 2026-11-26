package net.conczin.hollowvale.client;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class BlockTints {
    private BlockTints() {
    }

    public static final List<BlockTintSource> GRASS = List.of(new BlockTintSource() {
        @Override
        public int color(BlockState state) {
            return GrassColor.getDefaultColor();
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            return BiomeColors.getAverageGrassColor(level, pos);
        }
    });
}
