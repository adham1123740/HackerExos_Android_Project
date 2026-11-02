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

import androidx.annotation.NonNull;
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
    private static final int SHIZUKU_REQ_CODE = 1001;
    
    private static final String PAK_URL = "https://github.com/mohamedn5la1998-svg/vipn5la/raw/refs/heads/main/game_patch_4.6.1.21597.pak";
    private static final String GAME_PKG = "com.tencent.ig";
    private static final String TARGET_PATH = "/storage/emulated/0/Android/data/com.tencent.ig/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/game_patch_4.6.1.21597.pak";

    private TextView tvStatus;
    private boolean isShizukuGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        Button btnStart = findViewById(R.id.btn_start);

        // 1. تهيئة Shizuku
        initShizuku();

        // 2. التحقق من الصلاحيات عند فتح التطبيق
        checkPermissions();

        btnStart.setOnClickListener(v -> {
            if (!isShizukuGranted) {
                Toast.makeText(this, "يجب منح صلاحية Shizuku أولاً!", Toast.LENGTH_LONG).show();
                requestShizukuPermission();
                return;
            }
            startHackProcess();
        });
    }

    // ================= Shizuku Setup =================
    private void initShizuku() {
        // مستمع استلام Binder
        Shizuku.addBinderReceivedListenerSticky(() -> {
            if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                isShizukuGranted = true;
                runOnUiThread(() -> tvStatus.setText("Shizuku جاهز"));
            } else {
                runOnUiThread(this::requestShizukuPermission);
            }
        });

        // مستمع نتيجة طلب الصلاحية
        Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
            if (requestCode == SHIZUKU_REQ_CODE) {
                isShizukuGranted = (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED);
                runOnUiThread(() -> tvStatus.setText(isShizukuGranted ? "تم منح صلاحية Shizuku" : "تم رفض صلاحية Shizuku"));
            }
        });
    }

    private void requestShizukuPermission() {
        if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
            tvStatus.setText("إصدار Shizuku قديم جداً");
            return;
        }
        if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(SHIZUKU_REQ_CODE);
        } else {
            isShizukuGranted = true;
        }
    }

    // ================= Permissions Setup =================
    private void checkPermissions() {
        // أ. صلاحية Overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_OVERLAY);
        } 
        // ب. صلاحية Usage Stats (لمعرفة التطبيق المفتوح)
        else if (!hasUsageStatsPermission()) {
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
                checkPermissions(); // فحص صلاحية Usage Stats بعدها
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

    // ================= Hack Process =================
    private void startHackProcess() {
        tvStatus.setText("جاري تحميل ملف PAK...");
        new Thread(() -> {
            try {
                // 1. تحميل الملف
                File pakFile = new File(getFilesDir(), "game_patch_4.6.1.21597.pak");
                downloadFile(PAK_URL, pakFile);

                runOnUiThread(() -> tvStatus.setText("جاري نسخ الملف إلى اللعبة..."));

                // 2. نسخ الملف إلى مسار اللعبة باستخدام Shizuku
                String cmd = "cp -f \"" + pakFile.getAbsolutePath() + "\" \"" + TARGET_PATH + "\"";
                Process process = Shizuku.newProcess(new String[]{"sh", "-c", cmd}, null, null);
                int exitCode = process.waitFor();

                runOnUiThread(() -> {
                    if (exitCode == 0) {
                        tvStatus.setText("تم النسخ بنجاح! جاري فتح اللعبة...");
                        
                        // 3. فتح اللعبة
                        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(GAME_PKG);
                        if (launchIntent != null) {
                            startActivity(launchIntent);
                        } else {
                            Toast.makeText(this, "ببجي غير مثبتة!", Toast.LENGTH_SHORT).show();
                        }

                        // 4. تشغيل خدمة الواجهة العائمة
                        Intent serviceIntent = new Intent(this, FloatingService.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent);
                        } else {
                            startService(serviceIntent);
                        }
                    } else {
                        tvStatus.setText("فشل النسخ! كود الخطأ: " + exitCode);
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
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            conn.disconnect();
        }
    }
}