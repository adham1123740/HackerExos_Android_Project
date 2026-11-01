package com.wormgpt.wormpanel;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingService extends Service {

    private WindowManager windowManager;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;

    private TextView bubble;
    private LinearLayout panel;

    private boolean panelVisible = false;

    private int dp(float value) {
        return (int) (value * getResources()
            .getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        windowManager = (WindowManager)
            getSystemService(WINDOW_SERVICE);

        createBubble();
        createPanel();
    }

    private int getWindowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }

        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private WindowManager.LayoutParams createParams(
        int width,
        int height,
        int gravity
    ) {

        return new WindowManager.LayoutParams(
            width,
            height,
            getWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
    }

    private void createBubble() {

        bubble = new TextView(this);
        bubble.setText("W");
        bubble.setTextSize(23);
        bubble.setTypeface(Typeface.DEFAULT_BOLD);
        bubble.setTextColor(Color.WHITE);
        bubble.setGravity(Gravity.CENTER);
        bubble.setBackgroundColor(Color.rgb(190, 15, 40));

        bubbleParams = createParams(
            dp(58),
            dp(58),
            Gravity.TOP | Gravity.START
        );

        bubbleParams.x = dp(20);
        bubbleParams.y = dp(180);

        bubble.setOnTouchListener(new View.OnTouchListener() {

            private float downRawX;
            private float downRawY;
            private int startX;
            private int startY;
            private long downTime;
            private boolean moved;

            @Override
            public boolean onTouch(View view, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        downRawX = event.getRawX();
                        downRawY = event.getRawY();

                        startX = bubbleParams.x;
                        startY = bubbleParams.y;

                        downTime = System.currentTimeMillis();
                        moved = false;

                        return true;

                    case MotionEvent.ACTION_MOVE:

                        float dx = event.getRawX() - downRawX;
                        float dy = event.getRawY() - downRawY;

                        if (Math.abs(dx) > dp(5)
                            || Math.abs(dy) > dp(5)) {

                            moved = true;
                        }

                        if (moved) {

                            bubbleParams.x = startX + (int) dx;
                            bubbleParams.y = startY + (int) dy;

                            try {
                                windowManager.updateViewLayout(
                                    bubble,
                                    bubbleParams
                                );
                            } catch (Exception ignored) {
                            }
                        }

                        return true;

                    case MotionEvent.ACTION_UP:

                        long duration =
                            System.currentTimeMillis() - downTime;

                        if (!moved && duration < 350) {
                            togglePanel();
                        }

                        return true;
                }

                return false;
            }
        });

        windowManager.addView(bubble, bubbleParams);
    }

    private void createPanel() {

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(12), dp(14), dp(14));
        panel.setBackgroundColor(Color.rgb(20, 20, 25));

        TextView header = new TextView(this);

        header.setText("WORMPANEL  [ DRAG ]");
        header.setTextColor(Color.rgb(255, 45, 65));
        header.setTextSize(17);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setPadding(0, dp(5), 0, dp(14));

        panel.addView(header);

        header.setOnTouchListener(new View.OnTouchListener() {

            private float downX;
            private float downY;
            private int startX;
            private int startY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {

                    downX = event.getRawX();
                    downY = event.getRawY();

                    startX = panelParams.x;
                    startY = panelParams.y;

                    return true;
                }

                if (event.getAction() == MotionEvent.ACTION_MOVE) {

                    panelParams.x = startX
                        + (int) (event.getRawX() - downX);

                    panelParams.y = startY
                        + (int) (event.getRawY() - downY);

                    try {
                        windowManager.updateViewLayout(
                            panel,
                            panelParams
                        );
                    } catch (Exception ignored) {
                    }

                    return true;
                }

                return event.getAction() == MotionEvent.ACTION_UP;
            }
        });

        addPanelButton("ESP DEMO", () -> {
            Toast.makeText(
                this,
                "Demo mode: no real enemy detection",
                Toast.LENGTH_LONG
            ).show();

            showDemo();
        });

        addPanelButton("SCREEN CAPTURE", () -> {

            Intent intent = new Intent(
                this,
                MainActivity.class
            );

            intent.putExtra("request_capture", true);

            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(intent);
        });

        addPanelButton("OPEN PUBG", () -> {

            Intent launch = getPackageManager()
                .getLaunchIntentForPackage("com.tencent.ig");

            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);
            } else {
                Toast.makeText(
                    this,
                    "PUBG Mobile not found",
                    Toast.LENGTH_SHORT
                ).show();
            }
        });

        addPanelButton("HIDE PANEL", this::togglePanel);

        panelParams = createParams(
            dp(250),
            WindowManager.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.START
        );

        panelParams.x = dp(85);
        panelParams.y = dp(180);
    }

    private void addPanelButton(String text, Runnable action) {

        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);

        button.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(150, 15, 35)
            )
        );

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            );

        params.setMargins(0, dp(4), 0, dp(4));

        panel.addView(button, params);
        button.setOnClickListener(v -> action.run());
    }

    private void showDemo() {

        TextView demo = new TextView(this);

        demo.setText(
            "ESP DEMONSTRATION\n\n"
            + "Status: DEMO ONLY\n"
            + "Game data: Not accessed\n"
            + "Enemy tracking: Not available"
        );

        demo.setTextColor(Color.WHITE);
        demo.setTextSize(14);
        demo.setPadding(dp(12), dp(12), dp(12), dp(12));
        demo.setBackgroundColor(Color.rgb(45, 25, 30));

        panel.addView(
            demo,
            Math.max(0, panel.getChildCount() - 1)
        );
    }

    private void togglePanel() {

        if (panelVisible) {

            try {
                windowManager.removeView(panel);
            } catch (Exception ignored) {
            }

            panelVisible = false;

        } else {

            try {
                windowManager.addView(panel, panelParams);
                panelVisible = true;
            } catch (Exception e) {
                Toast.makeText(
                    this,
                    "Could not display panel",
                    Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    @Override
    public void onDestroy() {

        if (windowManager != null) {

            try {
                if (bubble != null && bubble.isAttachedToWindow()) {
                    windowManager.removeView(bubble);
                }
            } catch (Exception ignored) {
            }

            try {
                if (panel != null && panel.isAttachedToWindow()) {
                    windowManager.removeView(panel);
                }
            } catch (Exception ignored) {
            }
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}