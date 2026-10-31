package com.wormgpt.soundapp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private ToneGenerator tone;
    private Handler handler = new Handler();
    private boolean isPlaying = false;

    private TextView status;
    private Button startButton;
    private Button stopButton;

    private final Runnable soundRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPlaying || tone == null) return;

            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 500);
            handler.postDelayed(this, 800);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tone = new ToneGenerator(
            AudioManager.STREAM_MUSIC,
            85
        );

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(25, 25, 25, 25);
        layout.setBackgroundColor(Color.rgb(15, 15, 20));

        TextView title = new TextView(this);
        title.setText("Sound Controller");
        title.setTextSize(28);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams titleParams =
            new LinearLayout.LayoutParams(-1, -2);
        titleParams.bottomMargin = 35;

        layout.addView(title, titleParams);

        status = new TextView(this);
        status.setText("الصوت متوقف");
        status.setTextSize(18);
        status.setTextColor(Color.LTGRAY);
        status.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams statusParams =
            new LinearLayout.LayoutParams(-1, -2);
        statusParams.bottomMargin = 35;

        layout.addView(status, statusParams);

        startButton = new Button(this);
        startButton.setText("▶ تشغيل الصوت");
        startButton.setTextSize(17);

        stopButton = new Button(this);
        stopButton.setText("■ إيقاف الصوت");
        stopButton.setTextSize(17);

        LinearLayout.LayoutParams buttonParams =
            new LinearLayout.LayoutParams(-1, 60);

        buttonParams.bottomMargin = 15;

        layout.addView(startButton, buttonParams);
        layout.addView(stopButton, buttonParams);

        startButton.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(210, 35, 65)
            )
        );

        stopButton.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(65, 65, 75)
            )
        );

        startButton.setTextColor(Color.WHITE);
        stopButton.setTextColor(Color.WHITE);

        startButton.setOnClickListener(v -> startSound());
        stopButton.setOnClickListener(v -> stopSound());

        setContentView(layout);
    }

    private void startSound() {
        if (isPlaying) return;

        isPlaying = true;

        status.setText("● الصوت يعمل الآن");
        status.setTextColor(Color.GREEN);

        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        handler.post(soundRunnable);
    }

    private void stopSound() {
        isPlaying = false;

        handler.removeCallbacks(soundRunnable);

        if (tone != null) {
            tone.stopTone();
        }

        status.setText("الصوت متوقف");
        status.setTextColor(Color.LTGRAY);

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        stopSound();

        if (tone != null) {
            tone.release();
            tone = null;
        }

        handler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }
}
