package org.aether;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private WebView webView;
    private LinearLayout setupScreen;
    private LinearLayout loadingScreen;
    private LinearLayout webScreen;
    private TextView statusText;
    private ProgressBar progressBar;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#212121"));

        setupScreen = createSetupScreen();
        loadingScreen = createLoadingScreen();
        webScreen = createWebScreen();

        root.addView(setupScreen);
        root.addView(loadingScreen);
        root.addView(webScreen);

        setContentView(root);

        SharedPreferences prefs = getSharedPreferences("aether", MODE_PRIVATE);
        String savedUrl = prefs.getString("server_url", "");
        if (!savedUrl.isEmpty()) {
            showLoading();
            autoConnect(savedUrl);
        }
    }

    private LinearLayout createSetupScreen() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(60, 80, 60, 80);

        TextView icon = new TextView(this);
        icon.setText("A");
        icon.setTextSize(48);
        icon.setTextColor(Color.WHITE);
        icon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconP = new LinearLayout.LayoutParams(120, 120);
        iconP.gravity = Gravity.CENTER;
        iconP.bottomMargin = 24;
        icon.setLayoutParams(iconP);
        layout.addView(icon);

        TextView title = new TextView(this);
        title.setText("Aether");
        title.setTextSize(28);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 8);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Enter your PC's IP address");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.parseColor("#8e8e8e"));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 40);
        layout.addView(subtitle);

        EditText ipInput = new EditText(this);
        ipInput.setHint("e.g. 10.71.62.16:5000");
        ipInput.setTextColor(Color.WHITE);
        ipInput.setHintTextColor(Color.parseColor("#8e8e8e"));
        ipInput.setBackgroundColor(Color.parseColor("#2f2f2f"));
        ipInput.setPadding(24, 20, 24, 20);
        ipInput.setTextSize(16);
        ipInput.setSingleLine(true);
        LinearLayout.LayoutParams ipP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ipP.bottomMargin = 20;
        ipInput.setLayoutParams(ipP);
        layout.addView(ipInput);

        Button connectBtn = new Button(this);
        connectBtn.setText("Connect");
        connectBtn.setTextColor(Color.WHITE);
        connectBtn.setBackgroundColor(Color.parseColor("#10a37f"));
        connectBtn.setPadding(24, 16, 24, 16);
        connectBtn.setOnClickListener(v -> {
            String ip = ipInput.getText().toString().trim();
            if (!ip.isEmpty()) {
                if (!ip.startsWith("http")) ip = "http://" + ip;
                if (!ip.contains(":")) ip += ":5000";
                getSharedPreferences("aether", MODE_PRIVATE).edit().putString("server_url", ip).apply();
                showLoading();
                autoConnect(ip);
            }
        });
        layout.addView(connectBtn);

        return layout;
    }

    private LinearLayout createLoadingScreen() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(60, 80, 60, 80);
        layout.setVisibility(View.GONE);

        progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams pP = new LinearLayout.LayoutParams(80, 80);
        pP.gravity = Gravity.CENTER;
        pP.bottomMargin = 24;
        progressBar.setLayoutParams(pP);
        layout.addView(progressBar);

        statusText = new TextView(this);
        statusText.setText("Connecting to Aether...");
        statusText.setTextSize(16);
        statusText.setTextColor(Color.WHITE);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(20, 0, 20, 0);
        layout.addView(statusText);

        return layout;
    }

    private LinearLayout createWebScreen() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#212121"));
        layout.setVisibility(View.GONE);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#212121"));
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        layout.addView(webView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        return layout;
    }

    private void showLoading() {
        setupScreen.setVisibility(View.GONE);
        loadingScreen.setVisibility(View.VISIBLE);
        webScreen.setVisibility(View.GONE);
    }

    private void showWeb() {
        setupScreen.setVisibility(View.GONE);
        loadingScreen.setVisibility(View.GONE);
        webScreen.setVisibility(View.VISIBLE);
    }

    private void showSetup() {
        setupScreen.setVisibility(View.VISIBLE);
        loadingScreen.setVisibility(View.GONE);
        webScreen.setVisibility(View.GONE);
    }

    private void autoConnect(String url) {
        statusText.setText("Connecting to " + url + "...");
        executor.execute(() -> {
            try {
                URL u = new URL(url + "/api/conversations");
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                int code = conn.getResponseCode();
                if (code == 200) {
                    connected = true;
                    handler.post(() -> {
                        webView.loadUrl(url);
                        showWeb();
                    });
                } else {
                    retry(url);
                }
            } catch (Exception e) {
                retry(url);
            }
        });
    }

    private void retry(String url) {
        if (!connected) {
            handler.post(() -> {
                statusText.setText("PC not found. Retrying in 5s...");
                handler.postDelayed(() -> autoConnect(url), 5000);
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            getSharedPreferences("aether", MODE_PRIVATE).edit().clear().apply();
            showSetup();
        }
    }
}
