package net.conczin.hollowvale.entity;

import net.conczin.hollowvale.Common;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class Entities {
    private static final Map<String, EntityType<?>> TYPES = new LinkedHashMap<>();

    public static final EntityType<BoneWhale> BONE_WHALE = register("bone_whale",
            EntityType.Builder.of(BoneWhale::new, MobCategory.CREATURE).sized(3.0f, 1.5f).clientTrackingRange(10));
    public static final EntityType<Fry> FRY = register("fry",
            EntityType.Builder.of(Fry::new, MobCategory.MONSTER).sized(0.5f, 0.4f));
    public static final EntityType<HoppingSkull> HOPPING_SKULL = register("hopping_skull",
            EntityType.Builder.of(HoppingSkull::new, MobCategory.MONSTER).sized(0.5f, 0.5f));
    public static final EntityType<BoneSludge> BONE_SLUDGE = register("bone_sludge",
            EntityType.Builder.of(BoneSludge::new, MobCategory.MONSTER).sized(0.9f, 0.9f));
    public static final EntityType<SkullSludge> SKULL_SLUDGE = register("skull_sludge",
            EntityType.Builder.of(SkullSludge::new, MobCategory.MONSTER).sized(1.8f, 1.6f));
    public static final EntityType<EnderSkull> ENDER_SKULL = register("ender_skull",
            EntityType.Builder.of(EnderSkull::new, MobCategory.MONSTER).sized(0.75f, 0.75f));

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        EntityType<T> type = builder.build(name);
        TYPES.put(name, type);
        return type;
    }

    public static void registerEntities(Common.RegisterHelper<EntityType<?>> helper) {
        TYPES.forEach((name, type) -> helper.register(Common.locate(name), type));
    }

    public static void registerAttributes(BiConsumer<EntityType<? extends LivingEntity>, AttributeSupplier> consumer) {
        consumer.accept(BONE_WHALE, BoneWhale.createAttributes().build());
        consumer.accept(FRY, Fry.createAttributes().build());
        consumer.accept(HOPPING_SKULL, HoppingSkull.createAttributes().build());
        consumer.accept(BONE_SLUDGE, BoneSludge.createAttributes().build());
        consumer.accept(SKULL_SLUDGE, SkullSludge.createAttributes().build());
        consumer.accept(ENDER_SKULL, EnderSkull.createAttributes().build());
    }
}
