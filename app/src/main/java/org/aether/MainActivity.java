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
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ProgressBar;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private EditText ipInput;
    private EditText msgInput;
    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private LinearLayout setupScreen;
    private LinearLayout chatScreen;
    private TextView statusText;
    private ProgressBar progressBar;
    private Button sendBtn;
    private String serverUrl = "";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#212121"));

        setupScreen = createSetupScreen();
        chatScreen = createChatScreen();

        root.addView(setupScreen);
        root.addView(chatScreen);

        setContentView(root);

        SharedPreferences prefs = getSharedPreferences("aether", MODE_PRIVATE);
        String savedUrl = prefs.getString("server_url", "");
        if (!savedUrl.isEmpty()) {
            serverUrl = savedUrl;
            ipInput.setText(savedUrl.replace("http://", "").replace("https://", ""));
            showChatScreen();
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
        icon.setBackgroundResource(android.R.drawable.dialog_holo_dark_frame);
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

        ipInput = new EditText(this);
        ipInput.setHint("e.g. 192.168.1.100");
        ipInput.setTextColor(Color.WHITE);
        ipInput.setHintTextColor(Color.parseColor("#8e8e8e"));
        ipInput.setBackgroundColor(Color.parseColor("#2f2f2f"));
        ipInput.setPadding(24, 20, 24, 20);
        ipInput.setTextSize(16);
        ipInput.setSingleLine(true);
        LinearLayout.LayoutParams ipP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ipP.bottomMargin = 16;
        ipInput.setLayoutParams(ipP);
        layout.addView(ipInput);

        TextView portLabel = new TextView(this);
        portLabel.setText("Port: 11434 (Ollama)");
        portLabel.setTextSize(12);
        portLabel.setTextColor(Color.parseColor("#666666"));
        portLabel.setPadding(4, 0, 0, 12);
        layout.addView(portLabel);

        Button connectBtn = new Button(this);
        connectBtn.setText("Connect");
        connectBtn.setTextColor(Color.WHITE);
        connectBtn.setBackgroundColor(Color.parseColor("#10a37f"));
        connectBtn.setPadding(24, 16, 24, 16);
        LinearLayout.LayoutParams btnP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnP.bottomMargin = 16;
        connectBtn.setLayoutParams(btnP);
        connectBtn.setOnClickListener(v -> {
            String ip = ipInput.getText().toString().trim();
            if (!ip.isEmpty()) {
                if (!ip.startsWith("http")) ip = "http://" + ip;
                if (!ip.contains(":")) ip += ":11434";
                serverUrl = ip;
                SharedPreferences.Editor ed = getSharedPreferences("aether", MODE_PRIVATE).edit();
                ed.putString("server_url", serverUrl);
                ed.apply();
                testConnection();
            }
        });
        layout.addView(connectBtn);

        statusText = new TextView(this);
        statusText.setTextSize(13);
        statusText.setTextColor(Color.parseColor("#8e8e8e"));
        statusText.setGravity(Gravity.CENTER);
        layout.addView(statusText);

        return layout;
    }

    private LinearLayout createChatScreen() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#212121"));
        layout.setVisibility(View.GONE);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setPadding(20, 16, 20, 16);
        topBar.setBackgroundColor(Color.parseColor("#171717"));

        Button backBtn = new Button(this);
        backBtn.setText("←");
        backBtn.setTextColor(Color.WHITE);
        backBtn.setBackgroundColor(Color.TRANSPARENT);
        backBtn.setTextSize(20);
        backBtn.setOnClickListener(v -> {
            setupScreen.setVisibility(View.VISIBLE);
            chatScreen.setVisibility(View.GONE);
        });
        topBar.addView(backBtn);

        TextView title = new TextView(this);
        title.setText("Aether");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setPadding(16, 0, 0, 0);
        LinearLayout.LayoutParams tP = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tP);
        topBar.addView(title);

        layout.addView(topBar);

        scrollView = new ScrollView(this);
        chatContainer = new LinearLayout(this);
        chatContainer.setOrientation(LinearLayout.VERTICAL);
        chatContainer.setPadding(20, 16, 20, 16);
        scrollView.addView(chatContainer);
        layout.addView(scrollView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout inputArea = new LinearLayout(this);
        inputArea.setOrientation(LinearLayout.HORIZONTAL);
        inputArea.setPadding(16, 12, 16, 16);
        inputArea.setBackgroundColor(Color.parseColor("#171717"));

        msgInput = new EditText(this);
        msgInput.setHint("Message...");
        msgInput.setTextColor(Color.WHITE);
        msgInput.setHintTextColor(Color.parseColor("#8e8e8e"));
        msgInput.setBackgroundColor(Color.parseColor("#2f2f2f"));
        msgInput.setPadding(20, 14, 20, 14);
        msgInput.setTextSize(15);
        msgInput.setSingleLine(true);
        LinearLayout.LayoutParams mP = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        mP.rightMargin = 10;
        msgInput.setLayoutParams(mP);
        msgInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendUserMessage();
                return true;
            }
            return false;
        });
        inputArea.addView(msgInput);

        sendBtn = new Button(this);
        sendBtn.setText("→");
        sendBtn.setTextColor(Color.WHITE);
        sendBtn.setBackgroundColor(Color.parseColor("#10a37f"));
        sendBtn.setTextSize(18);
        sendBtn.setPadding(20, 14, 20, 14);
        sendBtn.setOnClickListener(v -> sendUserMessage());
        inputArea.addView(sendBtn);

        layout.addView(inputArea);

        return layout;
    }

    private void testConnection() {
        statusText.setText("Connecting...");
        statusText.setTextColor(Color.parseColor("#f59e0b"));
        executor.execute(() -> {
            try {
                URL url = new URL(serverUrl + "/api/tags");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                int code = conn.getResponseCode();
                mainHandler.post(() -> {
                    if (code == 200) {
                        showChatScreen();
                    } else {
                        statusText.setText("Connection failed. Check IP.");
                        statusText.setTextColor(Color.parseColor("#ef4444"));
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    statusText.setText("Cannot connect. Is Ollama running?");
                    statusText.setTextColor(Color.parseColor("#ef4444"));
                });
            }
        });
    }

    private void showChatScreen() {
        setupScreen.setVisibility(View.GONE);
        chatScreen.setVisibility(View.VISIBLE);
    }

    private void sendUserMessage() {
        String text = msgInput.getText().toString().trim();
        if (text.isEmpty()) return;

        addBubble(text, true);
        msgInput.setText("");

        ProgressBar loader = new ProgressBar(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 8, 0, 8);
        loader.setLayoutParams(lp);
        chatContainer.addView(loader);
        scrollToBottom();

        final TextView[] aiBubbleHolder = {null};

        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("model", "qwen3:latest");
                body.put("prompt", text);
                body.put("stream", true);

                URL url = new URL(serverUrl + "/api/generate");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(120000);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    try {
                        JSONObject chunk = new JSONObject(line);
                        if (chunk.has("response")) {
                            response.append(chunk.getString("response"));
                            final String partial = response.toString();
                            mainHandler.post(() -> {
                                if (aiBubbleHolder[0] == null) {
                                    loader.setVisibility(View.GONE);
                                    aiBubbleHolder[0] = addBubble(partial, false);
                                } else {
                                    aiBubbleHolder[0].setText(partial);
                                }
                                scrollToBottom();
                            });
                        }
                    } catch (Exception ignored) {}
                }
                reader.close();

                if (aiBubbleHolder[0] == null) {
                    mainHandler.post(() -> {
                        loader.setVisibility(View.GONE);
                        addBubble(response.length() > 0 ? response.toString() : "No response.", false);
                        scrollToBottom();
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    loader.setVisibility(View.GONE);
                    addBubble("Error: " + e.getMessage(), false);
                    scrollToBottom();
                });
            }
        });
    }

    private TextView addBubble(String text, boolean isUser) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        wP.bottomMargin = 16;
        wrapper.setLayoutParams(wP);

        if (isUser) {
            wrapper.setGravity(Gravity.END);
        } else {
            wrapper.setGravity(Gravity.START);
        }

        TextView label = new TextView(this);
        label.setText(isUser ? "You" : "Aether");
        label.setTextSize(12);
        label.setTextColor(Color.parseColor("#8e8e8e"));
        label.setPadding(8, 0, 0, 4);
        wrapper.addView(label);

        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextSize(15);
        bubble.setPadding(20, 14, 20, 14);
        if (isUser) {
            bubble.setTextColor(Color.WHITE);
            bubble.setBackgroundColor(Color.parseColor("#10a37f"));
        } else {
            bubble.setTextColor(Color.parseColor("#ececec"));
            bubble.setBackgroundColor(Color.parseColor("#2f2f2f"));
        }

        LinearLayout.LayoutParams bP = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bP.rightMargin = isUser ? 0 : 40;
        bP.leftMargin = isUser ? 40 : 0;
        bubble.setLayoutParams(bP);
        wrapper.addView(bubble);

        chatContainer.addView(wrapper);
        return bubble;
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}
