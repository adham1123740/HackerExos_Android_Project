package com.wormgpt.wormpanel;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private static final int REQ_OVERLAY = 100;
    private static final int REQ_CAPTURE = 200;
    private static final String MODEL_URL = "https://github.com/ultralytics/assets/releases/download/v8.2.0/yolov8n.tflite";
    private static final String MODEL_NAME = "yolov8n.tflite";

    private MediaProjectionManager mpm;
    private TextView statusText;
    private ProgressBar progress;
    private Button btnDownload, btnStart;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean downloading = false;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        statusText = findViewById(R.id.statusText);
        progress = findViewById(R.id.progress);
        btnDownload = findViewById(R.id.btnDownload);
        btnStart = findViewById(R.id.btnStart);

        btnDownload.setOnClickListener(v -> startDownload());
        btnStart.setOnClickListener(v -> checkOverlay());

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
            statusText.setText("الحالة: YOLO موجود وجاهز");
            btnDownload.setEnabled(false);
            btnDownload.setText("YOLO موجود ✓");
            btnStart.setEnabled(true);
        } else {
            statusText.setText("الحالة: قم بتنزيل YOLO أولاً");
            btnDownload.setEnabled(true);
            btnDownload.setText("تنزيل YOLO");
            btnStart.setEnabled(false);
        }
    }

    private void startDownload() {
        if (downloading || modelReady()) return;
        downloading = true;
        progress.setVisibility(View.VISIBLE);
        progress.setProgress(0);
        btnDownload.setEnabled(false);
        btnDownload.setText("جاري التنزيل...");

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                File target = modelFile();
                File tmp = new File(target.getAbsolutePath() + ".part");
                URL u = new URL(MODEL_URL);
                conn = (HttpURLConnection) u.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.connect();

                long total = conn.getContentLengthLong();
                InputStream in = conn.getInputStream();
                FileOutputStream out = new FileOutputStream(tmp);

                byte[] buf = new byte[64 * 1024];
                long read = 0; int n, lastPct = -1;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    read += n;
                    if (total > 0) {
                        int pct = (int)(read * 100 / total);
                        if (pct != lastPct) {
                            lastPct = pct;
                            ui.post(() -> progress.setProgress(pct));
                        }
                    }
                }
                out.close(); in.close();
                
                if (target.exists()) target.delete();
                tmp.renameTo(target);

                ui.post(() -> {
                    downloading = false;
                    progress.setVisibility(View.GONE);
                    refreshModelState();
                    Toast.makeText(this, "تم التنزيل بنجاح", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                ui.post(() -> {
                    downloading = false;
                    progress.setVisibility(View.GONE);
                    refreshModelState();
                    Toast.makeText(this, "فشل التنزيل", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
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
        } else if (req == REQ_CAPTURE) {
            if (res == Activity.RESULT_OK && data != null) {
                Intent svc = new Intent(this, OverlayService.class);
                svc.setAction(OverlayService.ACTION_START);
                svc.putExtra("code", res);
                svc.putExtra("data", data);
                startForegroundService(svc);
                openPubg();
                finish();
            }
        }
    }

    private void openPubg() {
        String[] pkgs = {"com.tencent.ig", "com.pubg.krmobile", "com.rekoo.pubgm", "com.vng.pubgmobile"};
        for (String pkg : pkgs) {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); return; }
        }
        Toast.makeText(this, "افتح ببجي يدوياً", Toast.LENGTH_SHORT).show();
    }
}