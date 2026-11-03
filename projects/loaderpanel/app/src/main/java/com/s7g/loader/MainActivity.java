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

    private static final String PAK_URL   = "https://github.com/mohamedn5la1998-svg/vipn5la/raw/refs/heads/main/game_patch_4.6.1.21597.pak";
    private static final String GAME_PKG  = "com.tencent.ig";
    private static final String PAKS_DIR  = "/storage/emulated/0/Android/data/com.tencent.ig/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/";
    private static final String PATCH_FILE = PAKS_DIR + "game_patch_4.6.1.21597.pak";
    private static final String PROTECT_FILE = PAKS_DIR + "pakchunk0-Android_NoES2.pak";
    private static final String ACE_LIB = "/data/data/com.tencent.ig/lib/libanogs.so";

    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        // ربط ملف الواجهة
        setContentView(R.layout.activity_main);

        // ربط العناصر
        tvStatus = findViewById(R.id.tv_status);
        Button btnStart      = findViewById(R.id.btn_start);
        Button btnDeleteProt = findViewById(R.id.btn_delete_protection);
        Button btnDeleteHack = findViewById(R.id.btn_delete_hack);
        Button btnLaunch     = findViewById(R.id.btn_launch);

        // محاولة تهيئة Shizuku بأمان (بدون انهيار إذا لم تكن مفعلة)
        initShizuku();
        checkPermissions();

        // زر تشغيل الهكر
        btnStart.setOnClickListener(v -> {
            if (!isShizukuReady()) {
                Toast.makeText(this, "يجب تفعيل Shizuku أولاً ومنح الصلاحية!", Toast.LENGTH_LONG).show();
                try { Shizuku.requestPermission(1001); } catch (Exception ignored) {}
                tvStatus.setText("في انتظار صلاحية Shizuku...");
                return;
            }
            installHack();
        });

        // زر حذف الحماية
        btnDeleteProt.setOnClickListener(v -> {
            if (!isShizukuReady()) {
                Toast.makeText(this, "يجب تفعيل Shizuku أولاً!", Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(() -> {
                runOnUiThread(() -> tvStatus.setText("جاري حذف ملفات الحماية..."));
                try {
                    ShizukuManager.exec("rm -f \"" + PROTECT_FILE + "\"");
                    ShizukuManager.exec("rm -f \"" + ACE_LIB + "\"");
                    runOnUiThread(() -> tvStatus.setText("تم حذف ملفات الحماية"));
                } catch (Exception e) {
                    runOnUiThread(() -> tvStatus.setText("فشل الحذف"));
                }
            }).start();
        });

        // زر حذف الهكر
        btnDeleteHack.setOnClickListener(v -> {
            if (!isShizukuReady()) {
                Toast.makeText(this, "يجب تفعيل Shizuku أولاً!", Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(() -> {
                runOnUiThread(() -> tvStatus.setText("جاري حذف ملف الهكر..."));
                try {
                    ShizukuManager.exec("rm -f \"" + PATCH_FILE + "\"");
                    runOnUiThread(() -> tvStatus.setText("تم حذف ملف الهكر"));
                } catch (Exception e) {
                    runOnUiThread(() -> tvStatus.setText("فشل الحذف"));
                }
            }).start();
        });

        // زر فتح اللعبة
        btnLaunch.setOnClickListener(v -> {
            Intent i = getPackageManager().getLaunchIntentForPackage(GAME_PKG);
            if (i != null) startActivity(i);
            else Toast.makeText(this, "ببجي غير مثبتة", Toast.LENGTH_SHORT).show();
        });
    }

    // دالة آمنة للتحقق من Shizuku لمنع الانهيار
    private boolean isShizukuReady() {
        try {
            return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            return false;
        }
    }

    private void initShizuku() {
        try {
            Shizuku.addBinderReceivedListenerSticky(() -> {
                runOnUiThread(() -> tvStatus.setText("Shizuku متصل"));
            });
            Shizuku.addRequestPermissionResultListener((code, res) -> {
                runOnUiThread(() -> tvStatus.setText(res == 0 ? "تم منح صلاحية Shizuku" : "تم رفض الصلاحية"));
            });
        } catch (Exception ignored) {}
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())), REQ_OVERLAY);
        } else if (!hasUsageStatsPermission()) {
            startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), REQ_USAGE);
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager ao = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = ao.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_OVERLAY && Settings.canDrawOverlays(this)) checkPermissions();
        else if (req == REQ_USAGE && hasUsageStatsPermission()) tvStatus.setText("جميع الصلاحيات جاهزة");
    }

    private void installHack() {
        tvStatus.setText("جاري تحميل ملف PAK...");
        new Thread(() -> {
            try {
                File f = new File(getFilesDir(), "game_patch.pak");
                downloadFile(PAK_URL, f);

                runOnUiThread(() -> tvStatus.setText("جاري نسخ الملف إلى اللعبة..."));

                boolean ok = ShizukuManager.exec("cp -f \"" + f.getAbsolutePath() + "\" \"" + PATCH_FILE + "\"") == 0;

                runOnUiThread(() -> {
                    if (ok) {
                        tvStatus.setText("تم التثبيت! افتح اللعبة");
                        Intent i = getPackageManager().getLaunchIntentForPackage(GAME_PKG);
                        if (i != null) startActivity(i);
                    } else {
                        tvStatus.setText("فشل النسخ! تحقق من Shizuku");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> tvStatus.setText("خطأ في التحميل: " + e.getMessage()));
            }
        }).start();
    }

    private void downloadFile(String urlStr, File out) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(15000);
        c.connect();
        try (InputStream in = c.getInputStream(); FileOutputStream fo = new FileOutputStream(out)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) fo.write(buf, 0, n);
        } finally {
            c.disconnect();
        }
    }
}