package net.conczin.hollowvale.neoforge;

import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.Sounds;
import net.conczin.hollowvale.config.Config;
import net.conczin.hollowvale.entity.Entities;
import net.conczin.hollowvale.item.Items;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.function.Consumer;

@Mod(Common.MOD_ID)
@EventBusSubscriber(modid = Common.MOD_ID)
public final class CommonNeoForge {
    public CommonNeoForge() {
        //noinspection ResultOfMethodCallIgnored
        Config.getInstance();
    }

    private static <T> void registerHelper(RegisterEvent event, Registry<T> register, Consumer<Common.RegisterHelper<T>> consumer) {
        event.register(register.key(), registry -> consumer.accept(registry::register));
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        registerHelper(event, BuiltInRegistries.SOUND_EVENT, Sounds::registerSounds);
        registerHelper(event, BuiltInRegistries.ENTITY_TYPE, Entities::registerEntities);
        registerHelper(event, BuiltInRegistries.ITEM, Items::registerItems);
        event.register(Registries.CREATIVE_MODE_TAB, registry -> registry.register(Common.locate(Common.MOD_ID), Items.createTab(CreativeModeTab.builder())));
    }

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        Entities.registerAttributes(event::put);
    }
}
