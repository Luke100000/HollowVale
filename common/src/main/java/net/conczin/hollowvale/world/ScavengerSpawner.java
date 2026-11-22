package net.conczin.hollowvale.world;

import net.conczin.hollowvale.config.Config;
import net.conczin.hollowvale.entity.Entities;
import net.conczin.hollowvale.entity.Scavenger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

public final class ScavengerSpawner {
    private static final int CHECK_INTERVAL = 20 * 60;
    private static final int ATTEMPTS = 10;
    private static final int MIN_DISTANCE = 24;
    private static final int MAX_DISTANCE = 44;

    public static void tick(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD || level.getGameTime() % CHECK_INTERVAL != 0
            || !level.getGameRules().getBoolean(GameRules.RULE_DO_TRADER_SPAWNING)) {
            return;
        }

        RandomSource random = level.random;
        if (random.nextFloat() >= Config.getInstance().scavengerSpawnChance || !level.getEntities(Entities.SCAVENGER, scavenger -> true).isEmpty()) {
            return;
        }

        List<ServerPlayer> players = level.players().stream().filter(player -> isValeBiome(level, player.blockPosition())).toList();
        if (players.isEmpty()) {
            return;
        }
        ServerPlayer player = players.get(random.nextInt(players.size()));
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            BlockPos pos = candidate(level, player, random);
            if (isValeBiome(level, pos) && canStand(level, pos)) {
                spawn(level, pos);
                return;
            }
        }
    }

    private static BlockPos candidate(ServerLevel level, ServerPlayer player, RandomSource random) {
        double angle = random.nextDouble() * Mth.TWO_PI;
        double distance = Mth.nextDouble(random, MIN_DISTANCE, MAX_DISTANCE);
        int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
        int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    private static boolean isValeBiome(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        return biome.is(ModBiomes.HOLLOW_VALE) || biome.is(ModBiomes.OSSUARY);
    }

    private static boolean canStand(ServerLevel level, BlockPos pos) {
        return level.getFluidState(pos).isEmpty() && level.getFluidState(pos.below()).isEmpty()
               && level.noCollision(EntityType.SKELETON_HORSE.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
    }

    private static void spawn(ServerLevel level, BlockPos pos) {
        Scavenger scavenger = Entities.SCAVENGER.create(level);
        if (scavenger == null) {
            return;
        }
        float yaw = level.random.nextFloat() * 360.0f;
        scavenger.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
        scavenger.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        level.addFreshEntity(scavenger);

        if (level.random.nextBoolean()) {
            return;
        }
        SkeletonHorse horse = EntityType.SKELETON_HORSE.create(level);
        if (horse == null) {
            return;
        }
        horse.moveTo(scavenger.getX(), scavenger.getY(), scavenger.getZ(), yaw, 0.0f);
        horse.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        horse.setTamed(true);
        horse.equipSaddle(new ItemStack(Items.SADDLE), null);
        horse.setPersistenceRequired();
        level.addFreshEntity(horse);
        scavenger.startRiding(horse, true);
    }
}
