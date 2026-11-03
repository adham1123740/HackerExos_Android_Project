package com.s7g.loader;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_OVERLAY = 100;
    private static final int REQ_USAGE = 101;
    
    private static final String PAK_URL = "https://github.com/mohamedn5la1998-svg/vipn5la/raw/refs/heads/main/game_patch_4.6.1.21597.pak";
    private static final String GAME_PKG = "com.tencent.ig";
    private static final String TARGET_PATH = "/storage/emulated/0/Android/data/com.tencent.ig/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/game_patch_4.6.1.21597.pak";

    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        Button btnStart = findViewById(R.id.btn_start);

        initShizuku();
        checkPermissions();

        btnStart.setOnClickListener(v -> {
            if (!ShizukuManager.isGranted()) {
                Toast.makeText(this, "يجب منح صلاحية Shizuku أولاً!", Toast.LENGTH_LONG).show();
                ShizukuManager.requestPermission();
                return;
            }
            startHackProcess();
        });
    }

    private void initShizuku() {
        Shizuku.addBinderReceivedListenerSticky(() -> {
            if (ShizukuManager.isGranted()) {
                runOnUiThread(() -> tvStatus.setText("Shizuku جاهز"));
            } else {
                runOnUiThread(ShizukuManager::requestPermission);
            }
        });

        Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
            if (requestCode == ShizukuManager.REQUEST_CODE) {
                runOnUiThread(() -> tvStatus.setText(
                    ShizukuManager.isGranted() ? "تم منح صلاحية Shizuku" : "تم رفض صلاحية Shizuku"));
            }
        });
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_OVERLAY);
        } else if (!hasUsageStatsPermission()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(intent, REQ_USAGE);
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                checkPermissions();
            } else {
                tvStatus.setText("يجب منح صلاحية العرض فوق التطبيقات!");
            }
        } else if (requestCode == REQ_USAGE) {
            if (hasUsageStatsPermission()) {
                tvStatus.setText("جميع الصلاحيات جاهزة");
            } else {
                tvStatus.setText("يجب منح صلاحية Usage Access!");
            }
        }
    }

    private void startHackProcess() {
        tvStatus.setText("جاري تحميل ملف PAK...");
        new Thread(() -> {
            try {
                File pakFile = new File(getFilesDir(), "game_patch.pak");
                downloadFile(PAK_URL, pakFile);

                runOnUiThread(() -> tvStatus.setText("جاري نسخ الملف إلى اللعبة..."));

                boolean success = ShizukuManager.copyFile(pakFile.getAbsolutePath(), TARGET_PATH);

                runOnUiThread(() -> {
                    if (success) {
                        tvStatus.setText("تم النسخ بنجاح! جاري فتح اللعبة...");
                        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(GAME_PKG);
                        if (launchIntent != null) startActivity(launchIntent);
                        startForegroundService(new Intent(this, FloatingService.class));
                    } else {
                        tvStatus.setText("فشل النسخ! تأكد من منح صلاحية Shizuku.");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> tvStatus.setText("حدث خطأ: " + e.getMessage()));
            }
        }).start();
    }

    private void downloadFile(String urlStr, File outFile) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.connect();

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(outFile)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        } finally {
            conn.disconnect();
        }
    }
}