package com.example.mymusic.data.repository;

import android.content.Context;

import com.example.mymusic.data.local.AppDatabase;
import com.example.mymusic.data.local.SettingDao;


public class SettingRepository {
    private final SettingDao settingDao;

    public SettingRepository(Context context){
        AppDatabase db = AppDatabase.getInstance(context);
        settingDao = db.settingDao();
    }

    public int getMaxSearchedTracks() {
        return settingDao.getMaxSearchedTracks();
    }

    public boolean setMaxSearchedTracks(int value) {
        if (5 <= value && value <=50) {
            int result = settingDao.updateMaxSearchedTracks(value);
            return result > 0;
        }
        return false;
    }

    public int getMaxSearchedArtists() {
        return settingDao.getMaxSearchedArtists();
    }

    public boolean setMaxSearchedArtists(int value) {
        if (5 <= value && value <=50) {
            int result = settingDao.updateMaxSearchedArtists(value);
            return result > 0;
        }
        return false;
    }

    public int getMaxSearchedAlbumsByArtist() {
        return settingDao.getMaxSearchedAlbumsByArtist();
    }

    public boolean setMaxSearchedAlbumsByArtist(int value) {
        if (5 <= value && value <=50) {
            int result = settingDao.updateMaxSearchedAlbumsByArtist(value);
            return result > 0;
        }
        return false;
    }

    public boolean setNumericPreference(boolean numeric){
        int result = settingDao.updateNumericPreference(numeric);
        return result > 0;
    }

    public boolean getNumericPreference(){
        return settingDao.getNumeriPadPreference();
    }

}