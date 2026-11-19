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

import java.util.LinkedHashMap;
import java.util.Map;

public final class Items {
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

    public static final ResourceKey<JukeboxSong> HOLLOW_VALE_SONG = ResourceKey.create(Registries.JUKEBOX_SONG, Common.locate("hollow_vale"));

    public static final Item ENDER_MARROW = register("ender_marrow", new Item(new Item.Properties()));
    public static final Item MARROW_LAMP = register("marrow_lamp", new BlockItem(Blocks.MARROW_LAMP, new Item.Properties()));
    public static final Item ENDER_FISH_SOUP = register("ender_fish_soup", new Item(new Item.Properties()
            .stacksTo(1)
            .food(new FoodProperties.Builder()
                    .nutrition(10)
                    .saturationModifier(1.2f)
                    .alwaysEdible()
                    .effect(new MobEffectInstance(MobEffects.NIGHT_VISION, 60 * 20), 1.0f)
                    .effect(new MobEffectInstance(MobEffects.LEVITATION, 5 * 20), 1.0f)
                    .usingConvertsTo(net.minecraft.world.item.Items.BOWL)
                    .build())));
    public static final Item FRY_BUCKET = register("fry_bucket", new FryBucketItem(new Item.Properties().stacksTo(1)));
    public static final Item MUSIC_DISC_HOLLOW_VALE = register("music_disc_hollow_vale", new Item(new Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.RARE)
            .jukeboxPlayable(HOLLOW_VALE_SONG)));

    public static final Item BONE_WHALE_SPAWN_EGG = spawnEgg("bone_whale", Entities.BONE_WHALE, 0xD8D2C0, 0x7A4FA0);
    public static final Item FRY_SPAWN_EGG = spawnEgg("fry", Entities.FRY, 0xD8D2C0, 0x5E6E78);
    public static final Item HOPPING_SKULL_SPAWN_EGG = spawnEgg("hopping_skull", Entities.HOPPING_SKULL, 0xC1C1C1, 0x494949);
    public static final Item BONE_SLUDGE_SPAWN_EGG = spawnEgg("bone_sludge", Entities.BONE_SLUDGE, 0x8A809E, 0xE3DDCB);
    public static final Item SKULL_SLUDGE_SPAWN_EGG = spawnEgg("skull_sludge", Entities.SKULL_SLUDGE, 0x6E6585, 0xE3DDCB);
    public static final Item ENDER_SKULL_SPAWN_EGG = spawnEgg("ender_skull", Entities.ENDER_SKULL, 0x1E1E2A, 0xB35CDB);

    private static Item register(String name, Item item) {
        ITEMS.put(name, item);
        return item;
    }

    private static Item spawnEgg(String name, EntityType<? extends Mob> type, int background, int highlight) {
        return register(name + "_spawn_egg", new SpawnEggItem(type, background, highlight, new Item.Properties()));
    }

    public static void registerItems(Common.RegisterHelper<Item> helper) {
        ITEMS.forEach((name, item) -> helper.register(Common.locate(name), item));
    }

    public static CreativeModeTab createTab(CreativeModeTab.Builder builder) {
        return builder
                .title(Component.translatable("itemGroup." + Common.MOD_ID))
                .icon(() -> new ItemStack(ENDER_MARROW))
                .displayItems((parameters, output) -> ITEMS.values().forEach(output::accept))
                .build();
    }
}
