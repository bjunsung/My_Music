package com.example.mymusic.util;

import android.content.Context;
import android.content.res.Configuration;

public class DarkModeUtils {
    public static boolean isDarkMode(Context context){
        boolean isDarkMode = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return isDarkMode;
    }
}
