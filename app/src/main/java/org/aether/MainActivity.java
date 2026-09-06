package org.aether;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
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
import android.widget.TextView;

public class MainActivity extends Activity {
    private WebView webView;
    private EditText ipInput;
    private LinearLayout setupScreen;
    private LinearLayout webScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#212121"));

        setupScreen = createSetupScreen();
        webScreen = createWebScreen();

        root.addView(setupScreen);
        root.addView(webScreen);

        setContentView(root);

        SharedPreferences prefs = getSharedPreferences("aether", MODE_PRIVATE);
        String savedUrl = prefs.getString("server_url", "");
        if (!savedUrl.isEmpty()) {
            ipInput.setText(savedUrl.replace("http://", ""));
            webScreen.setVisibility(View.VISIBLE);
            setupScreen.setVisibility(View.GONE);
            webView.loadUrl(savedUrl);
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
        subtitle.setText("PC's IP address (port 5000)");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.parseColor("#8e8e8e"));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 40);
        layout.addView(subtitle);

        ipInput = new EditText(this);
        ipInput.setHint("e.g. 192.168.1.100:5000");
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
        LinearLayout.LayoutParams btnP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        connectBtn.setLayoutParams(btnP);
        connectBtn.setOnClickListener(v -> {
            String ip = ipInput.getText().toString().trim();
            if (!ip.isEmpty()) {
                if (!ip.startsWith("http")) ip = "http://" + ip;
                if (!ip.contains(":")) ip += ":5000";
                SharedPreferences.Editor ed = getSharedPreferences("aether", MODE_PRIVATE).edit();
                ed.putString("server_url", ip);
                ed.apply();
                setupScreen.setVisibility(View.GONE);
                webScreen.setVisibility(View.VISIBLE);
                webView.loadUrl(ip);
            }
        });
        layout.addView(connectBtn);

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

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(12, 8, 12, 8);
        bar.setBackgroundColor(Color.parseColor("#171717"));

        Button reloadBtn = new Button(this);
        reloadBtn.setText("Refresh");
        reloadBtn.setTextColor(Color.WHITE);
        reloadBtn.setBackgroundColor(Color.parseColor("#2f2f2f"));
        reloadBtn.setTextSize(12);
        reloadBtn.setPadding(16, 8, 16, 8);
        reloadBtn.setOnClickListener(v -> webView.reload());
        bar.addView(reloadBtn);

        Button logoutBtn = new Button(this);
        logoutBtn.setText("Change Server");
        logoutBtn.setTextColor(Color.WHITE);
        logoutBtn.setBackgroundColor(Color.parseColor("#2f2f2f"));
        logoutBtn.setTextSize(12);
        logoutBtn.setPadding(16, 8, 16, 8);
        LinearLayout.LayoutParams lP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lP.leftMargin = 8;
        logoutBtn.setLayoutParams(lP);
        logoutBtn.setOnClickListener(v -> {
            getSharedPreferences("aether", MODE_PRIVATE).edit().clear().apply();
            webView.loadUrl("about:blank");
            webScreen.setVisibility(View.GONE);
            setupScreen.setVisibility(View.VISIBLE);
        });
        bar.addView(logoutBtn);

        layout.addView(bar);

        return layout;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
