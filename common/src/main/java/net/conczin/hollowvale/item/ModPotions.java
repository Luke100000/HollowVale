package net.conczin.hollowvale.item;

import net.conczin.hollowvale.Common;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModPotions {
    private static final Map<String, Potion> POTIONS = new LinkedHashMap<>();

    public static final ResourceKey<Potion> LEVITATION = register("levitation", new Potion(new MobEffectInstance(MobEffects.LEVITATION, 30 * 20)));
    public static final ResourceKey<Potion> LONG_LEVITATION = register("long_levitation", new Potion("levitation", new MobEffectInstance(MobEffects.LEVITATION, 60 * 20)));

    private static ResourceKey<Potion> register(String name, Potion potion) {
        POTIONS.put(name, potion);
        return ResourceKey.create(Registries.POTION, Common.locate(name));
    }

    private static Holder<Potion> holder(ResourceKey<Potion> key) {
        return BuiltInRegistries.POTION.getHolderOrThrow(key);
    }

    public static void registerPotions(Common.RegisterHelper<Potion> helper) {
        POTIONS.forEach((name, potion) -> helper.register(Common.locate(name), potion));
    }

    public static void addBrewingRecipes(PotionBrewing.Builder builder) {
        builder.addMix(net.minecraft.world.item.alchemy.Potions.AWKWARD, Items.ENDER_MARROW, holder(LEVITATION));
        builder.addMix(holder(LEVITATION), net.minecraft.world.item.Items.REDSTONE, holder(LONG_LEVITATION));
    }
}
