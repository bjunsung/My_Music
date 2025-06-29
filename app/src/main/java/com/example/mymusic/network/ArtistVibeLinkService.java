package com.example.mymusic.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.example.mymusic.model.TrackMetadata;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ArtistVibeLinkService {
    private static final String TAG = "ArtistVibeLinkService";
    private static final int PER_ARTIST_TIMEOUT_MS = 15000;

    public interface ArtistLinksCallback {
        void onComplete(List<List<String>> updatedVocalistList);
        void onError(String reason);
    }

    private static WebView webView;
    private static String artistPageUrl;
    private static List<List<String>> masterVocalistList;
    private static Queue<String> namesToFindQueue;
    private static ArtistLinksCallback finalCallback;
    private static Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private static ArtistVibeLinkBridge bridge;


    public static void fetchAllLinks(WebView wv, List<List<String>> vocalistList, String url, ArtistLinksCallback callback) {
        // ... 이전과 동일한 초기화 로직 ...
        webView = wv;
        artistPageUrl = url;
        masterVocalistList = vocalistList;
        finalCallback = callback;
        namesToFindQueue = new LinkedList<>();

        if (artistPageUrl == null || artistPageUrl.isEmpty()){
            Log.w(TAG, "Artist page URL is empty. Skipping link fetch.");
            callback.onComplete(vocalistList);
            return;
        }

        for (List<String> pair : masterVocalistList) {
            if (pair.size() >= 2 && pair.get(1) == null) {
                namesToFindQueue.add(pair.get(0));
            }
        }

        if (namesToFindQueue.isEmpty()) {
            Log.d(TAG, "No vocalists need a link update.");
            callback.onComplete(masterVocalistList);
            return;
        }

        Log.d(TAG, "Starting link fetch for " + namesToFindQueue.size() + " vocalists.");
        setupWebViewBasicSettings();
        processNextInQueue();
    }

    private static void processNextInQueue() {
        timeoutHandler.removeCallbacksAndMessages(null);

        if (namesToFindQueue.isEmpty()) {
            Log.d(TAG, "All vocalists processed. Finishing.");
            new Handler(Looper.getMainLooper()).post(() -> finalCallback.onComplete(masterVocalistList));
            return;
        }

        String currentTargetName = namesToFindQueue.poll();
        Log.d(TAG, "Now processing: " + currentTargetName);

        new Handler(Looper.getMainLooper()).post(() -> {
            bridge = new ArtistVibeLinkBridge();
            webView.addJavascriptInterface(bridge, "AndroidBridge");

            bridge.setListener(new ArtistVibeLinkBridge.OnLinkListener() {
                @Override
                public void onLinkFound(String name, String link) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        timeoutHandler.removeCallbacksAndMessages(null);

                        // [핵심 수정] 숫자만 추출하는 로직 추가
                        String artistId = null;
                        if (link != null && !link.isEmpty()) {
                            // link 문자열이 "/artist/155276" 같은 형태라고 가정
                            String[] parts = link.split("/");
                            if (parts.length > 0) {
                                // 가장 마지막 부분이 숫자 ID
                                artistId = parts[parts.length - 1];
                            }
                        }

                        Log.i(TAG, "SUCCESS for " + name + " -> Raw Link: " + link + ", Extracted ID: " + artistId);
                        updateMasterList(name, artistId); // 추출된 ID (또는 null)로 업데이트
                        processNextInQueue();
                    });
                }

                @Override
                public void onLinkNotFound(String name) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        timeoutHandler.removeCallbacksAndMessages(null);
                        Log.w(TAG, "CONFIRMED NOT FOUND for " + name + ". Moving to next.");
                        processNextInQueue();
                    });
                }
            });

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    String jsCode = createJavascriptCode(currentTargetName);
                    view.evaluateJavascript(jsCode, null);
                }
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Log.e(TAG, "WebView Error: " + description);
                    timeoutHandler.removeCallbacksAndMessages(null);
                    finalCallback.onError("웹 페이지 오류: " + description);
                }
            });

            timeoutHandler.postDelayed(() -> {
                Log.w(TAG, "TIMEOUT for " + currentTargetName + ". Assuming no link found.");
                processNextInQueue();
            }, PER_ARTIST_TIMEOUT_MS);

            webView.loadUrl(artistPageUrl);
        });
    }

    private static void setupWebViewBasicSettings() {
        new Handler(Looper.getMainLooper()).post(() -> {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(ConsoleMessage cm) {
                    Log.d("WebViewConsole", cm.message() + " -- From line " + cm.lineNumber());
                    return true;
                }
            });
        });
    }

    private static void updateMasterList(String name, String linkId) {
        for (List<String> pair : masterVocalistList) {
            if (pair.get(0).equals(name)) {
                pair.set(1, linkId);
                break;
            }
        }
    }

    private static String createJavascriptCode(String fullNameToSearch) {
        String containerSelector = "dl.info_member";
        String linkSelector = "dd.info_desc a";

        return "(function() {" +
                "  function findLink(retries) {" +
                "    const searchName = \"" + fullNameToSearch.replace("\"", "\\\"") + "\";" +
                "    console.log('Searching for ' + searchName + '. Retries left: ' + retries);" +
                "    const container = document.querySelector('" + containerSelector + "');" +
                "    if (container && container.innerText.trim() !== '') {" +
                "      console.log('Member container is ready.');" +
                "      let links = Array.from(container.querySelectorAll('" + linkSelector + "'));" +
                "      let match = links.find(a => a.innerText.trim() === searchName);" +
                "      if (match) {" +
                "        let link = match.getAttribute('href');" +
                "        console.log('SUCCESS: Found a match for ' + searchName + '. Link: ' + link);" +
                "        AndroidBridge.receiveMetadata(JSON.stringify({ name: searchName, link: link }));" +
                "        return;" +
                "      } else {" +
                "        console.log('FAILURE: Container is ready, but name not found.');" +
                "        AndroidBridge.reportNotFound(searchName);" +
                "        return;" +
                "      }" +
                "    }" +
                "    if (retries > 0) {" +
                "      console.log('Container not ready, retrying...');" +
                "      setTimeout(() => findLink(retries - 1), 500);" +
                "    } else {" +
                "      console.log('FAILURE: Container not found after all retries.');" +
                "      AndroidBridge.reportNotFound(searchName);" +
                "    }" +
                "  }" +
                "  findLink(20);" +
                "})();";
    }
}