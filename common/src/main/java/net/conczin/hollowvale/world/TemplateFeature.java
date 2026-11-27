package net.conczin.hollowvale.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.conczin.hollowvale.Common;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

import java.util.List;

/**
 * Places a random structure template with its bottom layer in the surface block, rotated randomly around its center.
 */
public record TemplateFeature(List<Identifier> templates, Holder<StructureProcessorList> processors) implements Feature {
    public static final MapCodec<TemplateFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.listOf().fieldOf("templates").forGetter(TemplateFeature::templates),
            StructureProcessorType.LIST_CODEC.fieldOf("processors").forGetter(TemplateFeature::processors)
    ).apply(instance, TemplateFeature::new));

    public static void register(Common.RegisterHelper<MapCodec<? extends Feature>> helper) {
        helper.register(Common.locate("template"), CODEC);
    }

    @Override
    public MapCodec<TemplateFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        StructureTemplate template = level.getLevel().getServer().getStructureTemplateManager()
                .get(templates.get(random.nextInt(templates.size()))).orElse(null);
        if (template == null) {
            return false;
        }

        BlockPos pivot = new BlockPos(template.getSize().getX() / 2, 0, template.getSize().getZ() / 2);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(Rotation.getRandom(random))
                .setRotationPivot(pivot)
                .setIgnoreEntities(true)
                .setRandom(random)
                .addProcessor(new BlockIgnoreProcessor(List.of(Blocks.AIR, Blocks.CAVE_AIR)));
        processors.value().list().forEach(settings::addProcessor);

        BlockPos corner = origin.below().subtract(pivot.atY(0));
        return template.placeInWorld(level, corner, corner, settings, random, 2);
    }
}
