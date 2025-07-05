package com.example.mymusic.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.example.mymusic.model.TrackMetadata;

public class LyricsSearchService {

    public interface MetadataCallback {
        void onSuccess(TrackMetadata metadata);
        void onFailure(String reason);
    }

    private static final int TIMEOUT_MS = 10000; // 10초 타임아웃

    public static void fetchMetadata(WebView webView, String trackId, MetadataCallback callback) {


        // [추가됨] 타임아웃 핸들러
        final Handler timeoutHandler = new Handler(Looper.getMainLooper());

        // final 변수로 만들어 콜백에서 접근 가능하게 함
        final boolean[] callbackInvoked = {false};

        final Runnable timeoutRunnable = () -> {
            if (!callbackInvoked[0]) {
                callbackInvoked[0] = true;
                Log.e("LyricsSearchService", "Operation timed out.");
                callback.onFailure("타임아웃: VIBE 페이지에서 응답이 없습니다.");
            }
        };

        final LyricsBridge bridge = new LyricsBridge();

        // [수정됨] 새로운 브릿지 리스너 설정
        bridge.setListener(new LyricsBridge.OnMetadataListener() {
            @Override
            public void onSuccess(TrackMetadata metadata) {
                if (!callbackInvoked[0]) {
                    Log.d("LyricsSearchService", "first callback invoked");
                    callbackInvoked[0] = true;
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    callback.onSuccess(metadata);
                }
            }

            @Override
            public void onFailure(String reason) {
                if (!callbackInvoked[0]) {
                    callbackInvoked[0] = true;
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    callback.onFailure(reason);
                }
            }
        });

        // UI 스레드에서 WebView 설정 실행
        new Handler(Looper.getMainLooper()).post(() -> {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.addJavascriptInterface(bridge, "AndroidBridge");

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    // [수정됨] 실패 보고 로직이 추가된 Javascript
                    // 보내주신 Javascript 코드 기반으로 수정
                    String jsCode = "(function waitForLyrics() {\n" +
                            "    const check = (retries = 100) => {\n" +
                            "        let paragraphs = document.querySelectorAll('div.lyrics p');\n" +
                            "        if (paragraphs.length > 0 && paragraphs[0].innerText.trim() !== '') {\n" +
                            "            let text = Array.from(paragraphs).map(p => p.innerText).join('\\n');\n" +
                            "            let rawTitle = document.querySelector('.title')?.innerText || '';\n" +
                            "            let title = rawTitle.replace(/^곡명\\s*/, '').trim();\n" +
                            "            let artistClass = document.querySelector('a.link_sub_title');\n" +
                            "            let link = artistClass ? artistClass.getAttribute('href') : null;\n" +
                            "            function extractCredits(label) {\n" +
                            "                const items = Array.from(document.querySelectorAll('div.item'))\n" +
                            "                    .filter(div => div.innerText.includes(label))\n" +
                            "                    .flatMap(div => Array.from(div.querySelectorAll('a.song_info_artist')))\n" +
                            "                    .map(a => a.innerText.trim());\n" +
                            "                return items.length > 0 ? items : null;\n" +
                            "            }\n" +
                            "            let vocalists = extractCredits('보컬');\n" +
                            "            let lyricists = extractCredits('작사');\n" +
                            "            let composers = extractCredits('작곡');\n" +
                            "            if (typeof AndroidBridge !== 'undefined') {\n" +
                            "                AndroidBridge.receiveMetadata(JSON.stringify({\n" +
                            "                    title: title,\n" +
                            "                    artistLink: link,\n" +
                            "                    lyrics: text,\n" +
                            "                    vocalists: vocalists,\n" +
                            "                    lyricists: lyricists,\n" +
                            "                    composers: composers\n" +
                            "                }));\n" +
                            "            }\n" +
                            "        } else if (retries > 0) {\n" +
                            "            setTimeout(() => check(retries-1), 100);\n" + // 자기 자신(check)을 다시 호출
                            "        } else {\n" +
                            "            // [추가됨] 재시도 모두 실패 시, 명시적으로 실패 보고\n" +
                            "            if (typeof AndroidBridge !== 'undefined') {\n" +
                            "               AndroidBridge.reportFailure('페이지에서 가사 정보를 찾을 수 없습니다.');\n" +
                            "            }\n" +
                            "        }\n" +
                            "    };\n" +
                            "    check();\n" +
                            "})()";
                    view.evaluateJavascript(jsCode, null);
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    if (!callbackInvoked[0]) {
                        callbackInvoked[0] = true;
                        timeoutHandler.removeCallbacks(timeoutRunnable);

                        String errorMessage = error.getDescription().toString();
                        callback.onFailure(errorMessage);
                    }
                }


            });

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    Log.d("WebViewConsole", consoleMessage.message());
                    if (consoleMessage.message().contains("Uncaught")) {
                        if (!callbackInvoked[0]) {
                            callbackInvoked[0] = true;
                            timeoutHandler.removeCallbacks(timeoutRunnable);
                            callback.onFailure("JavaScript 오류: " + consoleMessage.message());
                        }
                    }
                    return true;
                }
            });


            timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);
            String fullUrl = trackId.startsWith("http") ? trackId : "https://vibe.naver.com/track/" + trackId;
            webView.loadUrl(fullUrl);

        });
    }
}