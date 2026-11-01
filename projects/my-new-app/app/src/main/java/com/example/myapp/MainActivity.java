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

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity {

    private static final int REQ_OVERLAY = 100;
    private static final int REQ_CAPTURE = 200;
    private static final String MODEL_NAME = "yolov8n.tflite";

    private MediaProjectionManager mpm;
    private TextView statusText;
    private ProgressBar progress;
    private Button btnDownload, btnStart;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        statusText = findViewById(R.id.statusText);
        progress = findViewById(R.id.progress);
        btnDownload = findViewById(R.id.btnDownload);
        btnStart = findViewById(R.id.btnStart);

        btnDownload.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);

        checkModelFile();
        btnStart.setOnClickListener(v -> checkOverlay());
    }

    private void checkModelFile() {
        try {
            // بنفحص الملف اللي جوه الـ assets
            InputStream is = getAssets().open(MODEL_NAME);
            int size = is.available();
            is.close();

            if (size > 1_000_000) {
                statusText.setText("الحالة: الملف موجود جوه التطبيق، جاهز للتشغيل");
                btnStart.setEnabled(true);
                btnStart.setText("تفعيل ESP وفتح ببجي");
            } else {
                statusText.setText("الحالة: الملف صغير جداً أو تالف");
                btnStart.setEnabled(false);
                btnStart.setText("الملف تالف");
            }
        } catch (IOException e) {
            statusText.setText("الحالة: الملف غير موجود في مجلد assets");
            btnStart.setEnabled(false);
            btnStart.setText("الملف مفقود");
        }
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
                openPubg();
                finish();
            } else {
                Toast.makeText(this, "تم رفض تصوير الشاشة", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openPubg() {
        String[] pkgs = {"com.tencent.ig", "com.pubg.krmobile", "com.rekoo.pubgm", "com.vng.pubgmobile"};
        for (String pkg : pkgs) {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) { 
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
                startActivity(i); 
                return; 
            }
        }
        Toast.makeText(this, "لم يتم العثور على ببجي، افتحها يدوياً", Toast.LENGTH_SHORT).show();
    }
}