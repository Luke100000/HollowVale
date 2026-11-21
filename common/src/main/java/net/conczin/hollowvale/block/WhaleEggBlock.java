package net.conczin.hollowvale.block;

import net.conczin.hollowvale.entity.BoneWhale;
import net.conczin.hollowvale.entity.Entities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WhaleEggBlock extends Block {
    public static final IntegerProperty HATCH = BlockStateProperties.HATCH;
    private static final int LAST_STAGE = 2;
    private static final int STAGE_TICKS = 8000;
    private static final int STAGE_JITTER_TICKS = 300;
    private static final double OWNER_RANGE = 32.0;
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public WhaleEggBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HATCH, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HATCH);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, STAGE_TICKS + level.random.nextInt(STAGE_JITTER_TICKS));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int stage = state.getValue(HATCH);
        if (stage < LAST_STAGE) {
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.7f, 0.9f + random.nextFloat() * 0.2f);
            level.setBlock(pos, state.setValue(HATCH, stage + 1), Block.UPDATE_CLIENTS);
        } else {
            hatch(state, level, pos);
        }
    }

    private void hatch(BlockState state, ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 0.7f, 0.9f + level.random.nextFloat() * 0.2f);
        level.removeBlock(pos, false);
        level.levelEvent(2001, pos, Block.getId(state));

        BoneWhale calf = Entities.BONE_WHALE.create(level);
        if (calf == null) {
            return;
        }
        calf.setAge(AgeableMob.BABY_START_AGE);
        calf.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0f, 0.0f);
        Player owner = level.getNearestPlayer(calf, OWNER_RANGE);
        if (owner != null) {
            calf.tame(owner);
        }
        level.addFreshEntity(calf);
    }
}
