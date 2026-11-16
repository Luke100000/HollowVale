package net.conczin.hollowvale.config;

import net.conczin.hollowvale.Common;

public final class Config extends JsonConfig {
    private static final Config INSTANCE = loadOrCreate(new Config(Common.MOD_ID), Config.class);

    public Config(String name) {
        super(name);
    }

    public static Config getInstance() {
        return INSTANCE;
    }
}
