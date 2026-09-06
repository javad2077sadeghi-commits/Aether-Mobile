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
    private LinearLayout loginScreen;
    private LinearLayout setupScreen;
    private LinearLayout loadingScreen;
    private LinearLayout webScreen;
    private TextView statusText;
    private ProgressBar progressBar;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean connected = false;
    private String serverUrl = "";
    private String authToken = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        loginScreen = createLoginScreen();
        setupScreen = createSetupScreen();
        loadingScreen = createLoadingScreen();
        webScreen = createWebScreen();

        root.addView(loginScreen);
        root.addView(setupScreen);
        root.addView(loadingScreen);
        root.addView(webScreen);

        setContentView(root);

        SharedPreferences prefs = getSharedPreferences("aether", MODE_PRIVATE);
        serverUrl = prefs.getString("server_url", "");
        authToken = prefs.getString("auth_token", "");

        if (!serverUrl.isEmpty() && !authToken.isEmpty()) {
            showLoading();
            autoConnect(serverUrl);
        } else {
            showLogin();
        }
    }

    private LinearLayout createLoginScreen() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(60, 80, 60, 80);
        layout.setBackgroundColor(Color.WHITE);

        LinearLayout iconBg = new LinearLayout(this);
        iconBg.setGravity(Gravity.CENTER);
        iconBg.setBackgroundColor(Color.parseColor("#10a37f"));
        int s = dp(80);
        LinearLayout.LayoutParams iconP = new LinearLayout.LayoutParams(s, s);
        iconP.gravity = Gravity.CENTER;
        iconP.bottomMargin = 24;
        iconBg.setLayoutParams(iconP);

        TextView icon = new TextView(this);
        icon.setText("A");
        icon.setTextSize(36);
        icon.setTextColor(Color.WHITE);
        icon.setGravity(Gravity.CENTER);
        iconBg.addView(icon);
        layout.addView(iconBg);

        TextView title = new TextView(this);
        title.setText("Aether");
        title.setTextSize(24);
        title.setTextColor(Color.parseColor("#0d0d0d"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 8);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("رمز عبور را وارد کنید");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.parseColor("#8e8e8e"));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 32);
        layout.addView(subtitle);

        EditText passInput = new EditText(this);
        passInput.setHint("رمز عبور");
        passInput.setHintTextColor(Color.parseColor("#8e8e8e"));
        passInput.setTextColor(Color.parseColor("#0d0d0d"));
        passInput.setBackgroundColor(Color.parseColor("#f4f4f4"));
        passInput.setPadding(dp(16), dp(14), dp(16), dp(14));
        passInput.setTextSize(16);
        passInput.setSingleLine(true);
        passInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams passP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        passP.bottomMargin = 16;
        passInput.setLayoutParams(passP);
        layout.addView(passInput);

        TextView loginError = new TextView(this);
        loginError.setText("رمز عبور اشتباه است");
        loginError.setTextSize(13);
        loginError.setTextColor(Color.parseColor("#ef4444"));
        loginError.setVisibility(View.GONE);
        loginError.setPadding(0, 0, 0, 8);
        layout.addView(loginError);

        Button loginBtn = new Button(this);
        loginBtn.setText("ورود");
        loginBtn.setTextColor(Color.WHITE);
        loginBtn.setBackgroundColor(Color.parseColor("#10a37f"));
        loginBtn.setPadding(dp(16), dp(14), dp(16), dp(14));
        loginBtn.setOnClickListener(v -> {
            String pass = passInput.getText().toString().trim();
            if (!pass.isEmpty()) {
                loginBtn.setEnabled(false);
                loginBtn.setText("در حال ورود...");
                doLogin(pass, loginBtn, loginError);
            }
        });
        layout.addView(loginBtn);

        return layout;
    }

    private void doLogin(String password, Button btn, TextView errorView) {
        executor.execute(() -> {
            try {
                URL url = new URL(serverUrl.isEmpty() ? "http://10.71.62.16:5000/api/login" : serverUrl + "/api/login");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                String body = "{\"password\":\"" + password + "\"}";
                OutputStream os = conn.getOutputStream();
                os.write(body.getBytes());
                os.close();

                int code = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String resp = sb.toString();
                if (code == 200 && resp.contains("\"ok\":true")) {
                    int tokenStart = resp.indexOf("\"token\":\"") + 9;
                    int tokenEnd = resp.indexOf("\"", tokenStart);
                    String token = resp.substring(tokenStart, tokenEnd);

                    SharedPreferences.Editor ed = getSharedPreferences("aether", MODE_PRIVATE).edit();
                    ed.putString("auth_token", token);
                    ed.putString("server_url", serverUrl.isEmpty() ? "http://10.71.62.16:5000" : serverUrl);
                    ed.apply();
                    authToken = token;

                    handler.post(() -> {
                        showSetup();
                    });
                } else {
                    handler.post(() -> {
                        errorView.setVisibility(View.VISIBLE);
                        btn.setEnabled(true);
                        btn.setText("ورود");
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    errorView.setText("سرور یافت نشد - آدرس IP را وارد کنید");
                    errorView.setVisibility(View.VISIBLE);
                    btn.setEnabled(true);
                    btn.setText("ورود");
                    showSetup();
                });
            }
        });
    }

    private LinearLayout createSetupScreen() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(60, 80, 60, 80);
        layout.setBackgroundColor(Color.WHITE);

        TextView icon = new TextView(this);
        icon.setText("A");
        icon.setTextSize(48);
        icon.setTextColor(Color.WHITE);
        icon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconP = new LinearLayout.LayoutParams(dp(120), dp(120));
        iconP.gravity = Gravity.CENTER;
        iconP.bottomMargin = 24;
        icon.setLayoutParams(iconP);
        layout.addView(icon);

        TextView title = new TextView(this);
        title.setText("Aether");
        title.setTextSize(28);
        title.setTextColor(Color.parseColor("#0d0d0d"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 8);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("آدرس IP کامپیوتر را وارد کنید");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.parseColor("#8e8e8e"));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 40);
        layout.addView(subtitle);

        EditText ipInput = new EditText(this);
        ipInput.setHint("مثلاً 10.71.62.16:5000");
        ipInput.setTextColor(Color.parseColor("#0d0d0d"));
        ipInput.setHintTextColor(Color.parseColor("#8e8e8e"));
        ipInput.setBackgroundColor(Color.parseColor("#f4f4f4"));
        ipInput.setPadding(dp(16), dp(14), dp(16), dp(14));
        ipInput.setTextSize(16);
        ipInput.setSingleLine(true);
        LinearLayout.LayoutParams ipP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ipP.bottomMargin = 20;
        ipInput.setLayoutParams(ipP);
        layout.addView(ipInput);

        Button connectBtn = new Button(this);
        connectBtn.setText("اتصال");
        connectBtn.setTextColor(Color.WHITE);
        connectBtn.setBackgroundColor(Color.parseColor("#10a37f"));
        connectBtn.setPadding(dp(16), dp(14), dp(16), dp(14));
        connectBtn.setOnClickListener(v -> {
            String ip = ipInput.getText().toString().trim();
            if (!ip.isEmpty()) {
                if (!ip.startsWith("http")) ip = "http://" + ip;
                if (!ip.contains(":")) ip += ":5000";
                serverUrl = ip;
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
        layout.setBackgroundColor(Color.WHITE);
        layout.setVisibility(View.GONE);

        progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams pP = new LinearLayout.LayoutParams(dp(80), dp(80));
        pP.gravity = Gravity.CENTER;
        pP.bottomMargin = 24;
        progressBar.setLayoutParams(pP);
        layout.addView(progressBar);

        statusText = new TextView(this);
        statusText.setText("در حال اتصال...");
        statusText.setTextSize(16);
        statusText.setTextColor(Color.parseColor("#0d0d0d"));
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(20, 0, 20, 0);
        layout.addView(statusText);

        return layout;
    }

    private LinearLayout createWebScreen() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.WHITE);
        layout.setVisibility(View.GONE);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
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

    private void showLogin() {
        loginScreen.setVisibility(View.VISIBLE);
        setupScreen.setVisibility(View.GONE);
        loadingScreen.setVisibility(View.GONE);
        webScreen.setVisibility(View.GONE);
    }

    private void showSetup() {
        loginScreen.setVisibility(View.GONE);
        setupScreen.setVisibility(View.VISIBLE);
        loadingScreen.setVisibility(View.GONE);
        webScreen.setVisibility(View.GONE);
    }

    private void showLoading() {
        loginScreen.setVisibility(View.GONE);
        setupScreen.setVisibility(View.GONE);
        loadingScreen.setVisibility(View.VISIBLE);
        webScreen.setVisibility(View.GONE);
    }

    private void showWeb() {
        loginScreen.setVisibility(View.GONE);
        setupScreen.setVisibility(View.GONE);
        loadingScreen.setVisibility(View.GONE);
        webScreen.setVisibility(View.VISIBLE);
    }

    private void autoConnect(String url) {
        statusText.setText("اتصال به " + url + "...");
        executor.execute(() -> {
            try {
                URL u = new URL(url + "/api/conversations");
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                int code = conn.getResponseCode();
                if (code == 200) {
                    connected = true;
                    handler.post(() -> {
                        webView.loadUrl(url);
                        showWeb();
                    });
                } else if (code == 401) {
                    handler.post(() -> {
                        getSharedPreferences("aether", MODE_PRIVATE).edit().clear().apply();
                        authToken = "";
                        serverUrl = "";
                        showLogin();
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
                statusText.setText("سرور یافت نشد. 5 ثانیه دیگر دوباره تلاش می‌شود...");
                handler.postDelayed(() -> autoConnect(url), 5000);
            });
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            getSharedPreferences("aether", MODE_PRIVATE).edit().clear().apply();
            authToken = "";
            serverUrl = "";
            showLogin();
        }
    }
}
