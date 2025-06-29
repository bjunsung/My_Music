// ArtistVibeLinkService.java
package com.example.mymusic.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.mymusic.model.TrackMetadata;

import java.util.List;

public class ArtistVibeLinkService {

    public interface MetadataCallback {
        void onSuccess(TrackMetadata metadata);
        void onFailure(String reason);
    }

    public static void fetchMetadata(WebView webView, List<List<String>> vocalistList, String artistPageUrl, MetadataCallback callback) {
        if (vocalistList == null || vocalistList.isEmpty()) {
            callback.onFailure("보컬리스트가 비어있습니다");
            return;
        }

        String targetName = null;
        String baseName = null;
        for (List<String> pair : vocalistList) {
            if (pair.size() >= 2 && pair.get(1) == null) {
                targetName = pair.get(0);
                baseName = targetName.split("[()\\(\\)]")[0];
                break;
            }
        }

        if (targetName == null) {
            callback.onFailure("업데이트할 대상이 없습니다");
            return;
        }

        ArtistVibeLinkBridge bridge = new ArtistVibeLinkBridge();
        bridge.setListener(updatedList -> {
            TrackMetadata result = new TrackMetadata();
            result.setVocalists(updatedList);
            new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(result));
        }, vocalistList);

        String finalTargetName = targetName;
        String finalBaseName = baseName;

        new Handler(Looper.getMainLooper()).post(() -> {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.addJavascriptInterface(bridge, "AndroidBridge");

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    String jsCode = "(function waitForArtistLink(retries = 10) {" +
                            "  const check = () => {" +
                            "    let links = Array.from(document.querySelectorAll('a'));" +
                            "    console.log('Total links found: ' + links.length);" +
                            "    let match = links.find(a => a.innerText.trim().includes(\"" + finalBaseName + "\"));" +
                            "    let link = match ? match.getAttribute('href') : null;" +
                            "    console.log('Match found for " + finalBaseName + ": ' + link);" +
                            "    if (!window._LINK_SENT && typeof AndroidBridge !== 'undefined' && link !== null) {" +
                            "      AndroidBridge.receiveMetadata(JSON.stringify({ name: \"" + finalTargetName + "\", link: link }));" +
                            "      window._LINK_SENT = true;" +
                            "    }" +
                            "    if (retries > 0 && window._LINK_SENT !== true) {" +
                            "      setTimeout(() => waitForArtistLink(retries - 1), 500);" +
                            "    }" +
                            "  };" +
                            "  check();" +
                            "})();";
                    view.evaluateJavascript(jsCode, null);
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    callback.onFailure("웹 페이지 오류: " + description);
                }
            });

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    Log.d("WebViewConsole", consoleMessage.message());
                    return true;
                }
            });

            webView.loadUrl(artistPageUrl);
        });
    }
}
