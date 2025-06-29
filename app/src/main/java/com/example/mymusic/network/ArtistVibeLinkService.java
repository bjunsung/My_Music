package com.example.mymusic.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.example.mymusic.model.TrackMetadata; // 이 import는 필요 없을 수 있습니다.
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ArtistVibeLinkService {
    private static final String TAG = "ArtistVibeLinkService";
    private static final int PER_ARTIST_TIMEOUT_MS = 5000; // 타임아웃 시간 단축 권장

    public interface ArtistLinksCallback {
        void onComplete(List<List<String>> updatedVocalistList);
        void onError(String reason);
    }

    // 클래스 멤버로 상태를 관리합니다. static을 유지하되, 로직을 단순화합니다.
    private static WebView webView;
    private static List<List<String>> masterVocalistList;
    private static Queue<String> namesToFindQueue;
    private static ArtistLinksCallback finalCallback;
    private static final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private static final ArtistVibeLinkBridge bridge = new ArtistVibeLinkBridge();

    public static void fetchAllLinks(WebView wv, List<List<String>> vocalistList, String url, ArtistLinksCallback callback) {
        // --- 1. 초기화 ---
        // 진행 중인 타임아웃이 있다면 모두 취소
        timeoutHandler.removeCallbacksAndMessages(null);

        webView = wv;
        masterVocalistList = vocalistList;
        finalCallback = callback;
        namesToFindQueue = new LinkedList<>();

        if (url == null || url.isEmpty()){
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

        // --- 2. WebView 설정 및 페이지 로딩 (단 한번만 실행) ---
        new Handler(Looper.getMainLooper()).post(() -> {
            setupWebViewAndBridge();
            webView.loadUrl(url);
        });
    }

    private static void setupWebViewAndBridge() {
        // 브릿지 리스너 설정: 모든 검색 과정에서 재사용됩니다.
        bridge.setListener(new ArtistVibeLinkBridge.OnLinkListener() {
            @Override
            public void onLinkFound(String name, String link) {
                handleSearchResult(name, link);
            }

            @Override
            public void onLinkNotFound(String name) {
                handleSearchResult(name, null); // 링크가 없으면 null로 처리
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(bridge, "AndroidBridge");

        // WebChromeClient는 한 번만 설정해도 됩니다.
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d("WebViewConsole", cm.message() + " -- From line " + cm.lineNumber());
                return true;
            }
        });

        // WebViewClient도 한 번만 설정합니다.
        webView.setWebViewClient(new WebViewClient() {
            private boolean isPageLoaded = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                // onPageFinished가 여러 번 호출될 수 있으므로, 한 번만 실행하도록 플래그 사용
                if (!isPageLoaded) {
                    isPageLoaded = true;
                    Log.d(TAG, "Page finished loading. Starting to process the queue.");
                    // 페이지가 완전히 로드된 후, 큐 처리를 시작합니다.
                    searchNextArtistInQueue();
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                timeoutHandler.removeCallbacksAndMessages(null); // 진행중인 타임아웃 취소
                finalCallback.onError("웹 페이지 오류: " + description);
            }
        });
    }

    // 다음 아티스트 검색을 시작하는 메소드
    private static void searchNextArtistInQueue() {
        timeoutHandler.removeCallbacksAndMessages(null); // 이전 타임아웃 취소

        if (namesToFindQueue.isEmpty()) {
            Log.d(TAG, "All vocalists processed. Finishing.");
            new Handler(Looper.getMainLooper()).post(() -> finalCallback.onComplete(masterVocalistList));
            return;
        }

        String currentTargetName = namesToFindQueue.poll();
        Log.d(TAG, "Now processing: " + currentTargetName);

        // 새 검색을 시작하기 전에 브릿지 상태 초기화
        bridge.reset();

        // 타임아웃 설정
        timeoutHandler.postDelayed(() -> {
            Log.w(TAG, "TIMEOUT for " + currentTargetName + ". Assuming no link found.");
            handleSearchResult(currentTargetName, null);
        }, PER_ARTIST_TIMEOUT_MS);

        // 자바스크립트 코드 생성 및 실행
        String jsCode = createJavascriptCode(currentTargetName);
        new Handler(Looper.getMainLooper()).post(() -> webView.evaluateJavascript(jsCode, null));
    }

    // 검색 결과를 처리하고 다음 검색으로 넘어가는 공통 메소드
    private static void handleSearchResult(String name, String link) {
        new Handler(Looper.getMainLooper()).post(() -> {
            timeoutHandler.removeCallbacksAndMessages(null); // 성공/실패 시 타임아웃 즉시 취소

            String artistId = null;
            if (link != null && !link.isEmpty()) {
                String[] parts = link.split("/");
                if (parts.length > 0) {
                    artistId = parts[parts.length - 1];
                }
            }

            if (artistId != null) {
                Log.i(TAG, "SUCCESS for " + name + " -> Extracted ID: " + artistId);
            } else {
                Log.w(TAG, "NOT FOUND for " + name);
            }

            updateMasterList(name, artistId);
            searchNextArtistInQueue(); // 다음 아티스트 검색 시작
        });
    }

    // 아래 두 메소드는 수정할 필요가 없습니다.
    private static void updateMasterList(String name, String linkId) {
        for (List<String> pair : masterVocalistList) {
            if (pair.get(0).equals(name)) {
                pair.set(1, linkId);
                break;
            }
        }
    }

    private static String createJavascriptCode(String fullNameToSearch) {
        // 이 메소드의 내용은 이전과 동일하게 유지됩니다.
        // ... (이전 코드와 동일한 JS 코드 문자열 반환)
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