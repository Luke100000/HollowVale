package net.conczin.hollowvale.effect;

import net.conczin.hollowvale.Common;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModEffects {
    private static final Map<String, MobEffect> EFFECTS = new LinkedHashMap<>();

    /**
     * Left behind by a reeled in harpoon, the victim takes extra damage while it lasts.
     */
    public static final ResourceKey<MobEffect> HOOKED = register("hooked", new MobEffect(MobEffectCategory.HARMFUL, 0xD8D2C0) {
    });

    private static ResourceKey<MobEffect> register(String name, MobEffect effect) {
        EFFECTS.put(name, effect);
        return ResourceKey.create(Registries.MOB_EFFECT, Common.locate(name));
    }

    public static Holder<MobEffect> holder(ResourceKey<MobEffect> key) {
        return BuiltInRegistries.MOB_EFFECT.get(key).orElseThrow();
    }

    public static void registerEffects(Common.RegisterHelper<MobEffect> helper) {
        EFFECTS.forEach((name, effect) -> helper.register(Common.locate(name), effect));
    }
}
