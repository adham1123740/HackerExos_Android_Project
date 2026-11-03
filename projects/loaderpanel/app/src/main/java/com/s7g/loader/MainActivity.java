package com.s7g.loader;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_OVERLAY = 100;
    private static final int REQ_USAGE = 101;

    // روابط الصور الخاصة بك
    private static final String IMG_LOGO_URL = "https://i.ibb.co/39WMDPfy/Screenshot-Telegram.jpg";
    private static final String IMG_PUBG_URL = "https://i.ibb.co/zVsFpXbj/Screenshot-One-UI-Home.jpg";

    private static final String PAK_URL   = "https://github.com/mohamedn5la1998-svg/vipn5la/raw/refs/heads/main/game_patch_4.6.1.21597.pak";
    private static final String GAME_PKG  = "com.tencent.ig";
    
    // تم تعديل المسار ليكون المسار القياسي المضمون
    private static final String PAKS_DIR  = "/storage/emulated/0/Android/data/com.tencent.ig/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/";
    private static final String PATCH_FILE = PAKS_DIR + "game_patch_4.6.1.21597.pak";

    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        Button btnStart      = findViewById(R.id.btn_start);
        Button btnDeleteHack = findViewById(R.id.btn_delete_hack);
        ImageView imgLogo    = findViewById(R.id.img_logo_top);
        ImageView imgPubg    = findViewById(R.id.img_pubg_icon);

        // تحميل الصور من الروابط
        Glide.with(this).load(IMG_LOGO_URL).circleCrop().into(imgLogo);
        Glide.with(this).load(IMG_PUBG_URL).circleCrop().into(imgPubg);

        // تفعيل Shizuku
        try {
            Shizuku.addRequestPermissionResultListener((code, res) -> {
                runOnUiThread(this::updateShizukuStatus);
            });
        } catch (Exception ignored) {}

        checkPermissions();
        updateShizukuStatus();

        // زر تفعيل الهاك
        btnStart.setOnClickListener(v -> {
            if (!isShizukuReady()) {
                Toast.makeText(this, "يجب تفعيل Shizuku أولاً!", Toast.LENGTH_LONG).show();
                try { Shizuku.requestPermission(1001); } catch (Exception ignored) {}
                return;
            }
            installHack();
        });

        // زر حذف الهاك
        btnDeleteHack.setOnClickListener(v -> {
            if (!isShizukuReady()) {
                Toast.makeText(this, "يجب تفعيل Shizuku أولاً!", Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(() -> {
                runOnUiThread(() -> tvStatus.setText("جاري حذف ملف الهاك..."));
                try {
                    // استخدام أمر rm لحذف الملف
                    ShizukuManager.exec("rm -f \"" + PATCH_FILE + "\"");
                    runOnUiThread(() -> tvStatus.setText("تم حذف ملف الهاك بنجاح"));
                } catch (Exception e) {
                    runOnUiThread(() -> tvStatus.setText("فشل الحذف"));
                }
            }).start();
        });

        // الضغط على صورة الببجي يفتح اللعبة
        imgPubg.setOnClickListener(v -> {
            Intent i = getPackageManager().getLaunchIntentForPackage(GAME_PKG);
            if (i != null) startActivity(i);
            else Toast.makeText(this, "ببجي غير مثبتة", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateShizukuStatus() {
        if (isShizukuReady()) {
            tvStatus.setText("Shizuku: شغال 🟢");
            tvStatus.setTextColor(Color.parseColor("#00FF00"));
        } else {
            tvStatus.setText("Shizuku: غير شغال 🔴");
            tvStatus.setTextColor(Color.parseColor("#FF4081"));
        }
    }

    private boolean isShizukuReady() {
        try {
            return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            return false;
        }
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
    protected void onResume() {
        super.onResume();
        updateShizukuStatus();
    }

    private void installHack() {
        tvStatus.setText("جاري تحميل ملف الهاك...");
        new Thread(() -> {
            try {
                File f = new File(getFilesDir(), "game_patch.pak");
                downloadFile(PAK_URL, f);

                runOnUiThread(() -> tvStatus.setText("جاري تثبيت الملف في اللعبة..."));

                // ✅✅✅ هنا الحل الجذري لمشكلة فشل النسخ ✅✅✅
                // 1. إنشاء المجلد لو مش موجود
                ShizukuManager.exec("mkdir -p \"" + PAKS_DIR + "\"");
                
                // 2. استخدام أمر cat لنقل الملف (مضمون 100% مع Shizuku)
                int result = ShizukuManager.exec("cat \"" + f.getAbsolutePath() + "\" > \"" + PATCH_FILE + "\"");
                
                // 3. إعطاء صلاحيات للملف عشان اللعبة تقراه
                ShizukuManager.exec("chmod 644 \"" + PATCH_FILE + "\"");

                boolean ok = (result == 0);

                runOnUiThread(() -> {
                    if (ok) {
                        tvStatus.setText("تم التثبيت بنجاح! جاري فتح اللعبة...");
                        // تأخير بسيط قبل فتح اللعبة
                        new android.os.Handler().postDelayed(() -> {
                            Intent i = getPackageManager().getLaunchIntentForPackage(GAME_PKG);
                            if (i != null) startActivity(i);
                        }, 1500);
                    } else {
                        tvStatus.setText("فشل النسخ! جرب تعيد تشغيل Shizuku أو تأكد من صلاحيات التخزين");
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