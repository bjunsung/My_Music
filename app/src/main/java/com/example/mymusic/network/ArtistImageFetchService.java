package com.example.mymusic.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.List;

public class ArtistImageFetchService {
    private static final String TAG = "ArtistImageFetchService";
    public interface ImageUrlsCallback{
        void onSuccess(List<String> urls);
        void onFailure(String reason);
    }
    private static boolean jsInjected = false;

    public static void fetchImages(WebView webview, String artistId, ImageUrlsCallback callback){
        Log.d(TAG, "fetchImages() called with artistId = " + artistId);

        final boolean[] callbackInvoked = {false};

        final ArtistImageFetchBridge bridge = new ArtistImageFetchBridge();
        bridge.setImageFetchListener(new ArtistImageFetchBridge.OnImageFetchListener() {
            @Override
            public void onSuccess(List<String> images) {
                Log.d(TAG, "bridge - onSuccess() called");
                if (!callbackInvoked[0]){
                    Log.d(TAG, "onSuccess() call");
                    callbackInvoked[0] = true;
                    callback.onSuccess(images);
                } else{
                    Log.d(TAG, "onSuccess() skipped due to alredy invoked");
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
                public void onPageFinished(WebView view, String url){
                    Log.d(TAG, "WebView finished loading: " + url);
                    String jsCode =
                            "(function() {\n" +
                                    "    console.log('[JS] 🚀 실행 시작');\n" +
                                    "    let clicked = false;\n" +
                                    "    let btnRetry = 0;\n" +
                                    "    let btnPoll = setInterval(() => {\n" +
                                    "        console.log('[JS] 🔍 버튼 탐색 시도');\n" +
                                    "        let btn = document.querySelector('a.img_wrap');\n" +
                                    "        if (btn && !clicked) {\n" +
                                    "            clicked = true;\n" +
                                    "            console.log('[JS] ✅ 버튼 발견 및 클릭');\n" +
                                    "            btn.click();\n" +
                                    "            clearInterval(btnPoll);\n" +
                                    "\n" +
                                    "            setTimeout(() => {\n" +
                                    "                let modalRetry = 0;\n" +
                                    "                let modalPoll = setInterval(() => {\n" +
                                    "                    let modals = document.querySelectorAll('#__modal-container .modal');\n" +
                                    "                    if (modals.length > 0) {\n" +
                                    "                        let lastModal = modals[modals.length - 1];\n" +
                                    "                        let imgs = lastModal.querySelectorAll('img[src]');\n" +
                                    "                        console.log('[JS] 📸 이미지 수:', imgs.length);\n" +
                                    "                        if (imgs.length > 0) {\n" +
                                    "                            let urls = Array.from(imgs).map(img => img.src);\n" +
                                    "                            if (window.AndroidBridge && window.AndroidBridge.receivedImages) {\n" +
                                    "                                window.AndroidBridge.receivedImages(JSON.stringify({ imageUrls: urls }));\n" +
                                    "                            }\n" +
                                    "                            clearInterval(modalPoll);\n" +
                                    "                        }\n" +
                                    "                    } else {\n" +
                                    "                        console.log('[JS] ⏳ 모달 없음');\n" +
                                    "                        modalRetry++;\n" +
                                    "                        if (modalRetry > 10) {\n" +
                                    "                            console.log('[JS] ❌ 모달 감지 실패');\n" +
                                    "                            clearInterval(modalPoll);\n" +
                                    "                        }\n" +
                                    "                    }\n" +
                                    "                }, 1000);\n" +
                                    "            }, 2500);\n" +  // 클릭 후 기다림
                                    "        } else {\n" +
                                    "            btnRetry++;\n" +
                                    "            if (btnRetry > 20) {\n" +
                                    "                console.log('[JS] ❌ 버튼 탐색 실패');\n" +
                                    "                clearInterval(btnPoll);\n" +
                                    "            }\n" +
                                    "        }\n" +
                                    "    }, 600);\n" +
                                    "})();";

                    if (!jsInjected) {
                        jsInjected = true;
                        Log.d(TAG, "Evaluating JavaScript...");
                        view.evaluateJavascript(jsCode, null);
                    }


                    Log.d(TAG, "Evaluating JavaScript...");
                    view.evaluateJavascript(jsCode, null);
                }
            });

            String fullUrl = artistId.startsWith("http") ? artistId + "/detail" : "https://vibe.naver.com/artist/" + artistId + "/detail";
            Log.d(TAG, "Loading URL: " + fullUrl);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                webview.loadUrl(fullUrl);
            }, 1500);

        });

    }


}
