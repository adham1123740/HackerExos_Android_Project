package com.example.myapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;

import android.content.Context;
import android.content.Intent;

import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;

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

public class OverlayService extends Service {

    public static final String ACTION_START =
            "com.example.myapp.START_OVERLAY";

    private static final String CHANNEL_ID = "overlay_demo_channel";
    private static final int NOTIFICATION_ID = 701;

    private WindowManager windowManager;

    private View floatingBox;
    private View controlPanel;

    private WindowManager.LayoutParams boxParams;
    private WindowManager.LayoutParams panelParams;

    private boolean boxAdded = false;
    private boolean panelAdded = false;
    private boolean effectEnabled = true;

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager)
                getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {
        if (intent == null ||
                !ACTION_START.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, createNotification());

            if (!boxAdded && !panelAdded) {
                createFloatingBox();
                createControlPanel();
            }

        } catch (Exception e) {
            removeOverlayViews();
            stopSelf();
            return START_NOT_STICKY;
        }

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager =
                    (NotificationManager)
                            getSystemService(NOTIFICATION_SERVICE);

            if (manager != null) {
                NotificationChannel channel =
                        new NotificationChannel(
                                CHANNEL_ID,
                                "Overlay Demo",
                                NotificationManager.IMPORTANCE_LOW
                        );

                channel.setDescription(
                        "حالة تشغيل الواجهة العائمة"
                );

                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Overlay Demo")
                    .setContentText("الواجهة العائمة تعمل")
                    .setSmallIcon(
                            android.R.drawable.ic_menu_view
                    )
                    .setOngoing(true)
                    .build();
        }

        return new Notification.Builder(this)
                .setContentTitle("Overlay Demo")
                .setContentText("الواجهة العائمة تعمل")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build();
    }

    private GradientDrawable createBackground(
            int backgroundColor,
            int borderColor,
            float radius
    ) {
        GradientDrawable drawable = new GradientDrawable();

        drawable.setColor(backgroundColor);
        drawable.setCornerRadius(radius);
        drawable.setStroke(3, borderColor);

        return drawable;
    }

    private WindowManager.LayoutParams createLayoutParams(
            int width,
            int height
    ) {
        int type;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        return new WindowManager.LayoutParams(
                width,
                height,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
    }

    private void createFloatingBox() {
        if (windowManager == null || boxAdded) {
            return;
        }

        TextView box = new TextView(this);

        box.setText("H");
        box.setTextColor(Color.GREEN);
        box.setTextSize(24);
        box.setGravity(Gravity.CENTER);

        box.setBackground(
                createBackground(
                        Color.rgb(20, 25, 25),
                        Color.GREEN,
                        28
                )
        );

        floatingBox = box;

        boxParams = createLayoutParams(110, 110);
        boxParams.gravity = Gravity.TOP | Gravity.START;
        boxParams.x = 80;
        boxParams.y = 250;

        box.setOnTouchListener(
                new FloatingTouchListener(
                        boxParams,
                        box,
                        this::showControlPanel
                )
        );

        windowManager.addView(floatingBox, boxParams);
        boxAdded = true;
    }

    private void createControlPanel() {
        if (windowManager == null || panelAdded) {
            return;
        }

        LinearLayout panel = new LinearLayout(this);

        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(24, 20, 24, 20);

        panel.setBackground(
                createBackground(
                        Color.rgb(18, 18, 22),
                        Color.GREEN,
                        24
                )
        );

        TextView title = new TextView(this);

        title.setText("OVERLAY CONTROL");
        title.setTextColor(Color.GREEN);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 16);

        panel.addView(title);

        TextView status = new TextView(this);

        status.setText("الحالة: جاهز");
        status.setTextColor(Color.WHITE);
        status.setTextSize(15);
        status.setPadding(0, 0, 0, 12);

        panel.addView(status);

        Button toggleButton = new Button(this);

        toggleButton.setText("التأثير: ON");
        toggleButton.setTextSize(13);

        panel.addView(toggleButton);

        toggleButton.setOnClickListener(v -> {
            effectEnabled = !effectEnabled;

            toggleButton.setText(
                    effectEnabled
                            ? "التأثير: ON"
                            : "التأثير: OFF"
            );

            status.setText(
                    effectEnabled
                            ? "الحالة: مفعّل"
                            : "الحالة: متوقف"
            );
        });

        Button hideButton = new Button(this);

        hideButton.setText("إخفاء اللوحة");
        panel.addView(hideButton);

        hideButton.setOnClickListener(v -> {
            if (controlPanel != null && panelAdded) {
                controlPanel.setVisibility(View.GONE);
            }

            if (floatingBox != null && boxAdded) {
                floatingBox.setVisibility(View.VISIBLE);
            }
        });

        Button closeButton = new Button(this);

        closeButton.setText("إيقاف التطبيق العائم");
        panel.addView(closeButton);

        closeButton.setOnClickListener(v -> stopSelf());

        controlPanel = panel;

        panelParams = createLayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
        );

        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = 70;
        panelParams.y = 350;

        panel.setOnTouchListener(
                new FloatingTouchListener(
                        panelParams,
                        panel,
                        null
                )
        );

        windowManager.addView(controlPanel, panelParams);
        panelAdded = true;

        controlPanel.setVisibility(View.GONE);
    }

    private void showControlPanel() {
        if (controlPanel != null && panelAdded) {
            controlPanel.setVisibility(View.VISIBLE);
        }

        if (floatingBox != null && boxAdded) {
            floatingBox.setVisibility(View.GONE);
        }
    }

    private void removeOverlayViews() {
        if (windowManager == null) {
            return;
        }

        if (floatingBox != null && boxAdded) {
            try {
                windowManager.removeView(floatingBox);
            } catch (Exception ignored) {
            }
        }

        if (controlPanel != null && panelAdded) {
            try {
                windowManager.removeView(controlPanel);
            } catch (Exception ignored) {
            }
        }

        floatingBox = null;
        controlPanel = null;

        boxAdded = false;
        panelAdded = false;
    }

    @Override
    public void onDestroy() {
        removeOverlayViews();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class FloatingTouchListener
            implements View.OnTouchListener {

        private final WindowManager.LayoutParams params;
        private final View target;
        private final Runnable onClick;

        private int startX;
        private int startY;

        private float touchX;
        private float touchY;

        private boolean dragging;
        private long downTime;

        FloatingTouchListener(
                WindowManager.LayoutParams params,
                View target,
                Runnable onClick
        ) {
            this.params = params;
            this.target = target;
            this.onClick = onClick;
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {

            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN:

                    startX = params.x;
                    startY = params.y;

                    touchX = event.getRawX();
                    touchY = event.getRawY();

                    downTime = System.currentTimeMillis();

                    dragging = false;

                    return true;

                case MotionEvent.ACTION_MOVE:

                    int dx = (int) (event.getRawX() - touchX);
                    int dy = (int) (event.getRawY() - touchY);

                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                        dragging = true;
                    }

                    if (dragging) {
                        params.x = startX + dx;
                        params.y = startY + dy;

                        try {
                            if (target.getParent() != null) {
                                windowManager.updateViewLayout(
                                        target,
                                        params
                                );
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    return true;

                case MotionEvent.ACTION_UP:

                    long duration =
                            System.currentTimeMillis() - downTime;

                    if (!dragging && duration < 300) {
                        if (onClick != null) {
                            onClick.run();
                        }
                    }

                    return true;

                case MotionEvent.ACTION_CANCEL:
                    return true;

                default:
                    return false;
            }
        }
    }
}