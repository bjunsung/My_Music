package com.example.mymusic.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migrations {
    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // 1. 기존 테이블에 새 컬럼 추가
            db.execSQL("ALTER TABLE favorites_table ADD COLUMN playCountByDay TEXT");
            // 2. 새 컬럼 playCountList를 빈 JSON 배열로 초기화
            db.execSQL("UPDATE favorites_table SET playCountByDay = '[]'");
        }
    };


    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // 정수형 컬럼 (기본값 0)
            db.execSQL("ALTER TABLE favorites_table ADD COLUMN playCount INTEGER DEFAULT 0");
            // 문자열 컬럼 (기본값 NULL)
            db.execSQL("ALTER TABLE favorites_table ADD COLUMN audioUri TEXT");
        }
    };

    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE favorites_table ADD COLUMN primaryColor INTEGER");
        }
    };
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE artist_metadata_table ADD COLUMN artistNameKr TEXT");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS artist_metadata_table (" +
                            "vibeArtistId TEXT NOT NULL PRIMARY KEY, " +
                            "spotifyArtistId TEXT, " +
                            "debutDate TEXT, " +
                            "yearsOfActivity TEXT, " +
                            "agency TEXT, " +
                            "biography TEXT, " +
                            "images TEXT, " +
                            "members TEXT, " +
                            "activity TEXT)"
            );
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE favorites_table ADD COLUMN vocalists TEXT");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE favorites_table ADD COLUMN vibeTrackId TEXT");
            db.execSQL("ALTER TABLE settings_table ADD COLUMN trackIdInputPrefersNumeric INTEGER NOT NULL DEFAULT 1");
        }
    };

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE favorites_table ADD COLUMN trackNameKr TEXT");
            database.execSQL("ALTER TABLE favorites_table ADD COLUMN lyrics TEXT");
            database.execSQL("ALTER TABLE favorites_table ADD COLUMN lyricists TEXT");
            database.execSQL("ALTER TABLE favorites_table ADD COLUMN composers TEXT");
        }
    };



}
