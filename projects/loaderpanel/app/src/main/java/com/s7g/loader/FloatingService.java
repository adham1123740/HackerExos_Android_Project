package com.s7g.loader;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingService extends Service {
    private WindowManager wm;
    private LinearLayout floatingView;
    private WindowManager.LayoutParams params;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundNotification();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        floatingView = new LinearLayout(this);
        floatingView.setOrientation(LinearLayout.VERTICAL);
        floatingView.setPadding(30, 30, 30, 30);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#E60F0B1A"));
        gd.setCornerRadius(30);
        gd.setStroke(3, Color.parseColor("#D500F9"));
        floatingView.setBackground(gd);

        TextView title = new TextView(this);
        title.setText("EXOS MENU");
        title.setTextColor(Color.parseColor("#D500F9"));
        title.setGravity(Gravity.CENTER);
        title.setTextSize(16);
        title.setPadding(0, 0, 0, 20);
        floatingView.addView(title);

        Switch swEsp = new Switch(this);
        swEsp.setText("ESP | رؤية الأعداء");
        swEsp.setTextColor(Color.WHITE);
        swEsp.setOnCheckedChangeListener((v, isChecked) -> Toast.makeText(this, "ESP: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show());
        floatingView.addView(swEsp);

        Switch swAim = new Switch(this);
        swAim.setText("AIMBOT | التصويب");
        swAim.setTextColor(Color.WHITE);
        swAim.setPadding(0, 0, 0, 20);
        swAim.setOnCheckedChangeListener((v, isChecked) -> Toast.makeText(this, "AIM: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show());
        floatingView.addView(swAim);

        Button btnClose = new Button(this);
        btnClose.setText("إغلاق القائمة");
        GradientDrawable gdBtn = new GradientDrawable();
        gdBtn.setColor(Color.parseColor("#FF1744"));
        gdBtn.setCornerRadius(15);
        btnClose.setBackground(gdBtn);
        btnClose.setTextColor(Color.WHITE);
        btnClose.setOnClickListener(v -> stopSelf());
        floatingView.addView(btnClose);

        int layoutFlag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 200;

        title.setOnTouchListener(new View.OnTouchListener() {
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
        String ch = "exos_channel";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel nc = new NotificationChannel(ch, "Exos Service", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(nc);
        }
        Notification n = (Build.VERSION.SDK_INT >= 26)
            ? new Notification.Builder(this, ch).setContentTitle("Panel Exos يعمل").setSmallIcon(android.R.drawable.ic_menu_view).build()
            : new Notification.Builder(this).setContentTitle("Panel Exos يعمل").setSmallIcon(android.R.drawable.ic_menu_view).build();
        startForeground(1, n);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) wm.removeView(floatingView);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}