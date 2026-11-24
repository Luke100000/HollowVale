package net.conczin.hollowvale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Common {
    public static final String MOD_ID = "hollowvale";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean biomeLibraryMissing;

    public static void reportMissingBiomeLibrary() {
        biomeLibraryMissing = true;
        LOGGER.warn("Neither Biolith nor TerraBlender is installed, so the Hollow Vale biomes will not generate.");
    }

    public static void warnIfBiomeLibraryMissing(ServerPlayer player) {
        if (biomeLibraryMissing) {
            player.sendSystemMessage(Component.literal("[Hollow Vale] ").append(Component.translatable("message.hollowvale.missing_biome_library")).withStyle(ChatFormatting.RED));
        }
    }

    public static ResourceLocation locate(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public interface RegisterHelper<T> {
        void register(ResourceLocation name, T value);
    }
}
