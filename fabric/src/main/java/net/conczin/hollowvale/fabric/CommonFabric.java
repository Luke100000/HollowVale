package net.conczin.hollowvale.fabric;

import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.Sounds;
import net.conczin.hollowvale.Structurify;
import net.conczin.hollowvale.block.Blocks;
import net.conczin.hollowvale.config.Config;
import net.conczin.hollowvale.effect.ModEffects;
import net.conczin.hollowvale.entity.Entities;
import net.conczin.hollowvale.fabric.mixin.SpawnPlacementsInvoker;
import net.conczin.hollowvale.item.Items;
import net.conczin.hollowvale.item.ModPotions;
import net.conczin.hollowvale.world.BiolithCompat;
import net.conczin.hollowvale.world.TemplateFeature;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.function.Consumer;

public final class CommonFabric implements ModInitializer {
    private static <T> void registerHelper(Registry<T> register, Consumer<Common.RegisterHelper<T>> consumer) {
        consumer.accept((name, value) -> Registry.register(register, name, value));
    }

    @Override
    public void onInitialize() {
        //noinspection ResultOfMethodCallIgnored
        Config.getInstance();

        registerHelper(BuiltInRegistries.SOUND_EVENT, Sounds::registerSounds);
        registerHelper(BuiltInRegistries.FEATURE, TemplateFeature::register);
        registerHelper(BuiltInRegistries.ENTITY_TYPE, Entities::registerEntities);
        registerHelper(BuiltInRegistries.BLOCK, Blocks::registerBlocks);
        registerHelper(BuiltInRegistries.ITEM, Items::registerItems);
        registerHelper(BuiltInRegistries.POTION, ModPotions::registerPotions);
        registerHelper(BuiltInRegistries.MOB_EFFECT, ModEffects::registerEffects);
        FabricBrewingRecipeRegistryBuilder.BUILD.register(ModPotions::addBrewingRecipes);
        registerHelper(BuiltInRegistries.BLOCK_ENTITY_TYPE, helper -> Blocks.registerBlockEntities(helper,
                (factory, blocks) -> FabricBlockEntityTypeBuilder.create(factory::apply, blocks).build()));
        PointOfInterestHelper.register(Blocks.MARROW_LAMP_POI.location(), 0, 1, Blocks.MARROW_LAMP);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Common.locate(Common.MOD_ID), Items.createTab(FabricItemGroup.builder()));

        Entities.registerAttributes(FabricDefaultAttributeRegistry::register);
        Entities.registerSpawnPlacements(SpawnPlacementsInvoker::register);
        if (FabricLoader.getInstance().isModLoaded("biolith")) {
            BiolithCompat.register();
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> Structurify.register(dispatcher));
    }
}
