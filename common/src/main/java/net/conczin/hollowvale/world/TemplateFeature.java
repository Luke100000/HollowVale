package net.conczin.hollowvale.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.conczin.hollowvale.Common;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

import java.util.List;

/**
 * Places a random structure template with its bottom layer in the surface block, rotated randomly around its center.
 */
public class TemplateFeature extends Feature<TemplateFeature.Config> {
    public static final TemplateFeature INSTANCE = new TemplateFeature();

    public record Config(List<ResourceLocation> templates, Holder<StructureProcessorList> processors) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.listOf().fieldOf("templates").forGetter(Config::templates),
                StructureProcessorType.LIST_CODEC.fieldOf("processors").forGetter(Config::processors)
        ).apply(instance, Config::new));
    }

    private TemplateFeature() {
        super(Config.CODEC);
    }

    public static void register(Common.RegisterHelper<Feature<?>> helper) {
        helper.register(Common.locate("template"), INSTANCE);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        List<ResourceLocation> templates = context.config().templates();

        StructureTemplate template = level.getLevel().getServer().getStructureManager()                .get(templates.get(random.nextInt(templates.size()))).orElse(null);
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
        context.config().processors().value().list().forEach(settings::addProcessor);

        BlockPos corner = context.origin().below().subtract(pivot.atY(0));
        return template.placeInWorld(level, corner, corner, settings, random, 2);
    }
}
