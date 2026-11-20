package net.conczin.hollowvale.block;

import com.google.common.collect.ImmutableSet;
import net.conczin.hollowvale.Common;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

public final class Blocks {
    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();

    public static final Block BONE_BRICKS = register("bone_bricks", new Block(boneProperties()));
    public static final Block BONE_BRICK_STAIRS = register("bone_brick_stairs", new ModStairBlock(BONE_BRICKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(BONE_BRICKS)));
    public static final Block BONE_BRICK_SLAB = register("bone_brick_slab", new SlabBlock(BlockBehaviour.Properties.ofFullCopy(BONE_BRICKS)));
    public static final Block BONE_BRICK_WALL = register("bone_brick_wall", new WallBlock(BlockBehaviour.Properties.ofFullCopy(BONE_BRICKS)));
    public static final Block CRACKED_BONE_BRICKS = register("cracked_bone_bricks", new Block(boneProperties()));
    public static final Block CHISELED_BONE_BRICKS = register("chiseled_bone_bricks", new Block(boneProperties()));
    public static final Block BONE_PILLAR = register("bone_pillar", new RotatedPillarBlock(boneProperties()));
    public static final Block MARROW_INFUSED_BONE = register("marrow_infused_bone", new Block(boneProperties()));

    public static final Block ENDER_INFUSED_GRASS = register("ender_infused_grass", new EnderInfusedGrassBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.GRASS)
            .randomTicks()
            .strength(0.6f)
            .sound(SoundType.GRASS)));

    public static final Block MARROW_LAMP = register("marrow_lamp", new MarrowLampBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.SAND)
            .forceSolidOn()
            .strength(1.5f)
            .sound(SoundType.BONE_BLOCK)
            .lightLevel(state -> 10)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)));

    public static final Block WIND_CHIME = register("wind_chime", new WindChimeBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.SAND)
            .noCollission()
            .strength(0.3f)
            .sound(SoundType.BONE_BLOCK)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)));

    public static BlockEntityType<WindChimeBlockEntity> WIND_CHIME_ENTITY;

    public static final ResourceKey<PoiType> MARROW_LAMP_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, Common.locate("marrow_lamp"));

    private static BlockBehaviour.Properties boneProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.SAND)
                .instrument(NoteBlockInstrument.XYLOPHONE)
                .requiresCorrectToolForDrops()
                .strength(2.0f, 6.0f)
                .sound(SoundType.BONE_BLOCK);
    }

    private static Block register(String name, Block block) {
        BLOCKS.put(name, block);
        return block;
    }

    public static void registerBlocks(Common.RegisterHelper<Block> helper) {
        BLOCKS.forEach((name, block) -> helper.register(Common.locate(name), block));
    }

    public interface BlockEntityTypeFactory {
        BlockEntityType<?> create(BiFunction<BlockPos, BlockState, ? extends BlockEntity> factory, Block... blocks);
    }

    @SuppressWarnings("unchecked")
    public static void registerBlockEntities(Common.RegisterHelper<BlockEntityType<?>> helper, BlockEntityTypeFactory factory) {
        WIND_CHIME_ENTITY = (BlockEntityType<WindChimeBlockEntity>) factory.create(WindChimeBlockEntity::new, WIND_CHIME);
        helper.register(Common.locate("wind_chime"), WIND_CHIME_ENTITY);
    }

    public static PoiType createMarrowLampPoi() {
        return new PoiType(ImmutableSet.copyOf(MARROW_LAMP.getStateDefinition().getPossibleStates()), 0, 1);
    }
}
