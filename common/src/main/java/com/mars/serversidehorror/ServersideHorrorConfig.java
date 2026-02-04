package com.mars.serversidehorror;

import com.mars.deimos.config.DeimosConfig;
import com.google.common.collect.Lists;

import java.util.List;

public class ServersideHorrorConfig extends DeimosConfig {
    @Entry public static int grace_period = 3;
    @Entry public static boolean grace_period_applies_to_traps = false;

    @Entry public static int herobrine_starer_chance = 120000;
    @Entry public static boolean herobrine_starer_enable = true;

    @Entry public static int fake_joiner_chance = 720000;
    @Entry public static boolean fake_joiner_enable = true;

    @Entry public static int jumpscare_chance = 1080000;
    @Entry public static boolean jumpscare_enable = true;

    @Entry public static int long_night_chance = 75;
    @Entry public static boolean long_night_enable = true;

    @Entry public static int break_torches_chance = 720000;
    @Entry public static boolean break_torches_enable = true;

    @Entry public static int replace_torches_chance = 720000;
    @Entry public static boolean replace_torches_enable = true;

    @Entry public static int random_lightning_chance = 1800000;
    @Entry public static boolean random_lightning_enable = true;

    @Entry public static int fake_mining_chance = 500000;
    @Entry public static boolean fake_mining_enable = true;

    @Entry public static int fake_steps_chance = 500000;
    @Entry public static boolean fake_steps_enable = true;

    @Entry public static int setting_up_new_traps_chance = 1500000;
    @Entry public static boolean setting_up_new_traps_enable = true;

    @Entry public static int burn_down_house_chance_per_wake_up = 100;
    @Entry public static boolean burn_down_house_enable = false;

    @Entry public static int joining_on_bedrock_chance = 70;
    @Entry public static boolean joining_on_bedrock_enable = false;

    @Entry public static int joining_in_dungeon_chance = 140;
    @Entry public static boolean joining_in_dungeon_enable = true;

    @Entry public static int removing_leaves_chance = 2000000;
    @Entry public static boolean removing_leaves_enable = false;

    @Entry public static int random_signs_chance = 500000;
    @Entry public static boolean random_signs_enable = true;
    @Entry public static List<String> random_signs_texts = Lists.newArrayList("GG\n \n \n ",
            " \nBehind you\n \n ",
            " \nBAF\n \n ",
            " \n \n \nGG",
            " \n \n \n ",
            "null\n \n \n ",
            "Help!\nNull is here!\nTell everyone\nabout me!",
            "Leave\n \n \n:)",
            "I am\nwatching\n \n ",
            " \nYou shouldn’t\nbe here\n ",
            "I saw what\nyou did\n \n ",
            "This world\nisn’t yours\n \n ",
            "You don’t\nbelong here\n \n ",
            "The door\nis open\n \n ",
            "It’s already\ninside\n \n ",
            "I was here\n \n \n ",
            "DO NOT\n \n \nFORGET",
            "DO NOT\n \nRUN\n ",
            " \nI can’t feel\nmy hands\n ",
            "It hurts to\nbreathe\n \n ",
            "Buried alive\n \n \n ",
            "Still here\n \n \n ",
            "EZ\n \n \n ",
            "Projekt_M\nwas here\n \n ",
            "I am from Mars\n \n \n ");

    @Entry public static int random_fake_joiner_chance = 720000;
    @Entry public static boolean random_fake_joiner_enable = true;
    @Entry public static List<String> random_fake_joiner_list = Lists.newArrayList("Projekt_M;BAF;wusup;Huh, wrong server;Hello?;I want to play with you;Want to be friends?",
            "MarsThePlanet_;Make a wish;No eyes always watches");

    @Entry public static int starer_chance = 420000;
    @Entry public static boolean starer_enable = true;
    @Entry public static List<String> starer_list = Lists.newArrayList("Projekt_M", "MarsThePlanet_", "Filios14", "Ray857", "Harlock195", "MenT97", "DanTDM");

    @Entry public static int heads_from_list_chance = 800000;
    @Entry public static boolean heads_from_list_enable = true;
    @Entry public static List<String> heads_from_list_list = Lists.newArrayList("Projekt_M", "MarsThePlanet_", "Filios14", "Ray857", "Harlock195", "MenT97", "DanTDM");

    @Entry public static int scary_sound_chance = 1000000;
    @Entry public static boolean scary_sound_enable = true;
    @Entry public static List<String> scary_sound_list = Lists.newArrayList(
            "minecraft:block.bell.resonate",
            "minecraft:block.bell.use",
            "minecraft:entity.tnt.primed",
            "minecraft:entity.generic.explode",
            "minecraft:entity.creeper.primed",
            "minecraft:entity.arrow.hit",
            "minecraft:item.trident.hit_ground",
            "minecraft:entity.polar_bear.ambient",
            "minecraft:entity.polar_bear.ambient_baby",
            "minecraft:item.crossbow.hit",
            "minecraft:entity.polar_bear.death",
            "minecraft:entity.polar_bear.warning",
            "minecraft:entity.dragon_fireball.explode",
            "minecraft:entity.splash_potion.break",
            "minecraft:entity.ghast.scream",
            "minecraft:entity.allay.death");

    @Entry public static int random_heads_chance = 800000;
    @Entry public static boolean random_heads_enable = true;

    @Entry public static boolean old_villages_enable = true;
    @Entry public static boolean traps_enable = true;
}
