package com.example.mymusic.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "token_table")
public class Token {
    @PrimaryKey
    private int tokenId = 0;
    private String accessToken;
    public long expiresIn = 3600;
    public Token() {
        // Required by Room
    }
    public Token(String accessToken) {
        this.accessToken = accessToken;
    }
    public String getAccessToken() {
        return accessToken;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    public int getTokenId() {
        return tokenId;
    }
    public void setTokenId(int id){
        this.tokenId = id;
    }
}
