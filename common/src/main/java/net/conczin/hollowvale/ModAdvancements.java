package net.conczin.hollowvale;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;

/**
 * Awards advancements whose criterion has no vanilla trigger.
 */
public final class ModAdvancements {
    public static void award(ServerPlayer player, String name, String criterion) {
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Common.locate(name));
        if (advancement != null) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
