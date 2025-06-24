package com.example.mymusic.network;

import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class LyricsSearchService {

    public interface LyricCallback{
        void onSuccess(String lyrics);
        void onFailure(String reason);
    }

    public static void fetchLyrics(Context context, WebView webView, String trackId, LyricCallback callback){
        LyricsBridge bridge = new LyricsBridge();
        bridge.setListener(callback::onSuccess);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        //bridge 연결
        webView.addJavascriptInterface(bridge, "AndroidBridge");

        //webViewClient 설정
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url){
                view.evaluateJavascript(
                        "(function waitForLyrics() {" +
                                "const check = () => {" +
                                "let paragraphs = document.querySelectorAll('div.lyrics p');" +
                                "if (paragraphs && paragraphs.length > 0) {" +
                                "let text = Array.from(paragraphs).map(p => p.innerText).join('\\n');" +
                                "if (typeof AndroidBridge !== 'undefined' && AndroidBridge.receiveLyrics) {" +
                                "if (!window._LYRICS_SENT && text.trim().length > 0) {" +
                                "AndroidBridge.receiveLyrics(text);" +
                                "window._LYRICS_SENT = true;" +
                                "}" +
                                "}" +
                                "} else {" +
                                "setTimeout(check, 100);" +
                                "}" +
                                "};" +
                                "check();" +
                                "})()", null);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl){
                callback.onFailure(description);
            }
        });

        String fullUrl = trackId.startsWith("http") ? trackId :  "https://vibe.naver.com/track/" + trackId;
        webView.loadUrl(fullUrl);
    }

}
