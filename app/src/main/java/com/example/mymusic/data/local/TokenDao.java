package com.example.mymusic.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface TokenDao
{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void setAccessToken(Token token);

    @Query(("SELECT * FROM token_table WHERE tokenId = 0 LIMIT 1"))
    Token getAccessToken();
}
