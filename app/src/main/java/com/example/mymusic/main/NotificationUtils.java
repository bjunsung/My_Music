package com.example.mymusic.main;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public final class NotificationUtils {
    public static final String CHANNEL_ID = "playback_v2";

    public static void createPlaybackChannel(Context ctx) {
        if (Build.VERSION.SDK_INT < 26) return;

        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return;

        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "음악 재생", NotificationManager.IMPORTANCE_HIGH   );
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
    }
}
