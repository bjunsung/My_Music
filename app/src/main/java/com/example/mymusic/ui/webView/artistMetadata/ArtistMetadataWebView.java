package com.example.mymusic.ui.webView.artistMetadata;

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
import com.example.mymusic.model.ArtistMetadata;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ArtistMetadataWebView extends Fragment {
    private final String TAG = "ArtistMetadataWebView";
    private WebView mainWebView;
    private Button importButton;
    private String artistName, artistId;
    private ArtistMetadataWebViewBridge mainBridge;
    private ArtistMetadata currentArtistMetadata;
    public static final String BUNDLE_KEY = "artist_metadata";
    public static final String REQUEST_KEY = "artist_metadata_fetch_web_view";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(this, new OnBackPressedCallback(true){
                    @Override
                    public void handleOnBackPressed() {
                        if (mainWebView.canGoBack())
                            mainWebView.goBack();
                        else{
                            NavHostFragment.findNavController(ArtistMetadataWebView.this).popBackStack();
                        }
                    }
                });

        if (getArguments() != null){
            artistName = getArguments().getString("artist_name");
            artistId = getArguments().getString("artist_id");
            Log.d(TAG, "Arguments received: artist: " + artistName);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_web_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        mainWebView = view.findViewById(R.id.photo_webview);
        importButton = view.findViewById(R.id.import_button);

        setupMainWebView();
        loadInitialSearchPage();

        importButton.setText("아티스트 정보 가져오기");
        importButton.setOnClickListener(v -> {
            Log.i(TAG, "IMPORT BUTTON CLICKED");
            setupScrapingBridge();
            mainWebView.evaluateJavascript(createExtractionJs(), null);
        });
    }



    private void setupMainWebView() {
        Log.d(TAG, "setupMainWebView: Configuring main WebView for user interaction.");
        mainBridge = new ArtistMetadataWebViewBridge();

        mainWebView.getSettings().setJavaScriptEnabled(true);
        mainWebView.getSettings().setDomStorageEnabled(true);
        mainWebView.getSettings().setBlockNetworkImage(false);
        mainWebView.addJavascriptInterface(mainBridge, "AndroidMainBridge");
        mainWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage){
                Log.d("WebViewConsole-Main", consoleMessage.message());
                return true;
            }

        });

        mainBridge.setVisibilityListener(new ArtistMetadataWebViewBridge.VisibleListener() {
            @Override
            public void onArtistOpened() { importButton.setVisibility(View.VISIBLE); }

            @Override
            public void onArtistClosed() { importButton.setVisibility(View.GONE); }
        });

        mainWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(createMonitoringJs(), null);
            }
        });

    }

    private void setupScrapingBridge(){
        mainBridge.setMetadataListener(new ArtistMetadataWebViewBridge.MetadataListener() {
            @Override
            public void onSuccess(ArtistMetadata metadata) {
                Log.d(TAG, "BRIDGE-SUCCESS: Artist metadata received.");
                currentArtistMetadata = metadata;
                for (String imgLink : metadata.images){
                    Log.d(TAG + "Debug", "image_" + imgLink);
                }
                metadata.spotifyArtistId = artistId;
                Log.d(TAG, metadata.toString());
                if (metadata.artistNameKr != null && metadata.artistNameKr.equals(artistName))
                    metadata.artistNameKr = null;
                new Handler(Looper.getMainLooper()).post(() -> {
                    Bundle result = new Bundle();
                    result.putParcelable(BUNDLE_KEY, metadata);
                    getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                    getParentFragmentManager().popBackStack();
                });

            }

            @Override
            public void onFailure(String reason) {
                Log.d(TAG, "BRIDGE-FAILURE: Artist metadata extraction failed. reason: " + reason);
                Toast.makeText(getContext(), "아티스트 정보 추출 실패: " + reason, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void loadInitialSearchPage() {
        Log.d(TAG, "loadInitialSearchPage: Preparing initial search URL.");
        if (artistName != null && !artistName.isEmpty()){
            try{
                String searchLink = "https://vibe.naver.com/search/artists?query=" + URLEncoder.encode(artistName, "UTF-8");
                Log.d(TAG, "loadInitialSearchPage: Loading URL into MAIN WebView: " + searchLink);
                mainWebView.loadUrl(searchLink);
            } catch(UnsupportedEncodingException e){
                Log.e(TAG, "loadInitialSearchPage: Failed to encode URL query. Loading default page into MAIN WebView.");
                mainWebView.loadUrl("https://vibe.naver.com");
            }
        }
    }

    private String createMonitoringJs() {
        return "(function() {\n" +
                "    let lastPath = '';\n" +
                "    let visible = false;\n" +
                "    setInterval(() => {\n" +
                "        const currentPath = window.location.pathname;\n" +
                "        if (currentPath === lastPath) return;\n" +
                "        lastPath = currentPath;\n" +
                "        const isArtistPage = /\\/artist\\/\\d+(\\/detail)?/.test(currentPath);\n" +
                "        console.log('[JS] Path changed: ' + currentPath);\n" +
                "        console.log('[JS] isArtistPage: ' + isArtistPage);\n" +
                "        if (isArtistPage !== visible) {\n" +
                "            visible = isArtistPage;\n" +
                "            console.log('[JS] Visibility changed: ' + visible);\n" +
                "            AndroidMainBridge.artistLinkDetected(visible);\n" +
                "        }\n" +
                "    }, 500);\n" +
                "})();";
    }




    private String createExtractionJs() {
        return  "(function waitForArtist() {\n" +
                "    const check = (retries) => {\n" +
                "    const url = new URL(window.location.href);\n" +
                "    const pathSegments = url.pathname.split('/');\n" +
                "    let vibeArtistId = null;\n" +
                "    if (pathSegments.includes('artist')){\n" +
                "        const index = pathSegments.indexOf('artist');\n" +
                "        vibeArtistId = pathSegments[index + 1];\n" +
                "        console.log('vibeArtistId: ', vibeArtistId);\n" +
                "    }\n" +
                "        console.log('[JS] Checking for artist DOM, retries left: ' + retries);\n" +
                "        const titleAnchor = document.querySelector('.title_link');\n" +
                "        let artistName = null;\n" +
                "        if (titleAnchor) {\n" +
                "            artistName = titleAnchor.innerText.trim();\n" +
                "            console.log('[JS] Extracted artistId: ' + vibeArtistId + ', name: ' + artistName);\n" +
                "        } else {\n" +
                "            console.log('[JS] Title anchor not found');\n" +
                "        }\n" +
                "\n" +
                "        let paragraphs = document.querySelectorAll('dl.info');\n" +
                "        console.log('[JS] Found dl.info count: ' + paragraphs.length);\n" +
                "\n" +
                "        if (paragraphs.length > 0 && paragraphs[0].innerText.trim() !== '') {\n" +
                "            console.log('[JS] dl.info appears ready');\n" +
                "            function extractInfo(label, asArray = false) {\n" +
                "                const dlList = Array.from(document.querySelectorAll('dl.info'));\n" +
                "                const match = dlList.find(dl => {\n" +
                "                    const dt = dl.querySelector('.info_term');\n" +
                "                    return dt && dt.innerText.trim() === label;\n" +
                "                });\n" +
                "                const dd = match?.querySelector('.info_desc');\n" +
                "                if (!dd) {\n" +
                "                    console.log('[JS] ' + label + ' 항목 없음');\n" +
                "                    return asArray ? [] : null;\n" +
                "                }\n" +
                "                const text = dd.innerText.trim();\n" +
                "                console.log('[JS] ' + label + ' 항목 값: ' + text);\n" +
                "                return asArray ? text.split(',').map(s => s.trim()).filter(Boolean) : text;\n" +
                "            }\n" +
                "\n" +
                "            let debutDate = extractInfo('데뷔');\n" +
                "            let yearsOfActivity = extractInfo('연대', true);\n" +
                "            let agency = extractInfo('소속', true);\n" +
                "            const bioElement = document.querySelector('p.biography');\n" +
                "            let biography = bioElement ? bioElement.innerText.trim() : null;\n" +
                "            console.log('[JS] Biography: ' + biography);\n" +
                "            let images = Array.from(document.querySelectorAll('img.img_thumb'))\n" +
                "                .map(img => {\n" +
                "                    const src = img.src;\n" +
                "                    const index = src.indexOf('?type=');\n" +
                "                    return index !== -1 ? src.substring(0, index) : src;\n" +
                "                })\n" +
                "                  .filter(src => src && src.includes('music-phinf.pstatic.net') && !src.includes('musicmeta-phinf'));\n" +
                "            console.log('[JS] Found images: ' + images.length);\n" +
                "\n" +
                "            let members = Array.from(document.querySelectorAll('.member_row')).map(row => {\n" +
                "                const linkTag = row.querySelector('a.member_link');\n" +
                "                const href = linkTag?.getAttribute('href') || '';\n" +
                "                const artistIdMatch = href.match(/\\/artist\\/(\\d+)/);\n" +
                "                const artistId = artistIdMatch ? artistIdMatch[1] : null;\n" +
                "                const name = row.querySelector('.member_info strong')?.innerText?.trim() || null;\n" +
                "                console.log('[JS] Member: ' + name + ', ID: ' + artistId);\n" +
                "                return [name, artistId, null];\n" +
                "            }).filter(item => item[0] != null && item[1] != null);\n" +
                "\n" +
                "            let activity = Array.from(document.querySelectorAll('.info_activity a')).map(a => {\n" +
                "                const name = a.innerText.trim();\n" +
                "                const href = a.getAttribute('href') || '';\n" +
                "                const idMatch = href.match(/\\/artist\\/(\\d+)/);\n" +
                "                const artistId = idMatch ? idMatch[1] : null;\n" +
                "                console.log('[JS] Activity: ' + name + ', ID: ' + artistId);\n" +
                "                return [name, artistId];\n" +
                "            }).filter(item => item[0] && item[1]);\n" +
                "\n" +
                "            if (typeof AndroidMainBridge !== 'undefined') {\n" +
                "                console.log('[JS] Sending metadata to AndroidBridge');\n" +
                "                AndroidMainBridge.receiveMetadata(JSON.stringify({\n" +
                "                    vibeArtistId: vibeArtistId,\n" +
                "                    artistNameKr: artistName,\n" +
                "                    debutDate: debutDate,\n" +
                "                    yearsOfActivity: yearsOfActivity,\n" +
                "                    agency: agency,\n" +
                "                    biography: biography,\n" +
                "                    images: images,\n" +
                "                    members: members,\n" +
                "                    activity: activity\n" +
                "                }));\n" +
                "            } else {\n" +
                "                console.log('[JS] AndroidMainBridge is undefined');\n" +
                "            }\n" +
                "        } else {\n" +
                "            console.log('[JS] dl.info not ready or empty');\n" +
                "            if (retries > 0) {\n" +
                "                setTimeout(() => check(retries - 1), 500);\n" +
                "            } else {\n" +
                "                if (typeof AndroidMainBridge !== 'undefined') {\n" +
                "                    AndroidMainBridge.reportFailure('페이지에서 정보를 찾을 수 없습니다.');\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    };\n" +
                "    check(20);\n" +
                "})()";
    }




}




















