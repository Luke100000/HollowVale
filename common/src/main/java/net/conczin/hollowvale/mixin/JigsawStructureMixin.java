package net.conczin.hollowvale.mixin;

import net.conczin.hollowvale.Common;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(JigsawStructure.class)
public abstract class JigsawStructureMixin {
    @Shadow
    @Final
    private Holder<StructureTemplatePool> startPool;

    // Keeps this mod's structures out of lakes by checking the middle of the start chunk for water above the ground.
    @Inject(method = "findGenerationPoint", at = @At("RETURN"), cancellable = true)
    private void hollowvale$skipWater(Structure.GenerationContext context, CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        boolean ours = startPool.unwrapKey().map(key -> key.identifier().getNamespace().equals(Common.MOD_ID)).orElse(false);
        if (!ours || cir.getReturnValue().isEmpty()) {
            return;
        }
        ChunkPos chunk = context.chunkPos();
        ChunkGenerator generator = context.chunkGenerator();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        int surface = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        int ground = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        if (surface > ground) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
