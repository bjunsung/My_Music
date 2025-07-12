package com.example.mymusic.ui.photoWebView;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.mymusic.R;
import com.example.mymusic.ui.webView.photoWebView.PhotoDetectorBridge;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class WebViewFragment extends Fragment {

    private WebView webView;
    private Button importButton;
    private String artistId;
    private WindowInsetsControllerCompat insetsController;
    public static final String REQUEST_KEY = "webview_fragment_request";
    public static final String BUNDLE_KEY_IMAGE_URLS = "image_urls";

    private List<String> urls;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 이전 화면에서 전달받은 artistId 가져오기
        if (getArguments() != null) {
            artistId = getArguments().getString("artist_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_web_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        webView = view.findViewById(R.id.photo_webview);
        importButton = view.findViewById(R.id.import_button);

        setupWebView();

        importButton.setOnClickListener(v -> {
            extractImages();
        });
    }


    private void setupWebView() {
        // 1. 웹뷰 설정
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");

        // 2. 브릿지 설정
        PhotoDetectorBridge bridge = new PhotoDetectorBridge();
        bridge.setListener(new PhotoDetectorBridge.Listener() {
            @Override
            public void onPhotoViewerOpened() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> importButton.setVisibility(View.VISIBLE));
                }
            }
            @Override
            public void onPhotoViewerClosed() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> importButton.setVisibility(View.GONE));
                }
            }
        });
        webView.addJavascriptInterface(bridge, "AndroidDetector");

        // 3. 웹뷰 클라이언트 및 감시 스크립트 주입
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String monitoringJs =
                        "(function() {\n" +
                                "    let isViewerVisible = false;\n" +
                                "    setInterval(() => {\n" +
                                "        const photoViewer = document.querySelector('div.photoviewer');\n" +
                                "        if (photoViewer && !isViewerVisible) {\n" +
                                "            isViewerVisible = true;\n" +
                                "            window.AndroidDetector.photoViewerDetected(true);\n" +
                                "        } else if (!photoViewer && isViewerVisible) {\n" +
                                "            isViewerVisible = false;\n" +
                                "            window.AndroidDetector.photoViewerDetected(false);\n" +
                                "        }\n" +
                                "    }, 500);\n" +
                                "})();";

                view.evaluateJavascript(monitoringJs, null);
            }
        });

        // 4. 페이지 로드
        if (artistId != null && !artistId.isEmpty()) {
            webView.loadUrl("https://vibe.naver.com/artist/" + artistId + "/detail");
        }
    }

    private void extractImages() {
        String extractionJs = "(function() { const images = document.querySelectorAll('.photoviewer .swiper-wrapper img'); const urls = Array.from(images).map(img => img.src.split('?')[0]); return JSON.stringify(urls); })();";

        webView.evaluateJavascript(extractionJs, jsonResult -> {
            if (jsonResult != null && !jsonResult.equals("null") && jsonResult.length() > 2) {

                // [수정!] evaluateJavascript가 추가한 따옴표와 이스케이프 문자를 정리합니다.
                String actualJson = jsonResult;
                if (jsonResult.startsWith("\"") && jsonResult.endsWith("\"")) {
                    actualJson = jsonResult.substring(1, jsonResult.length() - 1).replace("\\\"", "\"");
                }

                Log.d("WebViewFragment", "Cleaned JSON for parsing: " + actualJson);

                // 이제 정리된 JSON 문자열을 파싱합니다.
                Gson gson = new Gson();
                try {
                    urls = gson.fromJson(actualJson, new TypeToken<List<String>>(){}.getType());

                    if (!urls.isEmpty()) {

                        Toast.makeText(getContext(), "이미지 " + urls.size()/2 + "개 가져오기 성공!", Toast.LENGTH_LONG).show();
                        Log.d("WebViewFragment", "Extracted URLs: " + urls.toString());

                        Bundle result = new Bundle();
                        result.putStringArrayList(BUNDLE_KEY_IMAGE_URLS, new ArrayList<>(urls));

                        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);

                        webView.loadUrl("about:blank"); // 즉시 WebView 정리
                        requireActivity().runOnUiThread(() -> {
                            NavHostFragment.findNavController(WebViewFragment.this).popBackStack();
                        });

                    }
                } catch (Exception e) {
                    Log.e("WebViewFragment", "Failed to parse cleaned JSON", e);
                    Toast.makeText(getContext(), "이미지를 파싱하는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(getContext(), "가져올 이미지가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }



    

}