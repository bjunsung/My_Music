package com.example.mymusic.ui.webView.lyricsSearch;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.mymusic.R;
import com.example.mymusic.model.TrackMetadata;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class LyricsSearchAutoFragment extends Fragment {
    private final String TAG = "LyricsSearchAuto";
    // --- UI 및 기본 상태 관련 변수 ---
    private WebView mainWebView; // 사용자가 보는 메인 WebView
    private Button importButton;
    private String trackName, albumName, artistName;
    private AutoLyricsBridge mainBridge; // 메인 WebView용 브릿지

    // --- 멤버 ID 스크래핑 전용 변수 ---
    private WebView scrapingWebView; // 데이터 추출 전용 보이지 않는 WebView
    private AutoLyricsBridge scrapingBridge;
    private TrackMetadata currentTrackMetadata;
    private Queue<String> memberNamesToFindQueue;
    private final Handler memberSearchTimeoutHandler = new Handler(Looper.getMainLooper());
    private static final int PER_MEMBER_TIMEOUT_MS = 7000;

    public static final String BUNDLE_KEY = "track_metadata";
    public static final String REQUEST_KEY = "auto_lyrics_search_key";

    // ✅ [복원됨] getArguments 코드
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(this, new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (mainWebView.canGoBack())
                            mainWebView.goBack();
                        else{
                            NavHostFragment.findNavController(LyricsSearchAutoFragment.this).popBackStack();
                        }
                    }
                });

        if (getArguments() != null) {
            trackName = getArguments().getString("track_name");
            albumName = getArguments().getString("album_name");
            artistName = getArguments().getString("artist_name");
            Log.d(TAG, "Arguments received: track=" + trackName + ", album=" + albumName + ", artist=" + artistName);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_web_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainWebView = view.findViewById(R.id.photo_webview);
        importButton = view.findViewById(R.id.import_button);

        setupMainWebView();
        loadInitialSearchPage();

        importButton.setText("가사 가져오기");

        importButton.setOnClickListener(v -> {
            Log.i(TAG, "IMPORT BUTTON CLICKED");
            mainWebView.evaluateJavascript(createExtractionJs(), null);
        });
    }

    /**
     * 사용자가 보는 메인 WebView를 설정합니다. (가사 추출까지 담당)
     */
    private void setupMainWebView() {
        Log.d(TAG, "setupMainWebView: Configuring main WebView for user interaction.");

        mainBridge = new AutoLyricsBridge();

        mainWebView.getSettings().setJavaScriptEnabled(true);
        mainWebView.getSettings().setDomStorageEnabled(true);
        mainWebView.getSettings().setBlockNetworkImage(false); // 사용자가 볼 때는 이미지 로드
        mainWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d("WebViewConsole-Main", cm.message());
                return true;
            }
        });

        mainBridge.setVisibilityListener(new AutoLyricsBridge.Listener() {
            @Override public void onLyricsOpened() { importButton.setVisibility(View.VISIBLE); }
            @Override public void onLyricsClosed() { importButton.setVisibility(View.GONE); }
        });

        mainBridge.setMetadataListener(new AutoLyricsBridge.OnMetadataListener() {
            @Override
            public void onSuccess(TrackMetadata metadata) {
                Log.i(TAG, "BRIDGE-SUCCESS: Lyrics metadata received.");
                currentTrackMetadata = metadata;
                startMemberIdSearchOnDedicatedWebView();
            }
            @Override
            public void onFailure(String reason) {
                Log.e(TAG, "BRIDGE-FAILURE: Lyrics extraction failed. Reason: " + reason);
                Toast.makeText(getContext(), "가사 추출 실패: " + reason, Toast.LENGTH_SHORT).show();
            }
        });

        mainWebView.addJavascriptInterface(mainBridge, "AndroidDetector");
        mainWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(createMonitoringJs(), null);
            }
        });
    }

    // =====================================================================================
    // == ArtistVibeLinkService 로직을 이식한 부분 ==
    // =====================================================================================

    private void startMemberIdSearchOnDedicatedWebView() {
        Log.i(TAG, "startMemberIdSearchOnDedicatedWebView: Initiating member ID search.");
        String artistUrl = "https://vibe.naver.com" + currentTrackMetadata.artistLink;
        List<String> vocalists = currentTrackMetadata.getVocalistNames();

        if (artistUrl.endsWith("null") || vocalists == null || vocalists.isEmpty()) {
            Log.w(TAG, "No artist link or vocalists found. Finishing process.");
            onAllProcessesComplete();
            return;
        }

        memberNamesToFindQueue = new LinkedList<>(vocalists);
        if (memberNamesToFindQueue.isEmpty()) {
            Log.d(TAG, "All vocalists already have IDs. Finishing process.");
            onAllProcessesComplete();
            return;
        }

        Log.d(TAG, "Creating a new invisible WebView for scraping.");
        scrapingWebView = new WebView(requireContext());
        setupScrapingWebViewAndBridge();
        scrapingWebView.loadUrl(artistUrl);
    }

    private void setupScrapingWebViewAndBridge() {
        scrapingBridge = new AutoLyricsBridge();
        scrapingBridge.setMemberLinkListener(new AutoLyricsBridge.OnMemberLinkListener() {
            @Override
            public void onMemberLinkFound(String name, String link) { handleSearchResult(name, link); }
            @Override
            public void onMemberLinkNotFound(String name) { handleSearchResult(name, null); }
        });

        scrapingWebView.getSettings().setJavaScriptEnabled(true);
        scrapingWebView.getSettings().setBlockNetworkImage(true); // 속도 향상
        scrapingWebView.addJavascriptInterface(scrapingBridge, "AndroidBridge");

        scrapingWebView.setWebViewClient(new WebViewClient() {
            private boolean isPageLoaded = false;
            @Override
            public void onPageFinished(WebView view, String url) {
                if (!isPageLoaded) {
                    isPageLoaded = true;
                    Log.i(TAG, "Scraping page finished loading. Starting to process the queue.");
                    searchNextMemberInQueue();
                }
            }
        });
    }

    private void searchNextMemberInQueue() {
        memberSearchTimeoutHandler.removeCallbacksAndMessages(null);
        if (memberNamesToFindQueue.isEmpty()) {
            Log.i(TAG, "Queue is empty. All members processed.");
            onAllProcessesComplete();
            return;
        }

        String currentTargetName = memberNamesToFindQueue.poll();
        Log.d(TAG, "Now processing member: '" + currentTargetName + "'.");
        scrapingBridge.resetMemberSearch();

        memberSearchTimeoutHandler.postDelayed(() -> {
            Log.e(TAG, "TIMEOUT for '" + currentTargetName + "'.");
            handleSearchResult(currentTargetName, null);
        }, PER_MEMBER_TIMEOUT_MS);

        scrapingWebView.evaluateJavascript(createMemberSearchJsCode(currentTargetName), null);
    }

    private void handleSearchResult(String name, String link) {
        memberSearchTimeoutHandler.removeCallbacksAndMessages(null);
        String artistId = null;
        if (link != null && !link.isEmpty()) {
            String[] parts = link.split("/");
            if (parts.length > 0) artistId = parts[parts.length - 1];
        }
        Log.d(TAG, "Received result for '" + name + "'. Extracted ID: " + artistId);
        currentTrackMetadata.updateVocalistId(name, artistId);
        searchNextMemberInQueue();
    }

    private void onAllProcessesComplete() {
        if (scrapingWebView != null) {
            scrapingWebView.destroy();
            scrapingWebView = null;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            Log.i(TAG, "ON-ALL-PROCESSES-COMPLETE: All tasks finished.");
            Log.d(TAG, "Final Metadata: " + new Gson().toJson(currentTrackMetadata));

            TrackMetadata handled = currentTrackMetadata;
            if (handled.title != null && !handled.title.isEmpty() && trackName != null && !trackName.isEmpty() && trackName.equals(handled.title)) {
               handled.title = null;
            }
            Bundle result = new Bundle();
            result.putParcelable(BUNDLE_KEY, currentTrackMetadata);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            NavHostFragment.findNavController(this).popBackStack();
        });
    }

    private void loadInitialSearchPage() {
        Log.d(TAG, "loadInitialSearchPage: Preparing initial search URL.");
        if (trackName != null && !trackName.isEmpty()) {
            StringBuilder query = new StringBuilder(trackName);
            if (artistName != null && !artistName.isEmpty()) query.append(" ").append(artistName);
            if (albumName != null && !albumName.isEmpty()) query.append(" ").append(albumName);

            try {
                String searchLink = "https://vibe.naver.com/search?query=" + URLEncoder.encode(query.toString(), "UTF-8");
                Log.d(TAG, "loadInitialSearchPage: Loading URL into MAIN WebView: " + searchLink);
                mainWebView.loadUrl(searchLink);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "loadInitialSearchPage: Failed to encode URL query. Loading default page into MAIN WebView.");
                mainWebView.loadUrl("https://vibe.naver.com");
            }
        }
    }

    // --- JavaScript Code Generators ---
    private String createMonitoringJs() {
        return "(function() {\n" +
                "    let v = false;\n" +
                "    setInterval(() => {\n" +
                "        const l = document.querySelector('div.lyrics');\n" +
                "        if (l && !v) {\n" +
                "            v = true;\n" +
                "            window.AndroidDetector.lyricsDetected(true);\n" +
                "        } else if (!l && v) {\n" +
                "            v = false;\n" +
                "            window.AndroidDetector.lyricsDetected(false);\n" +
                "        }\n" +
                "    }, 500);\n" +
                "})();";
    }


    private String createExtractionJs() {
        return "(function() {\n" +
                "    // ✅ 1. 현재 페이지 URL에서 트랙 ID 추출하는 로직 추가\n" +
                "    let vibeTrackId = null;\n" +
                "    const urlParts = window.location.href.split('/track/');\n" +
                "    if (urlParts.length > 1) {\n" +
                "        vibeTrackId = urlParts[1].split('?')[0];\n" +
                "    }\n" +
                "\n" +
                "    let t = document.querySelector('.title')?.innerText || '';\n" +
                "    let e = t.replace(/^곡명\\s*/, '').trim();\n" +
                "    let r = document.querySelectorAll('div.lyrics p');\n" +
                "    let n = '';\n" +
                "    if (r.length > 0 && r[0].innerText.trim() !== '') {\n" +
                "        n = Array.from(r).map(p => p.innerText).join('\\n');\n" +
                "    }\n" +
                "    let i = document.querySelector('a.link_sub_title');\n" +
                "    let o = i ? i.getAttribute('href') : null;\n" +
                "    function a(t) {\n" +
                "        const e = Array.from(document.querySelectorAll('div.item'))\n" +
                "            .filter(e => e.innerText.includes(t))\n" +
                "            .flatMap(e => Array.from(e.querySelectorAll('a.song_info_artist')))\n" +
                "            .map(e => e.innerText.trim());\n" +
                "        return e.length > 0 ? e : null;\n" +
                "    }\n" +
                "    let l = a('보컬');\n" +
                "    let s = a('작사');\n" +
                "    let c = a('작곡');\n" +
                "    if (typeof AndroidDetector !== 'undefined') {\n" +
                "        AndroidDetector.receiveMetadata(JSON.stringify({\n" +
                "            vibeTrackId: vibeTrackId, // ✅ 2. 추출한 ID를 JSON에 추가\n" +
                "            title: e,\n" +
                "            artistLink: o,\n" +
                "            lyrics: n,\n" +
                "            vocalists: l,\n" +
                "            lyricists: s,\n" +
                "            composers: c\n" +
                "        }));\n" +
                "    }\n" +
                "})();";
    }


    // ✅ [수정됨] 원본 코드가 사용하던 이름으로 JS 브릿지 호출을 수정 (receiveMetadata -> receiveMemberLink)
    private String createMemberSearchJsCode(String fullNameToSearch) {
        String name = fullNameToSearch.replace("\"", "\\\"");
        return "(function() {" +
                "  function findLink(retries) {" +
                "    const searchName = \"" + name + "\";" +
                "    console.log('JS: Searching for ' + searchName + '. Retries left: ' + retries);" +
                "    const container = document.querySelector('dl.info_member');" +
                "    if (container && container.innerText.trim() !== '') {" +
                "      console.log('JS: Member container is ready.');" +
                "      let links = Array.from(container.querySelectorAll('dd.info_desc a'));" +
                "      let match = links.find(a => a.innerText.trim() === searchName);" +
                "      if (match) {" +
                "        let link = match.getAttribute('href');" +
                "        console.log('JS-SUCCESS: Found a match for ' + searchName + '. Link: ' + link);" +
                "        AndroidBridge.receiveMemberLink(JSON.stringify({ name: searchName, link: link }));" + // 수정된 브릿지 메서드 호출
                "        return;" +
                "      } else {" +
                "        console.log('JS-FAILURE: Container is ready, but name not found.');" +
                "        AndroidBridge.reportMemberNotFound(searchName);" +
                "        return;" +
                "      }" +
                "    }" +
                "    if (retries > 0) {" +
                "      console.log('JS: Container not ready, retrying...');" +
                "      setTimeout(() => findLink(retries - 1), 100);" +
                "    } else {" +
                "      console.log('JS-FAILURE: Container not found after all retries.');" +
                "      AndroidBridge.reportMemberNotFound(searchName);" +
                "    }" +
                "  }" +
                "  findLink(50);" +
                "})();";
    }
}
