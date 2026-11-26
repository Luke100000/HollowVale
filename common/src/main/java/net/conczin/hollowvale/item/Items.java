package net.conczin.hollowvale.item;

import net.conczin.hollowvale.Common;
import net.conczin.hollowvale.block.Blocks;
import net.conczin.hollowvale.entity.Entities;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class Items {
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

    public static final ResourceKey<JukeboxSong> HOLLOW_VALE_SONG = ResourceKey.create(Registries.JUKEBOX_SONG, Common.locate("hollow_vale"));

    public static final Item ENDER_MARROW = register("ender_marrow", EnderMarrowItem::new);
    public static final Item MARROW_LAMP = register("marrow_lamp", p -> new MarrowLampItem(Blocks.MARROW_LAMP, p.useBlockDescriptionPrefix()));
    public static final Item WIND_CHIME = blockItem("wind_chime", Blocks.WIND_CHIME);
    public static final Item WHALE_EGG = register("whale_egg", new Item.Properties().rarity(Rarity.UNCOMMON), p -> new BlockItem(Blocks.WHALE_EGG, p.useBlockDescriptionPrefix()));

    public static final Item BONE_BRICKS = blockItem("bone_bricks", Blocks.BONE_BRICKS);
    public static final Item BONE_BRICK_STAIRS = blockItem("bone_brick_stairs", Blocks.BONE_BRICK_STAIRS);
    public static final Item BONE_BRICK_SLAB = blockItem("bone_brick_slab", Blocks.BONE_BRICK_SLAB);
    public static final Item BONE_BRICK_WALL = blockItem("bone_brick_wall", Blocks.BONE_BRICK_WALL);
    public static final Item CRACKED_BONE_BRICKS = blockItem("cracked_bone_bricks", Blocks.CRACKED_BONE_BRICKS);
    public static final Item CHISELED_BONE_BRICKS = blockItem("chiseled_bone_bricks", Blocks.CHISELED_BONE_BRICKS);
    public static final Item BONE_PILLAR = blockItem("bone_pillar", Blocks.BONE_PILLAR);
    public static final Item MARROW_INFUSED_BONE = blockItem("marrow_infused_bone", Blocks.MARROW_INFUSED_BONE);
    public static final Item ENDER_INFUSED_GRASS = blockItem("ender_infused_grass", Blocks.ENDER_INFUSED_GRASS);

    public static final Item ENDER_FISH_SOUP = register("ender_fish_soup", new Item.Properties()
            .stacksTo(1)
            .food(new FoodProperties.Builder()
                    .nutrition(10)
                    .saturationModifier(1.2f)
                    .alwaysEdible()
                    .build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                                    new MobEffectInstance(MobEffects.NIGHT_VISION, 60 * 20),
                                    new MobEffectInstance(MobEffects.LEVITATION, 5 * 20)), 1.0f))
                            .build())
            .usingConvertsTo(net.minecraft.world.item.Items.BOWL), Item::new);
    public static final Item BONE_WHALE_OMELETTE = register("bone_whale_omelette", new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(12).saturationModifier(0.9f).build()), Item::new);
    public static final Item FRY_BUCKET = register("fry_bucket", new Item.Properties().stacksTo(1), FryBucketItem::new);
    public static final Item MUSIC_DISC_HOLLOW_VALE = register("music_disc_hollow_vale", new Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.RARE)
            .jukeboxPlayable(HOLLOW_VALE_SONG), Item::new);
    public static final Item BONE_HARPOON = register("bone_harpoon", HarpoonItem::new);
    public static final Item BONE_MASK = register("bone_mask", BoneMaskItem::new);

    public static final Item BONE_WHALE_SPAWN_EGG = spawnEgg("bone_whale", Entities.BONE_WHALE);
    public static final Item FRY_SPAWN_EGG = spawnEgg("fry", Entities.FRY);
    public static final Item HOPPING_SKULL_SPAWN_EGG = spawnEgg("hopping_skull", Entities.HOPPING_SKULL);
    public static final Item BONE_SLUDGE_SPAWN_EGG = spawnEgg("bone_sludge", Entities.BONE_SLUDGE);
    public static final Item SKULL_SLUDGE_SPAWN_EGG = spawnEgg("skull_sludge", Entities.SKULL_SLUDGE);
    public static final Item ENDER_SKULL_SPAWN_EGG = spawnEgg("ender_skull", Entities.ENDER_SKULL);
    public static final Item UNDEAD_HIGHLAND_CATTLE_SPAWN_EGG = spawnEgg("undead_highland_cattle", Entities.UNDEAD_HIGHLAND_CATTLE);
    public static final Item SCAVENGER_SPAWN_EGG = spawnEgg("scavenger", Entities.SCAVENGER);

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        return register(name, new Item.Properties(), factory);
    }

    private static Item register(String name, Item.Properties properties, Function<Item.Properties, Item> factory) {
        Item item = factory.apply(properties.setId(ResourceKey.create(Registries.ITEM, Common.locate(name))));
        ITEMS.put(name, item);
        return item;
    }

    private static Item blockItem(String name, Block block) {
        return register(name, p -> new BlockItem(block, p.useBlockDescriptionPrefix()));
    }

    private static Item spawnEgg(String name, EntityType<? extends Mob> type) {
        return register(name + "_spawn_egg", new Item.Properties().spawnEgg(type), SpawnEggItem::new);
    }

    public static void registerItems(Common.RegisterHelper<Item> helper) {
        ITEMS.forEach((name, item) -> helper.register(Common.locate(name), item));
    }

    public static CreativeModeTab createTab(CreativeModeTab.Builder builder) {
        return builder
                .title(Component.translatable("itemGroup." + Common.MOD_ID))
                .icon(() -> new ItemStack(ENDER_MARROW))
                .displayItems((parameters, output) -> ITEMS.values().forEach(item -> output.accept(new ItemStack(item))))
                .build();
    }
}
