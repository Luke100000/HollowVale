package net.conczin.hollowvale.client;

import net.conczin.hollowvale.Sounds;
import net.conczin.hollowvale.world.ModBiomes;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Picks the sound for a biome's additions trigger and places it somewhere in the distance, instead of vanilla's
 * position-less sound in the player's head.
 */
public final class AmbientAdditions {
    private static final int UNDERGROUND_MARGIN = 5;

    /**
     * Spots above the surface are relative to the height map, others relative to the player. Local ones have no
     * position at all, which is what stereo sounds need: OpenAL only positions mono sources.
     */
    private record Spot(SoundEvent sound, int weight, double minDistance, double maxDistance, double minHeight,
                        double maxHeight, boolean aboveSurface, boolean local) {
        static Spot onSurface(SoundEvent sound, int weight, double minDistance, double maxDistance, double minHeight, double maxHeight) {
            return new Spot(sound, weight, minDistance, maxDistance, minHeight, maxHeight, true, false);
        }

        static Spot aroundPlayer(SoundEvent sound, int weight, double minDistance, double maxDistance) {
            return new Spot(sound, weight, minDistance, maxDistance, -2.0, 2.0, false, false);
        }

        static Spot local(SoundEvent sound, int weight) {
            return new Spot(sound, weight, 0, 0, 0, 0, false, true);
        }
    }

    private static final Map<ResourceKey<Biome>, List<Spot>> SPOTS = Map.of(
            ModBiomes.HOLLOW_VALE, List.of(
                    Spot.onSurface(Sounds.AMBIENT_CROWS, 6, 12, 32, 0, 10),
                    Spot.onSurface(Sounds.AMBIENT_CREAKS, 9, 10, 30, 0, 5),
                    Spot.local(Sounds.AMBIENT_WINDS, 4)),
            ModBiomes.OSSUARY, List.of(
                    Spot.onSurface(Sounds.AMBIENT_BONES, 45, 8, 24, 0, 0),
                    Spot.onSurface(Sounds.AMBIENT_CREAKS, 108, 10, 30, 0, 5),
                    Spot.local(Sounds.AMBIENT_WINDS, 48),
                    Spot.local(Sounds.AMBIENT_WHISPERS, 96),
                    Spot.onSurface(Sounds.AMBIENT_WHALES, 60, 20, 50, 10, 30)),
            ModBiomes.MARROW_HOLLOWS, List.of(
                    Spot.aroundPlayer(Sounds.AMBIENT_BONES, 45, 6, 20),
                    Spot.local(Sounds.AMBIENT_WHISPERS, 96),
                    Spot.aroundPlayer(Sounds.AMBIENT_DRIPS, 180, 4, 14)));

    public static @Nullable SimpleSoundInstance create(LocalPlayer player) {
        Level level = player.level();
        Holder<Biome> biome = level.getBiome(player.blockPosition());
        RandomSource random = level.getRandom();

        List<Spot> spots = SPOTS.entrySet().stream().filter(entry -> biome.is(entry.getKey())).map(Map.Entry::getValue).findFirst().orElse(null);
        if (spots == null) {
            return null;
        }

        Spot spot = pick(spots, random);
        if (spot.local) {
            return SimpleSoundInstance.forLocalAmbience(spot.sound, 1.0f, 1.0f);
        }
        double angle = random.nextDouble() * Mth.TWO_PI;
        double distance = Mth.nextDouble(random, spot.minDistance, spot.maxDistance);
        double x = player.getX() + Math.cos(angle) * distance;
        double z = player.getZ() + Math.sin(angle) * distance;
        return SimpleSoundInstance.forAmbientMood(spot.sound, random, x, height(level, player, spot, x, z, random), z);
    }

    private static Spot pick(List<Spot> spots, RandomSource random) {
        int roll = random.nextInt(spots.stream().mapToInt(Spot::weight).sum());
        for (Spot spot : spots) {
            roll -= spot.weight;
            if (roll < 0) {
                return spot;
            }
        }
        return spots.getLast();
    }

    private static double height(Level level, LocalPlayer player, Spot spot, double x, double z, RandomSource random) {
        int playerSurface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, player.getBlockX(), player.getBlockZ());
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
        boolean usable = surface > level.getMinBuildHeight() && player.getY() >= playerSurface - UNDERGROUND_MARGIN;
        if (spot.aboveSurface && usable) {
            return surface + Mth.nextDouble(random, spot.minHeight, spot.maxHeight);
        }
        return player.getEyeY() + Mth.nextDouble(random, -2.0, 2.0);
    }
}
