package net.conczin.hollowvale.item;

import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * Turns clicked dirt into ender infused grass, and clicking infused grass spreads the patch outwards.
 */
public class EnderMarrowItem extends Item {
    private static final TagKey<EntityType<?>> HEALABLE = TagKey.create(Registries.ENTITY_TYPE, Common.locate("marrow_healable"));
    private static final float HEAL = 10.0f;
    private static final int MAX_BLOCKS = 5;
    private static final int MAX_PATCH_SIZE = 1024;
    private static final int SPREAD_RANGE = 3;

    public EnderMarrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!target.is(HEALABLE) || target.getHealth() >= target.getMaxHealth()) {
            return InteractionResult.PASS;
        }
        if (target.level() instanceof ServerLevel level) {
            target.heal(HEAL);
            stack.consume(1, player);
            level.playSound(null, target.blockPosition(), SoundEvents.BONE_MEAL_USE, SoundSource.NEUTRAL, 1.0f, 1.0f);
            level.sendParticles(ParticleTypes.HEART, target.getX(), target.getY(0.8), target.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        boolean clickedGrass = level.getBlockState(clicked).is(Blocks.ENDER_INFUSED_GRASS);
        if (!clickedGrass && !isSpreadable(level, clicked)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        List<BlockPos> targets = new ArrayList<>();
        if (!clickedGrass) {
            targets.add(clicked);
        }
        List<BlockPos> frontier = frontier(level, clicked);
        Collections.shuffle(frontier, new Random(level.getRandom().nextLong()));
        targets.addAll(frontier.subList(0, Math.min(MAX_BLOCKS - targets.size(), frontier.size())));

        if (targets.isEmpty()) {
            return InteractionResult.PASS;
        }
        for (BlockPos pos : targets) {
            level.setBlockAndUpdate(pos, Blocks.ENDER_INFUSED_GRASS.defaultBlockState());
            level.levelEvent(1505, pos.above(), 0);
        }
        level.playSound(null, clicked, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0f, 0.8f);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }

    private static List<BlockPos> frontier(Level level, BlockPos start) {
        Set<BlockPos> patch = new HashSet<>(List.of(start));
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(List.of(start));
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (patch.size() < MAX_PATCH_SIZE && !patch.contains(near) && level.getBlockState(near).is(Blocks.ENDER_INFUSED_GRASS)) {
                    patch.add(near.immutable());
                    queue.add(near.immutable());
                }
            }
        }

        Set<BlockPos> candidates = new HashSet<>();
        for (BlockPos pos : patch) {
            for (BlockPos near : BlockPos.betweenClosed(pos.offset(-SPREAD_RANGE, -1, -SPREAD_RANGE), pos.offset(SPREAD_RANGE, 1, SPREAD_RANGE))) {
                if (isSpreadable(level, near)) {
                    candidates.add(near.immutable());
                }
            }
        }
        candidates.remove(start);
        return new ArrayList<>(candidates);
    }

    private static boolean isSpreadable(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.DIRT) && level.getBlockState(pos.above()).isAir();
    }
}
