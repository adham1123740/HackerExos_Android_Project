package com.mahi.overlayesp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_OVERLAY = 100;
    private static final int REQ_CAPTURE = 200;
    private static final String MODEL_URL =
            "https://github.com/ultralytics/assets/releases/download/v8.2.0/yolov8n.tflite";
    private static final String MODEL_NAME = "yolov8n.tflite";

    private MediaProjectionManager mpm;
    private TextView statusText;
    private ProgressBar progress;
    private Button btnDownload, btnStart;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean downloading = false;
    private volatile boolean cancelDownload = false;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        statusText = findViewById(R.id.statusText);
        progress   = findViewById(R.id.progress);
        btnDownload = findViewById(R.id.btnDownload);
        btnStart    = findViewById(R.id.btnStart);

        btnDownload.setOnClickListener(v -> startDownload());
        btnStart.setOnClickListener(v -> launchCapture());

        refreshModelState();
    }

    private File modelFile() {
        File dir = new File(getFilesDir(), "models");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, MODEL_NAME);
    }

    private boolean modelReady() {
        File f = modelFile();
        return f.exists() && f.length() > 1_000_000;
    }

    private void refreshModelState() {
        if (modelReady()) {
            statusText.setText("الحالة: YOLO موجود — يمكنك استخدامه");
            btnDownload.setEnabled(false);
            btnDownload.setText("YOLO موجود ✓");
            btnStart.setEnabled(true);
        } else {
            statusText.setText("الحالة: النموذج غير موجود — اضغط تنزيل");
            btnDownload.setEnabled(true);
            btnDownload.setText("تنزيل YOLO");
            btnStart.setEnabled(false);
        }
    }

    private void startDownload() {
        if (downloading) return;
        if (modelReady()) {
            Toast.makeText(this, "YOLO موجود مسبقاً، يمكنك استخدامه", Toast.LENGTH_SHORT).show();
            refreshModelState();
            return;
        }
        downloading = true;
        cancelDownload = false;
        progress.setVisibility(View.VISIBLE);
        progress.setProgress(0);
        btnDownload.setEnabled(false);
        btnDownload.setText("جاري التنزيل…");

        new Thread(this::downloadWorker, "model-dl").start();
    }

    private void downloadWorker() {
        HttpURLConnection conn = null;
        try {
            File target = modelFile();
            File tmp = new File(target.getAbsolutePath() + ".part");
            if (tmp.exists()) tmp.delete();

            URL u = new URL(MODEL_URL);
            conn = (HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(60_000);
            conn.setInstanceFollowRedirects(true);
            conn.connect();

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) throw new IOException("HTTP " + code);

            long total = conn.getContentLengthLong();
            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(tmp);

            byte[] buf = new byte[64 * 1024];
            long read = 0;
            int n, lastPct = -1;
            while ((n = in.read(buf)) != -1) {
                if (cancelDownload) throw new IOException("cancelled");
                out.write(buf, 0, n);
                read += n;
                if (total > 0) {
                    int pct = (int)(read * 100 / total);
                    if (pct != lastPct) {
                        lastPct = pct;
                        final int p = pct;
                        ui.post(() -> progress.setProgress(p));
                    }
                }
            }
            out.flush();
            out.getFD().sync();
            out.close();
            in.close();

            if (target.exists()) target.delete();
            if (!tmp.renameTo(target)) throw new IOException("rename failed");
            if (target.length() < 1_000_000) throw new IOException("file too small");

            ui.post(() -> {
                downloading = false;
                progress.setVisibility(View.GONE);
                refreshModelState();
                Toast.makeText(this, "تم التنزيل وحفظه بنجاح", Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) {
            ui.post(() -> {
                downloading = false;
                progress.setVisibility(View.GONE);
                refreshModelState();
                new AlertDialog.Builder(this)
                        .setTitle("فشل التنزيل")
                        .setMessage(e.getMessage())
                        .setPositiveButton("إعادة المحاولة", (d, w) -> startDownload())
                        .setNegativeButton("إلغاء", null)
                        .show();
            });
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void launchCapture() {
        if (!modelReady()) {
            Toast.makeText(this, "حمّل النموذج أولاً", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())), REQ_OVERLAY);
        } else {
            startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE);
            } else {
                Toast.makeText(this, "صلاحية Overlay مرفوضة", Toast.LENGTH_LONG).show();
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

    @Override
    protected void onDestroy() {
        cancelDownload = true;
        super.onDestroy();
    }
}