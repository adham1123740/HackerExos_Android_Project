package com.sonicwave.pro;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import android.view.animation.*;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private LinearLayout soundListContainer;
    private ScrollView scrollView;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RelativeLayout root = new RelativeLayout(this);
        root.setBackgroundColor(Color.parseColor("#0f172a"));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(40, 50, 40, 40);
        header.setBackgroundColor(Color.parseColor("#1e293b"));
        header.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText("SonicWave Pro");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Premium Sound Experience");
        subtitle.setTextColor(Color.parseColor("#94a3b8"));
        subtitle.setTextSize(14);
        header.addView(subtitle);

        root.addView(header, new RelativeLayout.LayoutParams(-1, -2));

        statusText = new TextView(this);
        statusText.setText("Select a sound to play");
        statusText.setTextColor(Color.parseColor("#38bdf8"));
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 20, 0, 20);
        RelativeLayout.LayoutParams statusParams = new RelativeLayout.LayoutParams(-1, -2);
        statusParams.addRule(RelativeLayout.BELOW, header.getId());
        root.addView(statusText, statusParams);

        scrollView = new ScrollView(this);
        RelativeLayout.LayoutParams scrollParams = new RelativeLayout.LayoutParams(-1, -2);
        scrollParams.addRule(RelativeLayout.BELOW, statusText.getId());
        scrollParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        
        soundListContainer = new LinearLayout(this);
        soundListContainer.setOrientation(LinearLayout.VERTICAL);
        soundListContainer.setPadding(30, 20, 30, 50);
        
        scrollView.addView(soundListContainer);
        root.addView(scrollView, scrollParams);

        setupSoundButtons();

        setContentView(root);
    }

    private void setupSoundButtons() {
        String[] sounds = {"Ambient Wave", "Deep Bass", "Crystal Clear", "Sonic Boom", "Night Calm"};
        for (String soundName : sounds) {
            Button btn = new Button(this);
            btn.setText(soundName);
            btn.setTextColor(Color.WHITE);
            btn.setBackgroundColor(Color.parseColor("#334155"));
            btn.setAllCaps(false);
            btn.setPadding(0, 30, 0, 30);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.setMargins(0, 10, 0, 10);
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> playSound(soundName));
            soundListContainer.addView(btn);
        }
    }

    private void playSound(String name) {
        statusText.setText("Playing: " + name);
        // Simulation of sound playback for the UI demo
        Animation scale = new ScaleAnimation(1.0f, 1.1f, 1.0f, 1.1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(200);
        scale.setRepeatMode(Animation.REVERSE);
        scale.setRepeatCount(1);
        statusText.startAnimation(scale);
    }
}