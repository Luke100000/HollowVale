package net.conczin.hollowvale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class Structurify {
    private static final int PICK_RANGE = 64;
    private static final int MARGIN = 1;
    private static final int HEADROOM = 2;
    private static final int BLOCK_SEARCH_NORTH = 4;
    private static final int CAVE_DISTANCE_SQ = 3 * 3;
    private static final int MAX_SOLIDS = 4_000_000;
    private static final int MAX_VOLUME = 16_000_000;
    private static final double FAR = 1e18;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("structurify")
                .requires(source -> source.hasPermission(2))
                .executes(context -> run(context.getSource(), null))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(context -> run(context.getSource(), StringArgumentType.getString(context, "name")))));
    }

    private static int run(CommandSourceStack source, @Nullable String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();

        BlockPos start = pickStart(level, player);
        if (start == null) {
            throw fail("Look at a block of the structure");
        }

        BoundingBox region = scanRegion(level, start);
        BoundingBox box = extend(level, region);

        BlockPos blockPos = findStructureBlock(level, box);
        String structureName = resolveName(level, blockPos, name);
        if (structureName == null) {
            throw fail("No structure block found, give a name: /structurify <name>");
        }
        if (blockPos == null) {
            blockPos = placeStructureBlock(level, box);
        }

        int changed = carveAir(level, box);

        StructureBlockEntity entity = (StructureBlockEntity) level.getBlockEntity(blockPos);
        if (entity == null) {
            throw fail("Structure block has no block entity");
        }
        configure(level, blockPos, entity, structureName, box);
        if (!entity.saveStructure()) {
            throw fail("Failed to save structure '" + structureName + "'");
        }

        ResourceLocation id = ResourceLocation.parse(structureName);
        String file = level.getServer().getWorldPath(LevelResource.GENERATED_DIR)
                .resolve(id.getNamespace()).resolve("structures").resolve(id.getPath() + ".nbt").toString();
        String message = "Exported %s (%dx%dx%d, %d air blocks updated) to %s".formatted(
                structureName, box.getXSpan(), box.getYSpan(), box.getZSpan(), changed, file);
        source.sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static CommandSyntaxException fail(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
    }

    @Nullable
    private static BlockPos pickStart(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(PICK_RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || !isSolid(level, hit.getBlockPos())) {
            return null;
        }
        return hit.getBlockPos();
    }

    private static boolean isSolid(ServerLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return !state.isAir()
               && !(state.getBlock() instanceof LiquidBlock)
               && !state.is(Blocks.STRUCTURE_VOID)
               && !state.is(Blocks.STRUCTURE_BLOCK)
               && !state.is(Blocks.GRASS_BLOCK)
               && !state.is(Blocks.DIRT)
               && !state.is(Blocks.BEDROCK);
    }

    private static BoundingBox scanRegion(ServerLevel level, BlockPos start) throws CommandSyntaxException {
        LongSet visited = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        BoundingBox box = new BoundingBox(start);
        visited.add(start.asLong());
        queue.enqueue(start.asLong());

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean seeded = true;
        while (seeded) {
            while (!queue.isEmpty()) {
                long current = queue.dequeueLong();
                int cx = BlockPos.getX(current), cy = BlockPos.getY(current), cz = BlockPos.getZ(current);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            pos.set(cx + dx, cy + dy, cz + dz);
                            if (visited.contains(pos.asLong()) || !isSolid(level, pos)) {
                                continue;
                            }
                            visited.add(pos.asLong());
                            queue.enqueue(pos.asLong());
                            box.encapsulate(pos);
                        }
                    }
                }
                if (visited.size() > MAX_SOLIDS) {
                    throw fail("Region exceeds " + MAX_SOLIDS + " blocks, aborting");
                }
            }

            seeded = false;
            for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                if (!visited.contains(p.asLong()) && isSolid(level, p)) {
                    visited.add(p.asLong());
                    queue.enqueue(p.asLong());
                    seeded = true;
                }
            }
        }
        return box;
    }

    private static BoundingBox extend(ServerLevel level, BoundingBox region) throws CommandSyntaxException {
        BoundingBox box = new BoundingBox(
                region.minX() - MARGIN, region.minY(), region.minZ() - MARGIN,
                region.maxX() + MARGIN, Math.min(region.maxY() + HEADROOM, level.getMaxBuildHeight() - 1), region.maxZ() + MARGIN);
        if ((long) box.getXSpan() * box.getYSpan() * box.getZSpan() > MAX_VOLUME) {
            throw fail("Bounding box too large: " + box.getXSpan() + "x" + box.getYSpan() + "x" + box.getZSpan());
        }
        return box;
    }

    @Nullable
    private static BlockPos findStructureBlock(ServerLevel level, BoundingBox box) {
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ() - BLOCK_SEARCH_NORTH, box.maxX(), box.maxY(), box.maxZ())) {
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.STRUCTURE_BLOCK)) {
                return pos.immutable();
            }
        }
        return null;
    }

    @Nullable
    private static String resolveName(ServerLevel level, @Nullable BlockPos blockPos, @Nullable String name) throws CommandSyntaxException {
        if (name != null) {
            String qualified = name.contains(":") ? name.trim() : Common.MOD_ID + ":" + name.trim();
            if (ResourceLocation.tryParse(qualified) == null) {
                throw fail("Invalid structure name '" + name + "'");
            }
            return qualified;
        }
        if (blockPos != null && level.getBlockEntity(blockPos) instanceof StructureBlockEntity entity && entity.hasStructureName()) {
            return entity.getStructureName();
        }
        return null;
    }

    private static BlockPos placeStructureBlock(ServerLevel level, BoundingBox box) throws CommandSyntaxException {
        int x = (box.minX() + box.maxX()) / 2;
        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int z = box.minZ() - 1; z >= box.minZ() - BLOCK_SEARCH_NORTH; z--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (level.hasChunkAt(pos) && level.getBlockState(pos).canBeReplaced()) {
                    level.setBlock(pos, Blocks.STRUCTURE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                    return pos;
                }
            }
        }
        throw fail("No free spot north of the structure for a structure block");
    }

    private static void configure(ServerLevel level, BlockPos pos, StructureBlockEntity entity, String name, BoundingBox box) {
        entity.setMode(StructureMode.SAVE);
        entity.setStructureName(ResourceLocation.parse(name));
        entity.setStructurePos(new BlockPos(box.minX(), box.minY(), box.minZ()).subtract(pos));
        entity.setStructureSize(new Vec3i(box.getXSpan(), box.getYSpan(), box.getZSpan()));
        entity.setIgnoreEntities(true);
        entity.setChanged();
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
    }

    private static int carveAir(ServerLevel level, BoundingBox box) {
        int nx = box.getXSpan(), ny = box.getYSpan(), nz = box.getZSpan();
        boolean[] solid = new boolean[nx * ny * nz];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 0; y < ny; y++) {
            for (int z = 0; z < nz; z++) {
                for (int x = 0; x < nx; x++) {
                    pos.set(box.minX() + x, box.minY() + y, box.minZ() + z);
                    solid[index(x, y, z, nx, nz)] = isSolid(level, pos);
                }
            }
        }

        double[] distance = new double[solid.length];
        for (int i = 0; i < solid.length; i++) {
            distance[i] = solid[i] ? 0 : FAR;
        }
        transformAxis(distance, nx, 1);
        transformAxis(distance, nz, nx);
        transformAxis(distance, ny, nx * nz);

        for (int z = 0; z < nz; z++) {
            for (int x = 0; x < nx; x++) {
                boolean covered = false;
                for (int y = ny - 1; y >= 0; y--) {
                    int i = index(x, y, z, nx, nz);
                    if (solid[i]) {
                        covered = true;
                    } else if (covered) {
                        distance[i] = 0;
                    }
                }
            }
        }

        int changed = 0;
        for (int y = 0; y < ny; y++) {
            for (int z = 0; z < nz; z++) {
                for (int x = 0; x < nx; x++) {
                    pos.set(box.minX() + x, box.minY() + y, box.minZ() + z);
                    if (!level.hasChunkAt(pos)) {
                        continue;
                    }
                    BlockState current = level.getBlockState(pos);
                    if (!current.is(Blocks.AIR) && !current.is(Blocks.CAVE_AIR)) {
                        continue;
                    }
                    BlockState target = distance[index(x, y, z, nx, nz)] > CAVE_DISTANCE_SQ
                            ? Blocks.CAVE_AIR.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    if (current != target) {
                        level.setBlock(pos, target, Block.UPDATE_CLIENTS);
                        changed++;
                    }
                }
            }
        }
        return changed;
    }

    private static int index(int x, int y, int z, int nx, int nz) {
        return (y * nz + z) * nx + x;
    }

    private static void transformAxis(double[] data, int n, int stride) {
        if (n < 2) {
            return;
        }
        double[] line = new double[n];
        double[] result = new double[n];
        int[] v = new int[n];
        double[] z = new double[n + 1];
        for (int start = 0; start < data.length; start++) {
            if ((start / stride) % n != 0) {
                continue;
            }
            for (int q = 0; q < n; q++) {
                line[q] = data[start + q * stride];
            }
            int k = 0;
            v[0] = 0;
            z[0] = Double.NEGATIVE_INFINITY;
            z[1] = Double.POSITIVE_INFINITY;
            for (int q = 1; q < n; q++) {
                double s = intersection(line, q, v[k]);
                while (s <= z[k]) {
                    k--;
                    s = intersection(line, q, v[k]);
                }
                k++;
                v[k] = q;
                z[k] = s;
                z[k + 1] = Double.POSITIVE_INFINITY;
            }
            k = 0;
            for (int q = 0; q < n; q++) {
                while (z[k + 1] < q) {
                    k++;
                }
                result[q] = (double) (q - v[k]) * (q - v[k]) + line[v[k]];
            }
            for (int q = 0; q < n; q++) {
                data[start + q * stride] = result[q];
            }
        }
    }

    private static double intersection(double[] f, int q, int p) {
        return ((f[q] + (double) q * q) - (f[p] + (double) p * p)) / (2.0 * q - 2.0 * p);
    }
}
