package com.s7g.loader;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class FloatingService extends Service {
    private WindowManager wm;
    private View floatingView;
    private WindowManager.LayoutParams params;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundNotification();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100; params.y = 200;

        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x; initialY = params.y;
                        initialTouchX = event.getRawX(); initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        wm.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });
        wm.addView(floatingView, params);
    }

    private void startForegroundNotification() {
        String ch = "esp_overlay";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel nc = new NotificationChannel(ch, "ESP Overlay", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(nc);
        }
        Notification n = (Build.VERSION.SDK_INT >= 26)
            ? new Notification.Builder(this, ch).setContentTitle("ESP يعمل").setSmallIcon(android.R.drawable.ic_menu_view).build()
            : new Notification.Builder(this).setContentTitle("ESP يعمل").setSmallIcon(android.R.drawable.ic_menu_view).build();
        startForeground(1, n);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override
    public void onDestroy() { super.onDestroy(); if (floatingView != null) wm.removeView(floatingView); }
    @Override
    public IBinder onBind(Intent intent) { return null; }
}