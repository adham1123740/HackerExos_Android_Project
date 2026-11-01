package com.wormgpt.wormpanel;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_OVERLAY = 100;
    private static final int REQ_CAPTURE = 200;
    private MediaProjectionManager mpm;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        
        mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        TextView statusText = findViewById(R.id.statusText);
        ProgressBar progress = findViewById(R.id.progress);
        Button btnDownload = findViewById(R.id.btnDownload);
        Button btnStart = findViewById(R.id.btnStart);

        btnDownload.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        
        statusText.setText("الحالة: جاهز للتشغيل");
        btnStart.setText("تشغيل ESP");
        btnStart.setEnabled(true);

        btnStart.setOnClickListener(v -> checkAndRequestOverlay());
    }

    private void checkAndRequestOverlay() {
        // لو الصلاحية مش مفعلة، نطلبها
        if (!Settings.canDrawOverlays(this)) {
            try {
                // محاولة فتح شاشة الصلاحية المخصصة للتطبيق
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQ_OVERLAY);
            } catch (Exception e) {
                // لو فشلت (زي ما بيحصل في أندرويد 16)، نفتح القائمة العامة
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    startActivityForResult(intent, REQ_OVERLAY);
                } catch (Exception ex) {
                    Toast.makeText(this, "افتح الإعدادات > التطبيقات > الوصول الخاص > الظهور فوق التطبيقات", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            startScreenCapture();
        }
    }

    private void startScreenCapture() {
        try {
            startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE);
        } catch (Exception e) {
            Toast.makeText(this, "فشل بدء تصوير الشاشة", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        
        if (req == REQ_OVERLAY) {
            // بعد ما المستخدم يرجع من الإعدادات، نتأكد تاني
            if (Settings.canDrawOverlays(this)) {
                startScreenCapture();
            } else {
                Toast.makeText(this, "لازم تفعل صلاحية 'الظهور فوق التطبيقات' عشان ESP يشتغل", Toast.LENGTH_LONG).show();
            }
        } else if (req == REQ_CAPTURE) {
            if (res == Activity.RESULT_OK && data != null) {
                Intent svc = new Intent(this, OverlayService.class);
                svc.setAction(OverlayService.ACTION_START);
                svc.putExtra("code", res);
                svc.putExtra("data", data);
                startForegroundService(svc);
                Toast.makeText(this, "ESP يعمل الآن", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "مشاركة الشاشة مرفوضة", Toast.LENGTH_SHORT).show();
            }
        }
    }
}