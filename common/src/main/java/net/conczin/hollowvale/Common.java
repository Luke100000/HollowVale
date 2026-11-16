package net.conczin.hollowvale;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Common {
    public static final String MOD_ID = "hollowvale";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation locate(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public interface RegisterHelper<T> {
        void register(ResourceLocation name, T value);
    }
}
