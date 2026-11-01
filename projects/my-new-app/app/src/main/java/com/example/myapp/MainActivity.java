package com.example.myapp;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
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

        Button btnDownload = findViewById(R.id.btnDownload);
        Button btnStart = findViewById(R.id.btnStart);
        TextView statusText = findViewById(R.id.statusText);
        ProgressBar progress = findViewById(R.id.progress);

        btnDownload.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        
        statusText.setText("الحالة: جاهز للتشغيل");
        btnStart.setEnabled(true);
        btnStart.setText("تفعيل ESP");

        btnStart.setOnClickListener(v -> checkOverlay());
    }

    private void checkOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), REQ_OVERLAY);
        } else {
            startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) checkOverlay();
            else Toast.makeText(this, "لازم تفعل صلاحية الظهور فوق التطبيقات", Toast.LENGTH_SHORT).show();
        } else if (req == REQ_CAPTURE) {
            if (res == Activity.RESULT_OK && data != null) {
                Intent svc = new Intent(this, OverlayService.class);
                svc.setAction(OverlayService.ACTION_START);
                svc.putExtra("code", res);
                svc.putExtra("data", data);
                startForegroundService(svc);
                
                Toast.makeText(this, "تم تشغيل ESP! افتح ببجي الآن يدوياً.", Toast.LENGTH_LONG).show();
                finish(); // نقفل الواجهة عشان نلعب
            } else {
                Toast.makeText(this, "تم رفض تصوير الشاشة", Toast.LENGTH_SHORT).show();
            }
        }
    }
}