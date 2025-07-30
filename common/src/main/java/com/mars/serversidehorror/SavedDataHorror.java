package com.mars.serversidehorror;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class SavedDataHorror extends SavedData {
    private List<String> player_messages = new ArrayList<>();
    private static final String keyPlayerMessages = "player_messages";
    private boolean long_night;

    private SavedDataHorror() { super(); }

    public static SavedDataHorror create() {
        return new SavedDataHorror();
    }

    public static SavedDataHorror load(CompoundTag tag, HolderLookup.Provider prov) {
        SavedDataHorror data = SavedDataHorror.create();

        ListTag listTag = (ListTag) tag.get(keyPlayerMessages);
        if(listTag != null){
            listTag.forEach(i -> {
                if(i instanceof StringTag stringTag)
                    data.player_messages.add(stringTag.getAsString());
            });
        }

        data.long_night = tag.getBoolean("long_night");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider prov) {
        ListTag listTag = new ListTag();
        player_messages.forEach(i -> listTag.add(StringTag.valueOf(i)));
        tag.put(keyPlayerMessages, listTag);

        tag.putBoolean("long_night", this.long_night);

        return tag;
    }

    public void addMessage(String msg){
        if(!player_messages.contains(msg)){
            player_messages.add(msg);
            this.setDirty();
        }
    }

    public void setPlayerMessages(List<String> value) {
        this.player_messages = value;
        this.setDirty();
    }

    public boolean getLongNight() {
        return long_night;
    }


    public void setLongNight(boolean value) {
        this.long_night = value;
        this.setDirty();
    }

    public List<String> getPlayerMessages() {
        return player_messages;
    }
}
