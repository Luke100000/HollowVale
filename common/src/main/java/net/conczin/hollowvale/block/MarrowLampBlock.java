package net.conczin.hollowvale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class MarrowLampBlock extends LanternBlock {
    public static final int RECALL_RANGE = 32;
    public static final int SAFE_RANGE = 12;

    public MarrowLampBlock(Properties properties) {
        super(properties);
    }

    public static Optional<BlockPos> findNearest(Level level, BlockPos pos, int range) {
        if (!(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        return server.getPoiManager().findClosest(type -> type.is(Blocks.MARROW_LAMP_POI), pos, range, PoiManager.Occupancy.ANY);
    }

    public static boolean isInSafeZone(Level level, BlockPos pos) {
        return findNearest(level, pos, SAFE_RANGE).isPresent();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL,
                    pos.getX() + 0.3 + random.nextDouble() * 0.4,
                    pos.getY() + (state.getValue(HANGING) ? 0.2 : 0.3) + random.nextDouble() * 0.3,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                    0.0, 0.02, 0.0);
        }
    }
}
