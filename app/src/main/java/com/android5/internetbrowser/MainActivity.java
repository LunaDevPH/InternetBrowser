package com.android5.internetbrowser;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FrameLayout webViewContainer;
    private EditText urlEditText;
    private Button goButton;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private View btnBack, btnForward, btnAi, btnHistory, btnDownloads, btnNewTab, btnAnalyze;
    private TabLayout tabLayout;
    
    private View errorLayout;
    private TextView errorMessage;
    private Button retryButton;
    private View startPageLayout;
    private TextView rotatingTipText;
    private HistoryManager historyManager;

    private final List<WebView> tabList = new ArrayList<>();
    private int currentTabIndex = -1;

    private final Handler tipHandler = new Handler(Looper.getMainLooper());
    private int currentTipIndex = 0;
    private final int[] tipStrings = {
            R.string.tip_search,
            R.string.tip_find_content,
            R.string.tip_privacy,
            R.string.tip_fast,
            R.string.tip_ai,
            R.string.tip_material,
            R.string.tip_history,
            R.string.tip_no_collection,
            R.string.tip_no_tracking,
            R.string.tip_anonymous
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        historyManager = new HistoryManager(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        webViewContainer = findViewById(R.id.webview_container);
        urlEditText = findViewById(R.id.url_edit_text);
        goButton = findViewById(R.id.go_button);
        progressBar = findViewById(R.id.progress_bar);
        tabLayout = findViewById(R.id.tab_layout);
        
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnAi = findViewById(R.id.btn_ai);
        btnHistory = findViewById(R.id.btn_history);
        btnDownloads = findViewById(R.id.btn_downloads);
        btnNewTab = findViewById(R.id.btn_new_tab);
        btnAnalyze = findViewById(R.id.btn_analyze);

        errorLayout = findViewById(R.id.error_layout);
        errorMessage = findViewById(R.id.error_message);
        retryButton = findViewById(R.id.retry_button);
        startPageLayout = findViewById(R.id.start_page_layout);
        rotatingTipText = findViewById(R.id.rotating_tip_text);

        initSuggestions();
        
        btnNewTab.setOnClickListener(v -> addNewTab(null));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchTab(tab.getPosition());
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        goButton.setOnClickListener(v -> loadUrl());

        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUrl();
                return true;
            }
            return false;
        });

        btnBack.setOnClickListener(v -> {
            WebView currentWebView = getCurrentWebView();
            if (currentWebView != null && currentWebView.canGoBack()) {
                currentWebView.goBack();
            } else {
                showStartPage();
            }
        });
        
        btnForward.setOnClickListener(v -> {
            WebView currentWebView = getCurrentWebView();
            if (currentWebView != null && currentWebView.canGoForward()) currentWebView.goForward();
        });
        
        btnAi.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AiChatActivity.class);
            startActivity(intent);
        });

        btnAnalyze.setOnClickListener(v -> analyzeCurrentPage());

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        btnDownloads.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DownloadsActivity.class);
            startActivity(intent);
        });

        retryButton.setOnClickListener(v -> {
            WebView currentWebView = getCurrentWebView();
            if (currentWebView != null) currentWebView.reload();
        });

        addNewTab(null);
    }

    private void analyzeCurrentPage() {
        WebView current = getCurrentWebView();
        if (current == null) return;

        current.evaluateJavascript("(function() { return document.body.innerText; })();", value -> {
            if (value != null && !value.isEmpty()) {
                // Remove leading/trailing quotes from JS string result
                String content = value;
                if (content.startsWith("\"") && content.endsWith("\"")) {
                    content = content.substring(1, content.length() - 1);
                }
                // Unescape common characters (basic)
                content = content.replace("\\n", "\n").replace("\\\"", "\"");
                
                Intent intent = new Intent(MainActivity.this, AiChatActivity.class);
                intent.putExtra("analyze_content", content);
                intent.putExtra("page_url", current.getUrl());
                startActivity(intent);
            }
        });
    }

    private void addNewTab(String url) {
        WebView newWebView = createWebView();
        tabList.add(newWebView);
        
        TabLayout.Tab newTab = tabLayout.newTab().setText("New Tab");
        tabLayout.addTab(newTab);
        
        if (tabList.size() > 1) {
            tabLayout.setVisibility(View.VISIBLE);
        }
        
        newTab.select();
        
        if (url != null) {
            loadUrl(url);
        } else {
            showStartPage();
        }
    }

    private void switchTab(int position) {
        if (position < 0 || position >= tabList.size()) return;
        
        currentTabIndex = position;
        webViewContainer.removeAllViews();
        WebView selectedWebView = tabList.get(position);
        webViewContainer.addView(selectedWebView);
        
        String url = selectedWebView.getUrl();
        if (url == null || url.isEmpty() || selectedWebView.getVisibility() == View.GONE) {
            showStartPage();
            urlEditText.setText("");
            btnAnalyze.setVisibility(View.GONE);
        } else {
            hideStartPage();
            urlEditText.setText(url);
            btnAnalyze.setVisibility(View.VISIBLE);
        }
    }

    private WebView createWebView() {
        WebView wv = new WebView(this);
        wv.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        optimizeWebView(wv);
        
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (view != getCurrentWebView()) return;
                urlEditText.setText(url);
                progressBar.setVisibility(View.VISIBLE);
                errorLayout.setVisibility(View.GONE);
                startPageLayout.setVisibility(View.GONE);
                webViewContainer.setVisibility(View.VISIBLE);
                btnAnalyze.setVisibility(View.GONE);
                stopTipRotation();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (view != getCurrentWebView()) return;
                progressBar.setVisibility(View.GONE);
                btnAnalyze.setVisibility(View.VISIBLE);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setSubtitle(view.getTitle());
                }
                
                int index = tabList.indexOf(view);
                if (index != -1 && tabLayout.getTabAt(index) != null) {
                    tabLayout.getTabAt(index).setText(view.getTitle());
                }

                String title = view.getTitle();
                if (title == null || title.isEmpty()) title = url;
                historyManager.addHistoryItem(title, url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (view != getCurrentWebView()) return;
                if (request.isForMainFrame()) {
                    showError(error.getDescription().toString());
                }
            }
        });

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (view != getCurrentWebView()) return;
                progressBar.setProgress(newProgress);
            }
        });

        wv.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimetype);
            String cookies = CookieManager.getInstance().getCookie(url);
            request.addRequestHeader("cookie", cookies);
            request.addRequestHeader("User-Agent", userAgent);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype));
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
            Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show();
        });

        return wv;
    }

    private WebView getCurrentWebView() {
        if (currentTabIndex >= 0 && currentTabIndex < tabList.size()) {
            return tabList.get(currentTabIndex);
        }
        return null;
    }

    private void showStartPage() {
        webViewContainer.setVisibility(View.GONE);
        startPageLayout.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
        btnAnalyze.setVisibility(View.GONE);
        startTipRotation();
        if (getSupportActionBar() != null) getSupportActionBar().setSubtitle(null);
    }

    private void hideStartPage() {
        startPageLayout.setVisibility(View.GONE);
        webViewContainer.setVisibility(View.VISIBLE);
        btnAnalyze.setVisibility(View.VISIBLE);
        stopTipRotation();
    }

    private void startTipRotation() {
        tipHandler.removeCallbacksAndMessages(null);
        tipHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (startPageLayout.getVisibility() == View.VISIBLE) {
                    currentTipIndex = (currentTipIndex + 1) % tipStrings.length;
                    rotatingTipText.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                        rotatingTipText.setText(tipStrings[currentTipIndex]);
                        rotatingTipText.animate().alpha(1f).setDuration(300).start();
                    }).start();
                    tipHandler.postDelayed(this, 5000);
                }
            }
        }, 5000);
    }

    private void stopTipRotation() {
        tipHandler.removeCallbacksAndMessages(null);
    }

    private void initSuggestions() {
        findViewById(R.id.chip_google).setOnClickListener(v -> loadUrl("https://www.google.com"));
        findViewById(R.id.chip_youtube).setOnClickListener(v -> loadUrl("https://www.youtube.com"));
        findViewById(R.id.chip_wikipedia).setOnClickListener(v -> loadUrl("https://www.wikipedia.org"));
    }

    private void showError(String description) {
        errorLayout.setVisibility(View.VISIBLE);
        webViewContainer.setVisibility(View.GONE);
        startPageLayout.setVisibility(View.GONE);
        btnAnalyze.setVisibility(View.GONE);
        errorMessage.setText(description);
        progressBar.setVisibility(View.GONE);
        stopTipRotation();
    }

    private void optimizeWebView(WebView wv) {
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        String customUA = "Mozilla/5.0 (Linux; Android 16; Pixel 9 Pro Build/VPP1.231215.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36";
        webSettings.setUserAgentString(customUA);
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private void loadUrl() {
        loadUrl(urlEditText.getText().toString().trim());
    }

    private void loadUrl(String url) {
        if (!url.isEmpty()) {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            WebView current = getCurrentWebView();
            if (current != null) {
                current.loadUrl(url);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (startPageLayout.getVisibility() == View.VISIBLE) {
            startTipRotation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTipRotation();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        WebView currentWebView = getCurrentWebView();
        if (currentWebView != null && currentWebView.getVisibility() == View.VISIBLE) {
            if (currentWebView.canGoBack()) {
                currentWebView.goBack();
            } else {
                showStartPage();
            }
        } else {
            super.onBackPressed();
        }
    }
}