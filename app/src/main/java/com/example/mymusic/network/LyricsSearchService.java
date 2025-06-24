package com.example.mymusic.network;

import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.mymusic.model.TrackMetadata;

public class LyricsSearchService {

    public interface MetadataCallback {
        void onSuccess(TrackMetadata metadata);
        void onFailure(String reason);
    }

    public static void fetchMetadata(WebView webView, String trackId, MetadataCallback callback) {
        LyricsBridge bridge = new LyricsBridge();
        bridge.setListener(callback::onSuccess);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(bridge, "AndroidBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(
                        "(function waitForLyrics(retries = 10) {\n" +
                                "    const check = () => {\n" +
                                "        let paragraphs = document.querySelectorAll('div.lyrics p');\n" +
                                "        if (paragraphs.length > 0) {\n" +
                                "            let text = Array.from(paragraphs).map(p => p.innerText).join('\\n');\n" +

                                "            let rawTitle = document.querySelector('.title')?.innerText || '';\n" +
                                "            let title = rawTitle.replace(/^곡명\\s*/, '').trim();\n" +

                                "            function extractCredits(label) {\n" +
                                "                return Array.from(document.querySelectorAll('div.item'))\n" +
                                "                    .filter(div => div.innerText.includes(label))\n" +
                                "                    .flatMap(div => Array.from(div.querySelectorAll('a.song_info_artist')))\n" +
                                "                    .map(a => a.innerText.trim());\n" +
                                "            }\n" +

                                "            let lyricists = extractCredits('작사');\n" +
                                "            let composers = extractCredits('작곡');\n" +

                                "            if (!window._LYRICS_SENT && text.trim().length > 0 && typeof AndroidBridge !== 'undefined') {\n" +
                                "                AndroidBridge.receiveMetadata(JSON.stringify({\n" +
                                "                    title: title,\n" +
                                "                    lyrics: text,\n" +
                                "                    lyricists: lyricists,\n" +
                                "                    composers: composers\n" +
                                "                }));\n" +
                                "                window._LYRICS_SENT = true;\n" +
                                "            }\n" +
                                "        } else if (retries > 0) {\n" +
                                "            setTimeout(() => waitForLyrics(retries - 1), 500);\n" +
                                "        }\n" +
                                "    };\n" +
                                "    check();\n" +
                                "})()", null
                );
            }


            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                callback.onFailure(description);
            }
        });

        String fullUrl = trackId.startsWith("http") ? trackId : "https://vibe.naver.com/track/" + trackId;
        webView.loadUrl(fullUrl);
    }
}
