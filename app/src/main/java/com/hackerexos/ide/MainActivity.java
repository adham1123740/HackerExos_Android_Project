package com.wormgpt.soundapp;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
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

        // Root Layout
        RelativeLayout root = new RelativeLayout(this);
        root.setBackgroundColor(Color.parseColor("#0b141a"));

        // Header (Toolbar)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(30, 30, 30, 30);
        header.setBackgroundColor(Color.parseColor("#202c33"));
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView headerTitle = new TextView(this);
        headerTitle.setText("WormGPT Chat");
        headerTitle.setTextColor(Color.WHITE);
        headerTitle.setTextSize(20);
        headerTitle.setTypeface(null, Typeface.BOLD);
        header.addView(headerTitle);

        root.addView(header, new RelativeLayout.LayoutParams(-1, -2));

        // Chat Area
        scrollView = new ScrollView(this);
        RelativeLayout.LayoutParams scrollParams = new RelativeLayout.LayoutParams(-1, -2);
        scrollParams.addRule(RelativeLayout.BELOW, header);
        scrollParams.addRule(RelativeLayout.ABOVE, 500); // Placeholder for input
        
        chatContainer = new LinearLayout(this);
        chatContainer.setOrientation(LinearLayout.VERTICAL);
        chatContainer.setPadding(30, 30, 30, 30);
        chatContainer.setGravity(Gravity.BOTTOM);
        
        scrollView.addView(chatContainer);
        root.addView(scrollView, scrollParams);

        // Input Area
        LinearLayout inputLayout = new LinearLayout(this);
        inputLayout.setOrientation(LinearLayout.HORIZONTAL);
        inputLayout.setPadding(20, 20, 20, 20);
        inputLayout.setBackgroundColor(Color.parseColor("#202c33"));
        inputLayout.setGravity(Gravity.CENTER_VERTICAL);

        messageInput = new EditText(this);
        messageInput.setHint("اكتب رسالتك هنا...");
        messageInput.setHintTextColor(Color.GRAY);
        messageInput.setTextColor(Color.WHITE);
        messageInput.setBackgroundColor(Color.parseColor("#2a3942"));
        messageInput.setPadding(30, 30, 30, 30);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, -2, 1f);
        
        sendButton = new Button(this);
        sendButton.setText("إرسال");
        sendButton.setBackgroundColor(Color.parseColor("#00a884"));
        sendButton.setTextColor(Color.WHITE);

        inputLayout.addView(messageInput, inputParams);
        inputLayout.addView(sendButton);

        RelativeLayout.LayoutParams inputRootParams = new RelativeLayout.LayoutParams(-1, -2);
        inputRootParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        root.addView(inputLayout, inputRootParams);

        sendButton.setOnClickListener(v -> {
            String msg = messageInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                addMessage(msg, true);
                messageInput.setText("");
            }
        });

        setContentView(root);
        
        // Welcome Message
        addMessage("مرحباً بك في واجهة WormGPT الخرافية! 🚀", false);
    }

    private void addMessage(String text, boolean isMe) {
        TextView msgView = new TextView(this);
        msgView.setText(text);
        msgView.setPadding(40, 20, 40, 20);
        msgView.setTextSize(16);
        msgView.setTextColor(Color.WHITE);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMargins(0, 10, 0, 10);

        if (isMe) {
            msgView.setBackgroundColor(Color.parseColor("#005c4b"));
            params.gravity = Gravity.END;
        } else {
            msgView.setBackgroundColor(Color.parseColor("#202c33"));
            params.gravity = Gravity.START;
        }

        msgView.setLayoutParams(params);
        
        // Animation
        Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(500);
        Animation slideUp = new TranslateAnimation(0, 0, 100, 0);
        slideUp.setDuration(500);
        
        msgView.startAnimation(fadeIn);
        msgView.startAnimation(slideUp);

        chatContainer.addView(msgView);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}