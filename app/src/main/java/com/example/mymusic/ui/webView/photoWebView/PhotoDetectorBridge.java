package com.example.mymusic.ui.webView.photoWebView;

// PhotoDetectorBridge.java
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;

public class PhotoDetectorBridge {

    public interface Listener {
        void onPhotoViewerOpened();
        void onPhotoViewerClosed();
    }

    private Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @JavascriptInterface
    public void photoViewerDetected(boolean isVisible) {
        if (listener == null) return;

        mainHandler.post(() -> {
            if (isVisible) {
                listener.onPhotoViewerOpened();
            } else {
                listener.onPhotoViewerClosed();
            }
        });
    }
}
