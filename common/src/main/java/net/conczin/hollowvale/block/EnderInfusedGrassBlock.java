package net.conczin.hollowvale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;

/**
 * Grass that doesn't spread, and reverts to dirt when covered.
 */
public class EnderInfusedGrassBlock extends Block {
    public EnderInfusedGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canStayGrass(state, level, pos)) {
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
        }
    }

    // Same rule as vanilla grass.
    private static boolean canStayGrass(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.is(net.minecraft.world.level.block.Blocks.SNOW) && aboveState.getValue(SnowLayerBlock.LAYERS) == 1) {
            return true;
        }
        if (aboveState.getFluidState().getAmount() == 8) {
            return false;
        }
        int lightBlocked = LightEngine.getLightBlockInto(state, aboveState, Direction.UP, aboveState.getLightDampening());
        return lightBlocked < LightEngine.MAX_LEVEL;
    }
}
