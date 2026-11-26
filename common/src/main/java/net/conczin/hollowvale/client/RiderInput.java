package net.conczin.hollowvale.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

public final class RiderInput {
    public static boolean isAscending(Player player) {
        return player instanceof LocalPlayer local && local.input.keyPresses.jump();
    }

    public static boolean isDescending(Player player) {
        return player instanceof LocalPlayer local && local.input.keyPresses.shift();
    }
}
