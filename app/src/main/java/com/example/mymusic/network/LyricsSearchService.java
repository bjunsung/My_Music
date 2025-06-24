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
                        "(function waitForLyrics() {\n" +
                                "  const check = () => {\n" +
                                "    let paragraphs = document.querySelectorAll('div.lyrics p');\n" +
                                "    if (paragraphs && paragraphs.length > 0) {\n" +
                                "      let text = Array.from(paragraphs).map(p => p.innerText).join('\\n');\n" +
                                "      if (typeof AndroidBridge !== 'undefined' && AndroidBridge.receiveLyrics) {\n" +
                                "        AndroidBridge.receiveLyrics(text);\n" +
                                "      } else {\n" +
                                "        console.log('🔴 AndroidBridge not ready');\n" +
                                "        setTimeout(check, 100);\n" +
                                "      }\n" +
                                "    } else {\n" +
                                "      console.log('⏳ Waiting for lyrics DOM...');\n" +
                                "      setTimeout(check, 100);\n" +
                                "    }\n" +
                                "  };\n" +
                                "  check();\n" +
                                "})();", null
                );
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
