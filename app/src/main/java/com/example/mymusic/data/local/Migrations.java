package com.example.mymusic.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migrations {

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE favorites_table ADD COLUMN trackNameKr TEXT");
            database.execSQL("ALTER TABLE favorites_table ADD COLUMN lyrics TEXT");
            database.execSQL("ALTER TABLE favorites_table ADD COLUMN lyricists TEXT");
            database.execSQL("ALTER TABLE favorites_table ADD COLUMN composers TEXT");
        }
    };

    // 향후 다른 Migration도 여기에 추가 가능

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE favorites_table ADD COLUMN vibeTrackId TEXT");
            db.execSQL("ALTER TABLE settings_table ADD COLUMN trackIdInputPrefersNumeric INTEGER NOT NULL DEFAULT 1");
        }
    };
}
