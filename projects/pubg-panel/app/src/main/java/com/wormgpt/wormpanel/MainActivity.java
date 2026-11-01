package com.wormgpt.wormpanel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int OVERLAY_REQUEST = 1001;
    private static final int CAPTURE_REQUEST = 1002;

    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildUI();

        if (getIntent().getBooleanExtra("request_capture", false)) {
            requestScreenCapture();
        }
    }

    private void buildUI() {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(30), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(13, 13, 17));

        TextView title = new TextView(this);
        title.setText("WORMPANEL");
        title.setTextSize(28);
        title.setTextColor(Color.rgb(255, 45, 65));
        title.setTypeface(Typeface.DEFAULT_BOLD);

        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("FLOATING CONTROL CENTER");
        subtitle.setTextSize(12);
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setPadding(0, dp(6), 0, dp(30));

        root.addView(subtitle);

        TextView status = new TextView(this);
        status.setText("● SYSTEM READY");
        status.setTextColor(Color.GREEN);
        status.setTextSize(14);

        root.addView(status);

        addButton("ENABLE FLOATING PANEL", v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );

                startActivityForResult(intent, OVERLAY_REQUEST);
            } else {
                startFloatingService();
            }
        });

        addButton("SCREEN CAPTURE PERMISSION", v -> {
            requestScreenCapture();
        });

        addButton("OPEN PUBG MOBILE", v -> {
            launchPUBG();
        });

        addButton("STOP FLOATING PANEL", v -> {
            stopService(new Intent(this, FloatingService.class));
            Toast.makeText(
                this,
                "Floating panel stopped",
                Toast.LENGTH_SHORT
            ).show();
        });

        TextView footer = new TextView(this);
        footer.setText(
            "\nWormPanel v1.0\n" +
            "Android Floating Tools\n" +
            "ESP Demo does not read game data."
        );

        footer.setTextColor(Color.GRAY);
        footer.setTextSize(12);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(35), 0, 0);

        root.addView(footer);

        setContentView(root);
    }

    private void addButton(String text, View.OnClickListener listener) {

        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(180, 20, 40)
            )
        );

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            );

        params.setMargins(0, dp(10), 0, dp(5));

        root.addView(button, params);
        button.setOnClickListener(listener);
    }

    private void startFloatingService() {

        Intent intent = new Intent(this, FloatingService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        Toast.makeText(
            this,
            "Floating panel enabled",
            Toast.LENGTH_SHORT
        ).show();
    }

    private void requestScreenCapture() {

        MediaProjectionManager manager =
            (MediaProjectionManager) getSystemService(
                Context.MEDIA_PROJECTION_SERVICE
            );

        if (manager != null) {
            startActivityForResult(
                manager.createScreenCaptureIntent(),
                CAPTURE_REQUEST
            );
        }
    }

    private void launchPUBG() {

        // Global PUBG Mobile package name.
        String packageName = "com.tencent.ig";

        Intent launchIntent =
            getPackageManager().getLaunchIntentForPackage(packageName);

        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            Toast.makeText(
                this,
                "PUBG Mobile was not found. Your version may use a different package name.",
                Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == OVERLAY_REQUEST) {

            if (Settings.canDrawOverlays(this)) {
                startFloatingService();
            } else {
                Toast.makeText(
                    this,
                    "Overlay permission was not granted",
                    Toast.LENGTH_SHORT
                ).show();
            }

        } else if (requestCode == CAPTURE_REQUEST) {

            if (resultCode == RESULT_OK && data != null) {
                Toast.makeText(
                    this,
                    "Screen capture permission granted for this session",
                    Toast.LENGTH_LONG
                ).show();
            } else {
                Toast.makeText(
                    this,
                    "Screen capture permission denied",
                    Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private int dp(float value) {
        return (int) (value * getResources()
            .getDisplayMetrics().density + 0.5f);
    }
}