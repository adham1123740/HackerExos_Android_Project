package com.wormgpt.exo;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.view.animation.*;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private LinearLayout chatContainer;
    private EditText messageInput;
    private Button sendButton;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermissions();

        RelativeLayout root = new RelativeLayout(this);
        root.setBackgroundColor(Color.parseColor("#0a0a0a"));

        LinearLayout header = new LinearLayout(this);
        header.setId(View.generateViewId());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(40, 50, 40, 40);
        header.setBackgroundColor(Color.parseColor("#121212"));
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView headerTitle = new TextView(this);
        headerTitle.setText("WormGPT AI");
        headerTitle.setTextColor(Color.WHITE);
        headerTitle.setTextSize(22);
        headerTitle.setTypeface(null, Typeface.BOLD);
        header.addView(headerTitle);
        root.addView(header, new RelativeLayout.LayoutParams(-1, -2));

        scrollView = new ScrollView(this);
        scrollView.setId(View.generateViewId());
        RelativeLayout.LayoutParams scrollParams = new RelativeLayout.LayoutParams(-1, -2);
        scrollParams.addRule(RelativeLayout.BELOW, header.getId());
        scrollParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        chatContainer = new LinearLayout(this);
        chatContainer.setOrientation(LinearLayout.VERTICAL);
        chatContainer.setPadding(30, 30, 30, 30);
        scrollView.addView(chatContainer);
        root.addView(scrollView, scrollParams);

        LinearLayout inputLayout = new LinearLayout(this);
        inputLayout.setOrientation(LinearLayout.HORIZONTAL);
        inputLayout.setPadding(20, 20, 20, 50);
        inputLayout.setGravity(Gravity.CENTER_VERTICAL);
        inputLayout.setBackgroundColor(Color.parseColor("#121212"));

        messageInput = new EditText(this);
        messageInput.setHint("Ask WormGPT...");
        messageInput.setHintTextColor(Color.GRAY);
        messageInput.setTextColor(Color.WHITE);
        messageInput.setBackground(null);
        messageInput.setPadding(30, 30, 30, 30);

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(100f);
        inputBg.setColor(Color.parseColor("#1f1f1f"));

        LinearLayout inputWrapper = new LinearLayout(this);
        inputWrapper.setBackground(inputBg);
        inputWrapper.setGravity(Gravity.CENTER_VERTICAL);
        inputWrapper.setPadding(20, 0, 20, 0);
        inputWrapper.addView(messageInput, new LinearLayout.LayoutParams(0, -2, 1f));

        sendButton = new Button(this);
        sendButton.setText("➤");
        sendButton.setTextColor(Color.WHITE);
        sendButton.setBackgroundColor(Color.parseColor("#3d5afe"));
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setCornerRadius(100f);
        btnBg.setColor(Color.parseColor("#3d5afe"));
        sendButton.setBackground(btnBg);
        sendButton.setAllCaps(false);
        sendButton.setPadding(20, 0, 20, 0);

        inputLayout.addView(inputWrapper, new LinearLayout.LayoutParams(0, -2, 1f));
        inputLayout.addView(sendButton, new LinearLayout.LayoutParams(-2, -2));

        RelativeLayout.LayoutParams inputParams = new RelativeLayout.LayoutParams(-1, -2);
        inputParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        root.addView(inputLayout, inputParams);

        sendButton.setOnClickListener(v -> {
            String text = messageInput.getText().toString();
            if(!text.isEmpty()) {
                addMessage(text, true);
                messageInput.setText("");
                ScrollView.postDelayed(() -> scrollView.fullScroll(View.FOCUS_DOWN), 100);
                postDelayed(() -> addMessage("Executing request via WormGPT engine...", false), 1000);
            }
        });

        setContentView(root);
    }

    private void addMessage(String text, boolean isUser) {
        TextView msg = new TextView(this);
        msg.setText(text);
        msg.setTextColor(Color.WHITE);
        msg.setTextSize(16);
        msg.setPadding(40, 20, 40, 20);

        GradientDrawable bubble = new GradientDrawable();
        bubble.setCornerRadius(50f);
        if(isUser) {
            bubble.setColor(Color.parseColor("#3d5afe"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
            params.gravity = Gravity.END;
            params.setMargins(0, 20, 0, 20);
            msg.setLayoutParams(params);
        } else {
            bubble.setColor(Color.parseColor("#252525"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
            params.gravity = Gravity.START;
            params.setMargins(0, 20, 0, 20);
            msg.setLayoutParams(params);
        }
        msg.setBackground(bubble);
        chatContainer.addView(msg);
    }

    private void requestPermissions() {
        int mic = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO);
        int storage = checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(mic != PackageManager.PERMISSION_GRANTED || storage != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }
    }
}