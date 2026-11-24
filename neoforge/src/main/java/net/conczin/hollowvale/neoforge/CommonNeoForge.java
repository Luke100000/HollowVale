package net.conczin.hollowvale.neoforge;

import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.Sounds;
import net.conczin.hollowvale.Structurify;
import net.conczin.hollowvale.block.Blocks;
import net.conczin.hollowvale.config.Config;
import net.conczin.hollowvale.effect.ModEffects;
import net.conczin.hollowvale.entity.Entities;
import net.conczin.hollowvale.item.Items;
import net.conczin.hollowvale.item.ModPotions;
import net.conczin.hollowvale.world.BiolithCompat;
import net.conczin.hollowvale.world.ScavengerSpawner;
import net.conczin.hollowvale.world.TemplateFeature;
import net.conczin.hollowvale.world.TerraBlenderCompat;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
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
        registerHelper(event, BuiltInRegistries.FEATURE, TemplateFeature::register);
        registerHelper(event, BuiltInRegistries.ENTITY_TYPE, Entities::registerEntities);
        registerHelper(event, BuiltInRegistries.BLOCK, Blocks::registerBlocks);
        registerHelper(event, BuiltInRegistries.ITEM, Items::registerItems);
        registerHelper(event, BuiltInRegistries.POTION, ModPotions::registerPotions);
        registerHelper(event, BuiltInRegistries.MOB_EFFECT, ModEffects::registerEffects);
        registerHelper(event, BuiltInRegistries.BLOCK_ENTITY_TYPE, helper -> Blocks.registerBlockEntities(helper,
                (factory, blocks) -> BlockEntityType.Builder.of(factory::apply, blocks).build(null)));
        event.register(Registries.POINT_OF_INTEREST_TYPE, registry -> registry.register(Blocks.MARROW_LAMP_POI.location(), Blocks.createMarrowLampPoi()));
        event.register(Registries.CREATIVE_MODE_TAB, registry -> registry.register(Common.locate(Common.MOD_ID), Items.createTab(CreativeModeTab.builder())));
    }

    @SubscribeEvent
    public static void onRegisterBrewingRecipes(RegisterBrewingRecipesEvent event) {
        ModPotions.addBrewingRecipes(event.getBuilder());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ScavengerSpawner.tick(level);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Common.warnIfBiomeLibraryMissing(player);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        if (!FMLLoader.isProduction()) {
            Structurify.register(event.getDispatcher());
        }
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded("biolith")) {
                BiolithCompat.register();
            } else if (ModList.get().isLoaded("terrablender")) {
                TerraBlenderCompat.register();
            } else {
                Common.reportMissingBiomeLibrary();
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        Entities.registerSpawnPlacements(new Entities.SpawnPlacementRegistrar() {
            @Override
            public <T extends Mob> void register(EntityType<T> type, SpawnPlacementType placement, Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) {
                event.register(type, placement, heightmap, predicate, RegisterSpawnPlacementsEvent.Operation.OR);
            }
        });
    }

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        Entities.registerAttributes(event::put);
    }
}
