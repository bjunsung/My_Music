package com.example.mymusic.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.mymusic.data.local.converter.Converters;

@Database(entities = {Token.class, Favorites.class, FavoriteArtist.class, Settings.class} , version = 4)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    public abstract TokenDao tokenDao();
    public abstract FavoritesDao favoritesDao();
    public abstract FavoriteArtistDao favoriteArtistDao();
    public abstract SettingDao settingDao();
    public static synchronized AppDatabase getInstance(Context context){
        if (instance == null){
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "app_database"
            ).addMigrations(Migrations.MIGRATION_3_4).addCallback(roomCallback).build();
        }
        return instance;
    }

    private static final RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            db.execSQL("INSERT INTO settings_table " +
                    "(id, maxSearchedTracks, maxSearchedArtists, maxSearchedAlbumsByArtist, personalized) " +
                    "VALUES (0, 20, 20, 20, 0)");

            // DB가 처음 생성될 때 1회 호출됨 (기본값 insert 용도)
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            // DB가 열릴 때마다 호출됨 (기본값 삽입 or 점검용)
        }
    };

}
