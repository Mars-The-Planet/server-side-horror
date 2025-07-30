package com.mars.serversidehorror;

import com.mars.deimos.config.DeimosConfig;

public class ServersideHorrorConfig extends DeimosConfig {
    @Entry public static int herobrine_starer_chance = 180000;
    @Entry public static boolean herobrine_starer_enable = true;

    @Entry public static int fake_joiner_chance = 780000;
    @Entry public static boolean fake_joiner_enable = true;

    @Entry public static int jumpscare_chance = 1080000;
    @Entry public static boolean jumpscare_enable = true;

    @Entry public static int long_night_chance = 75;
    @Entry public static boolean long_night_enable = true;

    @Entry public static int break_torches_chance = 780000;
    @Entry public static boolean break_torches_enable = true;

    @Entry public static int replace_torches_chance = 780000;
    @Entry public static boolean replace_torches_enable = true;
}
