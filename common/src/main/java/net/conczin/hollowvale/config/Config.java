package net.conczin.hollowvale.config;

import net.conczin.hollowvale.Common;

public final class Config extends JsonConfig {
    private static final Config INSTANCE = loadOrCreate(new Config(Common.MOD_ID), Config.class);

    // Chance per minute spent in the Vale or Ossuary.
    public float scavengerSpawnChance = 1.0f / 30.0f;

    @Override
    int getVersion() {
        return 2;
    }

    public Config(String name) {
        super(name);
    }

    public static Config getInstance() {
        return INSTANCE;
    }
}
