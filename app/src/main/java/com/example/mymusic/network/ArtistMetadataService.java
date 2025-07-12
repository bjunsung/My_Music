package com.example.mymusic.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.mymusic.model.ArtistMetadata;

public class ArtistMetadataService {

    private static final String TAG = "ArtistMetadataService";

    public interface MetadataCallback {
        void onSuccess(ArtistMetadata metadata);
        void onFailure(String reason);
    }

    public static void fetchMetadata(WebView webview, String artistId, MetadataCallback callback) {
        Log.d(TAG, "fetchMetadata() called with artistId = " + artistId);

        final boolean[] callbackInvoked = {false};

        final ArtistMetadataBridge bridge = new ArtistMetadataBridge();
        bridge.setListener(new ArtistMetadataBridge.OnMetadataListener() {
            @Override
            public void onSuccess(ArtistMetadata metadata) {
                Log.d(TAG, "bridge - onSuccess() called");
                if (!callbackInvoked[0]) {
                    callbackInvoked[0] = true;
                    callback.onSuccess(metadata);
                } else {
                    Log.d(TAG, "onSuccess() skipped due to already invoked");
                }
            }

            @Override
            public void onFailure(String reason) {
                Log.d(TAG, "onFailure() called with reason: " + reason);
                if (!callbackInvoked[0]) {
                    callbackInvoked[0] = true;
                    callback.onFailure(reason);
                } else {
                    Log.d(TAG, "onFailure() skipped due to already invoked");
                }
            }
        });

        new Handler(Looper.getMainLooper()).post(() -> {
            Log.d(TAG, "WebView configuration started");

            webview.getSettings().setJavaScriptEnabled(true);
            webview.getSettings().setDomStorageEnabled(true);
            webview.addJavascriptInterface(bridge, "AndroidBridge");

            webview.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    Log.d(TAG, "WebView finished loading: " + url);

                    String jsCode = "(function waitForLyrics() {\n" +
                            "    const check = (retries) => {\n" +
                            "        let paragraphs = document.querySelectorAll('dl.info');\n" +
                            "        if (paragraphs.length > 0 && paragraphs[0].innerText.trim() !== '') {\n" +
                            "            console.log('[JS] DOM is ready, extracting info');\n" +
                            "            function extractInfo(label, asArray = false) {\n" +
                            "                const dlList = Array.from(document.querySelectorAll('dl.info'));\n" +
                            "                const match = dlList.find(dl => {\n" +
                            "                    const dt = dl.querySelector('.info_term');\n" +
                            "                    return dt && dt.innerText.trim() === label;\n" +
                            "                });\n" +
                            "                const dd = match?.querySelector('.info_desc');\n" +
                            "                if (!dd) return asArray ? [] : null;\n" +
                            "                const text = dd.innerText.trim();\n" +
                            "                if (asArray) {\n" +
                            "                    return text.split(',').map(s => s.trim()).filter(Boolean);\n" +
                            "                }\n" +
                            "                return text;\n" +
                            "            }\n" +
                            "            let debutDate = extractInfo('데뷔');\n" +
                            "            let yearsOfActivity = extractInfo('연대', true);\n" +
                            "            let agency = extractInfo('소속', true);\n" +
                            "            const bioElement = document.querySelector('p.biography');\n" +
                            "            let biography = bioElement ? bioElement.innerText.trim() : null;\n" +
                            "            let images = Array.from(document.querySelectorAll('img.img_thumb'))\n" +
                            "                .map(img => img.src)\n" +
                            "                .filter(Boolean);\n" +
                            "            let members = Array.from(document.querySelectorAll('.member_row')).map(row => {\n" +
                            "                const linkTag = row.querySelector('a.member_link');\n" +
                            "                const href = linkTag?.getAttribute('href') || '';\n" +
                            "                const artistIdMatch = href.match(/\\/artist\\/(\\d+)/);\n" +
                            "                const artistId = artistIdMatch ? artistIdMatch[1] : null;\n" +
                            "                const name = row.querySelector('.member_info strong')?.innerText?.trim() || null;\n" +
                            "                return [name, artistId, null];\n" +
                            "            }).filter(item => item[0] != null && item[1] != null);\n" +
                            "            let activity = Array.from(document.querySelectorAll('.info_activity a')).map(a => {\n" +
                            "                const name = a.innerText.trim();\n" +
                            "                const href = a.getAttribute('href') || '';\n" +
                            "                const idMatch = href.match(/\\/artist\\/(\\d+)/);\n" +
                            "                const artistId = idMatch ? idMatch[1] : null;\n" +
                            "                return [name, artistId];\n" +
                            "            }).filter(item => item[0] && item[1]);\n" +
                            "            if (typeof AndroidBridge !== 'undefined') {\n" +
                            "                console.log('[JS] Sending metadata to AndroidBridge');\n" +
                            "                AndroidBridge.receiveMetadata(JSON.stringify({\n" +
                            "                    debutDate: debutDate,\n" +
                            "                    yearsOfActivity: yearsOfActivity,\n" +
                            "                    agency: agency,\n" +
                            "                    biography: biography,\n" +
                            "                    images: images,\n" +
                            "                    members: members,\n" +
                            "                    activity: activity\n" +
                            "                }));\n" +
                            "            } else {\n" +
                            "                console.log('[JS] AndroidBridge is undefined');\n" +
                            "            }\n" +
                            "        } else if (retries > 0) {\n" +
                            "            console.log('[JS] Retry check: ' + retries);\n" +
                            "            setTimeout(() => check(retries-1), 500);\n" +
                            "        } else {\n" +
                            "            console.log('[JS] Retry failed: No metadata found');\n" +
                            "            if (typeof AndroidBridge !== 'undefined') {\n" +
                            "               AndroidBridge.reportFailure('페이지에서 정보를 찾을 수 없습니다.');\n" +
                            "            }\n" +
                            "        }\n" +
                            "    };\n" +
                            "    check(20);\n" +
                            "})()";

                    Log.d(TAG, "Evaluating JavaScript...");
                    view.evaluateJavascript(jsCode, null);
                }
            });

            String fullUrl = artistId.startsWith("http") ? artistId + "/detail" : "https://vibe.naver.com/artist/" + artistId + "/detail";
            Log.d(TAG, "Loading URL: " + fullUrl);
            webview.loadUrl(fullUrl);
        });
    }
}
