package net.conczin.hollowvale.block;

import net.conczin.hollowvale.Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class WindChimeBlock extends Block implements EntityBlock {
    public static final IntegerProperty ROD = IntegerProperty.create("rod", 0, 4);
    public static final int RODS = 4;

    private static final VoxelShape SHAPE = Block.box(4.0, 1.0, 4.0, 12.0, 16.0, 12.0);

    // Root, minor third, fourth and fifth.
    private static final float VOLUME = 0.5f;
    private static final float VOLUME_JITTER = 0.1f;
    private static final float PITCH_JITTER = 0.02f;
    private static final float[] PITCHES = {1.0f, 1.189f, 1.335f, 1.498f};

    private static final int CALM_CHANCE = 6;
    private static final int WINDY_CHANCE = 3;

    public WindChimeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ROD, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROD);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WindChimeBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.above(), Direction.DOWN);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == Direction.UP && !state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int chance = level.isRaining() || level.isThundering() ? WINDY_CHANCE : CALM_CHANCE;
        if (random.nextInt(chance) == 0) {
            float pitch = PITCHES[random.nextInt(PITCHES.length)] * (1.0f + (random.nextFloat() - 0.5f) * 2.0f * PITCH_JITTER);
            float volume = VOLUME + (random.nextFloat() - 0.5f) * 2.0f * VOLUME_JITTER;
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, Sounds.WIND_CHIME, SoundSource.BLOCKS, volume, pitch, false);
        }
    }
}
