package com.mars.serversidehorror;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.List;

public class SavedDataHorror extends SavedData {
    private List<String> player_messages;
    private boolean long_night;
    public static final Codec<SavedDataHorror> CODEC = RecordCodecBuilder.create(
            builder -> builder.group(
                    Codec.BOOL.fieldOf("long_night").forGetter(SavedDataHorror::getLongNight),
                    Codec.STRING.listOf().fieldOf("player_messages").forGetter(SavedDataHorror::getPlayerMessages)
            ).apply(builder, SavedDataHorror::new)
    );
    public static final SavedDataType<SavedDataHorror> TYPE = new SavedDataType<>("saved_data_horror", SavedDataHorror::new, CODEC, null );

    public SavedDataHorror() {
        this(false, new ArrayList<>());
    }

    public SavedDataHorror(boolean long_night, List<String> player_messages) {
        this.long_night = long_night;
        this.player_messages = new ArrayList<>(player_messages);;
    }

    public static SavedDataHorror get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        SavedDataHorror data = storage.computeIfAbsent(TYPE);
        return data;
    }

    public void addMessage(String msg){
        if(!player_messages.contains(msg)){
            player_messages.add(msg);
            this.setDirty();
        }
    }

    public void setPlayerMessages(List<String> value) {
        this.player_messages = new ArrayList<>(value);
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
