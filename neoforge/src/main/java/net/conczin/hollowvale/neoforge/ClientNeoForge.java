package net.conczin.hollowvale.neoforge;

import net.conczin.hollowvale.Common;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

@Mod(value = Common.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Common.MOD_ID, value = Dist.CLIENT)
public final class ClientNeoForge {
}
