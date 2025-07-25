package com.mars.serversidehorror;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class PlayerBehavior extends SavedData {
    private List<String> player_names = new ArrayList<>();
    private int test;

    private PlayerBehavior() { super(); }

    public static PlayerBehavior create() {
        return new PlayerBehavior();
    }

    public static PlayerBehavior load(CompoundTag tag, HolderLookup.Provider prov) {
        PlayerBehavior data = PlayerBehavior.create();
//        ListTag listTag = tag.getList("player_names", StringTag.TAG_COMPOUND);
//        for (int i = 0; i < listTag.size(); i++) {
//            data.player_names.add(listTag.getString(i));
//        }

        data.test = tag.getInt("test");
        return data;
    }

    public void addPlayer(String name){
        if(!player_names.contains(name)){
            System.out.println("add");
            player_names.add(name);
            this.setDirty();
        }
    }

    public void setPlayerNames(List<String> value) {
        this.player_names = value;
        this.setDirty();
    }

    public int getTest() {
        return test;
    }


    public void setTest(int value) {
        this.test = value;
        this.setDirty();
    }

    public List<String> getPlayer_names() {
        return player_names;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider prov) {
//        ListTag listTag = new ListTag();
//        for (String s : this.player_names) {
//            listTag.add(StringTag.valueOf(s));
//        }
//        tag.put("player_names", listTag);

        tag.putInt("test", this.test);

        return tag;
    }
}
