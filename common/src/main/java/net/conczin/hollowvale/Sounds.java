package net.conczin.hollowvale;

import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;

public final class Sounds {
    private static final List<SoundEvent> SOUNDS = new ArrayList<>();

    public static final SoundEvent MUSIC_HOLLOW_VALE = create("music.hollow_vale");
    public static final SoundEvent MUSIC_OSSUARY = create("music.ossuary");
    public static final SoundEvent MUSIC_MARROW_HOLLOWS = create("music.marrow_hollows");
    public static final SoundEvent MUSIC_DISC_HOLLOW_VALE = create("music_disc.hollow_vale");

    public static final SoundEvent AMBIENT_HOLLOW_VALE_LOOP = create("ambient.hollow_vale.loop");
    public static final SoundEvent AMBIENT_OSSUARY_LOOP = create("ambient.ossuary.loop");
    public static final SoundEvent AMBIENT_MARROW_HOLLOWS_LOOP = create("ambient.marrow_hollows.loop");

    public static final SoundEvent AMBIENT_ADDITIONS = create("ambient.additions");
    public static final SoundEvent AMBIENT_BONES = create("ambient.bones");
    public static final SoundEvent AMBIENT_CREAKS = create("ambient.creaks");
    public static final SoundEvent AMBIENT_CROWS = create("ambient.crows");
    public static final SoundEvent AMBIENT_WINDS = create("ambient.winds");
    public static final SoundEvent AMBIENT_WHISPERS = create("ambient.whispers");
    public static final SoundEvent AMBIENT_WHALES = create("ambient.whales");
    public static final SoundEvent AMBIENT_DRIPS = create("ambient.drips");

    public static final SoundEvent BONE_WHALE_AMBIENT = create("entity.bone_whale.ambient");
    public static final SoundEvent BONE_WHALE_HURT = create("entity.bone_whale.hurt");
    public static final SoundEvent BONE_WHALE_DEATH = create("entity.bone_whale.death");
    public static final SoundEvent BONE_WHALE_TELEPORT = create("entity.bone_whale.teleport");

    public static final SoundEvent FRY_AMBIENT = create("entity.fry.ambient");
    public static final SoundEvent FRY_HURT = create("entity.fry.hurt");
    public static final SoundEvent FRY_DEATH = create("entity.fry.death");
    public static final SoundEvent FRY_EAT = create("entity.fry.eat");

    public static final SoundEvent HOPPING_SKULL_AMBIENT = create("entity.hopping_skull.ambient");
    public static final SoundEvent HOPPING_SKULL_HOP = create("entity.hopping_skull.hop");
    public static final SoundEvent HOPPING_SKULL_FUSE = create("entity.hopping_skull.fuse");
    public static final SoundEvent HOPPING_SKULL_HURT = create("entity.hopping_skull.hurt");
    public static final SoundEvent HOPPING_SKULL_DEATH = create("entity.hopping_skull.death");

    public static final SoundEvent BONE_SLUDGE_SQUISH = create("entity.bone_sludge.squish");
    public static final SoundEvent BONE_SLUDGE_JUMP = create("entity.bone_sludge.jump");
    public static final SoundEvent BONE_SLUDGE_ATTACK = create("entity.bone_sludge.attack");
    public static final SoundEvent BONE_SLUDGE_HURT = create("entity.bone_sludge.hurt");
    public static final SoundEvent BONE_SLUDGE_DEATH = create("entity.bone_sludge.death");

    public static final SoundEvent SKULL_SLUDGE_SQUISH = create("entity.skull_sludge.squish");
    public static final SoundEvent SKULL_SLUDGE_SPIT = create("entity.skull_sludge.spit");
    public static final SoundEvent SKULL_SLUDGE_ATTACK = create("entity.skull_sludge.attack");
    public static final SoundEvent SKULL_SLUDGE_HURT = create("entity.skull_sludge.hurt");
    public static final SoundEvent SKULL_SLUDGE_DEATH = create("entity.skull_sludge.death");

    public static final SoundEvent ENDER_SKULL_AMBIENT = create("entity.ender_skull.ambient");
    public static final SoundEvent ENDER_SKULL_AIM = create("entity.ender_skull.aim", 48.0f);
    public static final SoundEvent ENDER_SKULL_SCREAM = create("entity.ender_skull.scream", 64.0f);
    public static final SoundEvent ENDER_SKULL_ATTACK = create("entity.ender_skull.attack");
    public static final SoundEvent ENDER_SKULL_CHOMP = create("entity.ender_skull.chomp");
    public static final SoundEvent ENDER_SKULL_HURT = create("entity.ender_skull.hurt");
    public static final SoundEvent ENDER_SKULL_DEATH = create("entity.ender_skull.death");

    public static final SoundEvent BUCKET_FILL_FRY = create("item.bucket.fill_fry");
    public static final SoundEvent BUCKET_EMPTY_FRY = create("item.bucket.empty_fry");

    public static final SoundEvent WIND_CHIME = create("block.wind_chime.chime");

    private static SoundEvent create(String name) {
        return register(SoundEvent.createVariableRangeEvent(Common.locate(name)));
    }

    private static SoundEvent create(String name, float range) {
        return register(SoundEvent.createFixedRangeEvent(Common.locate(name), range));
    }

    private static SoundEvent register(SoundEvent sound) {
        SOUNDS.add(sound);
        return sound;
    }

    public static void registerSounds(Common.RegisterHelper<SoundEvent> helper) {
        SOUNDS.forEach(sound -> helper.register(sound.location(), sound));
    }
}
