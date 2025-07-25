package com.example.mymusic.data.local;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Migrations {
    public static final Migration MIGRATION_15_16 = new Migration(15, 16) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // trackIds가 빈 문자열("") 또는 "[]" 인 경우 NULL로 변경
            db.execSQL(
                    "UPDATE playlist_table " +
                            "SET trackIds = NULL " +
                            "WHERE trackIds IS NOT NULL " +
                            "AND (TRIM(trackIds) = '' OR TRIM(trackIds) = '[]')"
            );
        }
    };
    public static final Migration MIGRATION_14_15 = new Migration(14, 15) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // 1. 새 테이블 생성 (trackIds를 NULL 허용으로 변경)
            db.execSQL("CREATE TABLE playlist_table_new (" +
                    "playlistId TEXT NOT NULL PRIMARY KEY, " +
                    "playlistName TEXT NOT NULL, " +
                    "trackIds TEXT, " +  // NULL 허용
                    "totalDurationSec INTEGER NOT NULL, " +
                    "createdDate TEXT NOT NULL, " +
                    "lastPlayedTimeMs INTEGER, " +
                    "playCount INTEGER NOT NULL" +
                    ")");

            // 2. 기존 데이터 복사
            db.execSQL("INSERT INTO playlist_table_new " +
                    "(playlistId, playlistName, trackIds, totalDurationSec, createdDate, lastPlayedTimeMs, playCount) " +
                    "SELECT playlistId, playlistName, " +
                    "CASE WHEN trackIds = '[]' OR trackIds = '' THEN NULL ELSE trackIds END, " + // []는 NULL로 변환
                    "totalDurationSec, createdDate, lastPlayedTimeMs, playCount " +
                    "FROM playlist_table");

            // 3. 기존 테이블 삭제
            db.execSQL("DROP TABLE playlist_table");

            // 4. 새 테이블 이름 변경
            db.execSQL("ALTER TABLE playlist_table_new RENAME TO playlist_table");
        }
    };

    static final Migration MIGRATION_13_14 = new Migration(13, 14) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // 기본 데이터가 없으면 삽입
            db.execSQL("INSERT OR IGNORE INTO playlist_table " +
                    "(playlistId, playlistName, trackIds, totalDurationSec, createdDate, lastPlayedTimeMs, playCount) " +
                    "VALUES ('sys_recently_played', '최근 재생한 음악', '[]', 0, DATE('now'), null, 0)");
        }
    };

    static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // 1. 임시 테이블 생성 (NOT NULL 제약 맞추기)
            db.execSQL("CREATE TABLE IF NOT EXISTS playlist_table_new (" +
                    "playlistId TEXT NOT NULL PRIMARY KEY, " +
                    "playlistName TEXT NOT NULL, " +              // ✅ NOT NULL 추가
                    "trackIds TEXT NOT NULL, " +                   // ✅ NOT NULL 추가
                    "totalDurationSec INTEGER NOT NULL, " +
                    "createdDate TEXT NOT NULL, " +                // ✅ NOT NULL 추가
                    "lastPlayedTimeMs INTEGER, " +
                    "playCount INTEGER NOT NULL)");

            // 2. 기존 데이터 복사
            db.execSQL("INSERT INTO playlist_table_new " +
                    "(playlistId, playlistName, trackIds, totalDurationSec, createdDate, lastPlayedTimeMs, playCount) " +
                    "SELECT playlistId, " +
                    "IFNULL(playlistName, ''), " +                 // ✅ NULL 방지
                    "IFNULL(trackIds, '[]'), " +                   // ✅ NULL 방지
                    "totalDurationSec, " +
                    "IFNULL(createdDate, DATE('now')), " +         // ✅ NULL 방지
                    "NULL, " +
                    "playCount " +
                    "FROM playlist_table");

            // 3. 기존 테이블 삭제
            db.execSQL("DROP TABLE playlist_table");

            // 4. 새 테이블 이름 변경
            db.execSQL("ALTER TABLE playlist_table_new RENAME TO playlist_table");
        }
    };


    static final Migration  MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE favorites_table ADD COLUMN lastPlayedDate TEXT");
        }
    };
    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.beginTransaction();
            try {
                // 1) 새 테이블 생성
                db.execSQL(
                        "CREATE TABLE favorites_table_new (" +
                                "trackId TEXT NOT NULL PRIMARY KEY, " +
                                "albumId TEXT, artistId TEXT, trackName TEXT, albumName TEXT, artistName TEXT, " +
                                "artworkUrl TEXT, releaseDate TEXT, durationMs TEXT, addedDate TEXT, " +
                                "vibeTrackId TEXT, trackNameKr TEXT, lyrics TEXT, " +
                                "vocalists TEXT, lyricists TEXT, composers TEXT, primaryColor INTEGER, " +
                                "audioUri TEXT, playCount INTEGER, " +
                                "playCountByDay TEXT, firstCountedDate TEXT)"
                );

                // 2) 기존 데이터 읽기
                Cursor cursor = db.query("SELECT * FROM favorites_table");
                Gson gson = new Gson();
                Type listType = new TypeToken<List<List<String>>>() {}.getType();

                int resetCount = 0; // 초기화된 데이터 수
                if (cursor.moveToFirst()) {
                    do {
                        // 매 레코드마다 새 객체 생성
                        ContentValues values = new ContentValues();
                        String newJson = "{}";
                        String firstDate = null;

                        int trackIdIdx = cursor.getColumnIndex("trackId");
                        String trackId = (trackIdIdx != -1) ? cursor.getString(trackIdIdx) : null;

                        try {
                            // 2-1) 기존 컬럼 복사
                            String[] columns = cursor.getColumnNames();
                            for (String column : columns) {
                                if (column.equals("playCountByDay") || column.equals("firstCountedDate")) continue;
                                int idx = cursor.getColumnIndex(column);
                                if (idx == -1) {
                                    values.putNull(column);
                                    continue;
                                }
                                switch (cursor.getType(idx)) {
                                    case Cursor.FIELD_TYPE_STRING:
                                        values.put(column, cursor.getString(idx));
                                        break;
                                    case Cursor.FIELD_TYPE_INTEGER:
                                        values.put(column, cursor.getInt(idx));
                                        break;
                                    default:
                                        values.putNull(column);
                                }
                            }

                            // 2-2) playCountByDay 변환
                            int playIdx = cursor.getColumnIndex("playCountByDay");
                            String oldJson = (playIdx != -1) ? cursor.getString(playIdx) : null;

                            int playCountIdx = cursor.getColumnIndex("playCount");
                            int originalPlayCount = (playCountIdx != -1) ? cursor.getInt(playCountIdx) : 0;

                            if (oldJson != null && !oldJson.equals("[]")) {
                                List<List<String>> oldList = gson.fromJson(oldJson, listType);
                                if (oldList != null && !oldList.isEmpty()) {
                                    Map<String, Integer> newMap = new LinkedHashMap<>();
                                    for (List<String> pair : oldList) {
                                        if (pair.size() == 2) {
                                            try {
                                                newMap.put(pair.get(0), Integer.parseInt(pair.get(1)));
                                            } catch (NumberFormatException e) {
                                                android.util.Log.w("Migration9_10", "Invalid count for trackId=" + trackId + " pair=" + pair);
                                            }
                                        }
                                    }
                                    int sum = newMap.values().stream().mapToInt(Integer::intValue).sum();
                                    if (sum == originalPlayCount) {
                                        // 합계 일치 → 정상 변환
                                        newJson = gson.toJson(newMap);
                                        firstDate = newMap.isEmpty() ? null : Collections.min(newMap.keySet());
                                        values.put("playCount", originalPlayCount);
                                    } else {
                                        // 합계 불일치 → 초기화
                                        values.put("playCount", 0);
                                        newJson = "{}";
                                        firstDate = null;
                                        resetCount++;
                                        android.util.Log.w("Migration9_10", "Resetting trackId=" + trackId + " (sum=" + sum + ", playCount=" + originalPlayCount + ")");
                                    }
                                } else {
                                    // 빈 리스트 → 0으로 초기화
                                    values.put("playCount", 0);
                                }
                            } else {
                                // 원래 빈 리스트 → 0으로 초기화
                                values.put("playCount", 0);
                            }

                            values.put("playCountByDay", newJson);
                            values.put("firstCountedDate", firstDate);

                        } catch (Exception e) {
                            // 파싱 실패 → 초기화
                            android.util.Log.e("Migration9_10", "Migration failed for trackId " + trackId + ", resetting", e);
                            values.put("playCountByDay", "{}");
                            values.put("playCount", 0);
                            values.putNull("firstCountedDate");
                            resetCount++;
                        }

                        db.insert("favorites_table_new", SQLiteDatabase.CONFLICT_REPLACE, values);
                    } while (cursor.moveToNext());
                }
                cursor.close();

                // 3) 기존 테이블 교체
                db.execSQL("DROP TABLE favorites_table");
                db.execSQL("ALTER TABLE favorites_table_new RENAME TO favorites_table");

                // 요약 로그
                android.util.Log.i("Migration9_10", "Migration completed. Reset entries: " + resetCount);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    };


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
