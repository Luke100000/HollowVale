package net.conczin.hollowvale.entity;

import net.conczin.hollowvale.Common;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

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
            EntityType.Builder.of(BoneSludge::new, MobCategory.MONSTER).sized(10.0f / 16.0f, 10.0f / 16.0f));
    public static final EntityType<SkullSludge> SKULL_SLUDGE = register("skull_sludge",
            EntityType.Builder.of(SkullSludge::new, MobCategory.MONSTER).sized(22.0f / 16.0f, 22.0f / 16.0f));
    public static final EntityType<EnderSkull> ENDER_SKULL = register("ender_skull",
            EntityType.Builder.of(EnderSkull::new, MobCategory.MONSTER).sized(0.825f, 0.9375f));
    public static final EntityType<UndeadHighlandCattle> UNDEAD_HIGHLAND_CATTLE = register("undead_highland_cattle",
            EntityType.Builder.of(UndeadHighlandCattle::new, MobCategory.CREATURE).sized(0.9f, 1.3f));
    public static final EntityType<Scavenger> SCAVENGER = register("scavenger",
            EntityType.Builder.of(Scavenger::new, MobCategory.CREATURE).sized(0.6f, 1.95f).eyeHeight(1.62f).ridingOffset(-0.75f).clientTrackingRange(10));
    public static final EntityType<HarpoonBolt> HARPOON_BOLT = register("harpoon_bolt",
            EntityType.Builder.<HarpoonBolt>of(HarpoonBolt::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(8).updateInterval(2));
    public static final EntityType<SludgeBall> SLUDGE_BALL = register("sludge_ball",
            EntityType.Builder.<SludgeBall>of(SludgeBall::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10));

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
        consumer.accept(UNDEAD_HIGHLAND_CATTLE, UndeadHighlandCattle.createAttributes().build());
        consumer.accept(SCAVENGER, Scavenger.createAttributes().build());
    }

    public interface SpawnPlacementRegistrar {
        <T extends Mob> void register(EntityType<T> type, SpawnPlacementType placement, Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate);
    }

    public static void registerSpawnPlacements(SpawnPlacementRegistrar registrar) {
        Heightmap.Types surface = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        registrar.register(FRY, SpawnPlacementTypes.ON_GROUND, surface, Monster::checkAnyLightMonsterSpawnRules);
        registrar.register(HOPPING_SKULL, SpawnPlacementTypes.ON_GROUND, surface, Monster::checkAnyLightMonsterSpawnRules);
        registrar.register(BONE_SLUDGE, SpawnPlacementTypes.ON_GROUND, surface, Monster::checkAnyLightMonsterSpawnRules);
        registrar.register(SKULL_SLUDGE, SpawnPlacementTypes.ON_GROUND, surface, Monster::checkAnyLightMonsterSpawnRules);
        registrar.register(ENDER_SKULL, SpawnPlacementTypes.ON_GROUND, surface,
                (type, level, reason, pos, random) -> (level.getLevel().isNight() || !level.canSeeSky(pos)) && Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random));
        registrar.register(UNDEAD_HIGHLAND_CATTLE, SpawnPlacementTypes.ON_GROUND, surface, Mob::checkMobSpawnRules);
        registrar.register(BONE_WHALE, SpawnPlacementTypes.NO_RESTRICTIONS, surface,
                (type, level, reason, pos, random) -> level.canSeeSky(pos));
    }
}
