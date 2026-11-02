package com.s7g.loader;

import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class FloatingService extends Service {
    private WindowManager wm;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isViewAdded = false;

    // فحص ما إذا كانت ببجي هي التطبيق المفتوح حالياً
    private Runnable checkGameRunnable = new Runnable() {
        @Override
        public void run() {
            if (isGameInForeground()) {
                if (!isViewAdded) {
                    wm.addView(floatingView, params);
                    isViewAdded = true;
                }
            } else {
                if (isViewAdded) {
                    wm.removeView(floatingView);
                    isViewAdded = false;
                }
            }
            handler.postDelayed(this, 1000); // يفحص كل ثانية
        }
    };

    private boolean isGameInForeground() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        java.util.List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);
        if (appList != null && !appList.isEmpty()) {
            String topPackage = null;
            long lastTime = 0;
            for (UsageStats usageStats : appList) {
                if (usageStats.getLastTimeUsed() > lastTime) {
                    lastTime = usageStats.getLastTimeUsed();
                    topPackage = usageStats.getPackageName();
                }
            }
            return "com.tencent.ig".equals(topPackage);
        }
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
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

        // تفعيل السحب
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

        handler.post(checkGameRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(checkGameRunnable);
        if (isViewAdded && floatingView != null) {
            wm.removeView(floatingView);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}