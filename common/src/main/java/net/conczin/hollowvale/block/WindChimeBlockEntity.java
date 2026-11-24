package net.conczin.hollowvale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Exists only so the client can render the swinging rods.
 */
public class WindChimeBlockEntity extends BlockEntity {
    public float swingTime;
    public float lastRenderTime = -1.0f;

    public WindChimeBlockEntity(BlockPos pos, BlockState state) {
        super(Blocks.WIND_CHIME_ENTITY, pos, state);
    }
}
