package com.example.mymusic.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "settings_table")
public class Settings {
    @NonNull
    @PrimaryKey
    public int id = 1;


}
