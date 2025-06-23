package com.example.mymusic.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.mymusic.data.local.converter.Converters;

@Database(entities = {Token.class, Favorites.class, FavoriteArtist.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    public abstract TokenDao tokenDao();
    public abstract FavoritesDao favoritesDao();
    public abstract FavoriteArtistDao favoriteArtistDao();
    //public abstract SettingDao settingDao();
    public static synchronized AppDatabase getInstance(Context context){
        if (instance == null){
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "app_database"
            ).build();
        }
        return instance;
    }
}
