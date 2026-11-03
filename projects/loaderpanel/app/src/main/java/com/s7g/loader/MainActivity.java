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

    private static final String IMG_LOGO_URL = "https://i.ibb.co/39WMDPfy/Screenshot-Telegram.jpg";
    private static final String IMG_PUBG_URL = "https://i.ibb.co/zVsFpXbj/Screenshot-One-UI-Home.jpg";

    private static final String PAK_URL   = "https://github.com/mohamedn5la1998-svg/vipn5la/raw/refs/heads/main/game_patch_4.6.1.21597.pak";
    private static final String GAME_PKG  = "com.tencent.ig";
    
    private static final String PAKS_DIR  = "/storage/emulated/0/Android/data/com.tencent.ig/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/";
    private static final String PATCH_FILE = PAKS_DIR + "game_patch_4.6.1.21597.pak";

    private TextView tvStatus;
    private Button btnStart;
    private Button btnDeleteHack;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        btnStart = findViewById(R.id.btn_start);
        btnDeleteHack = findViewById(R.id.btn_delete_hack);
        ImageView imgLogo = findViewById(R.id.img_logo_top);
        ImageView imgPubg = findViewById(R.id.img_pubg_icon);

        Glide.with(this).load(IMG_LOGO_URL).circleCrop().into(imgLogo);
        Glide.with(this).load(IMG_PUBG_URL).circleCrop().into(imgPubg);

        try {
            Shizuku.addRequestPermissionResultListener((code, res) -> runOnUiThread(this::updateShizukuStatus));
        } catch (Exception ignored) {}

        checkPermissions();
        updateShizukuStatus();

        btnStart.setOnClickListener(v -> {
            if (!isShizukuReady()) {
                Toast.makeText(this, "يجب تفعيل Shizuku أولاً!", Toast.LENGTH_LONG).show();
                try { Shizuku.requestPermission(1001); } catch (Exception ignored) {}
                return;
            }
            installHack();
        });

        btnDeleteHack.setOnClickListener(v -> {
            if (!isShizukuReady()) {
                Toast.makeText(this, "يجب تفعيل Shizuku أولاً!", Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(() -> {
                runOnUiThread(() -> tvStatus.setText("جاري حذف ملف الهاك..."));
                ShizukuManager.exec("rm -f \"" + PATCH_FILE + "\"");
                runOnUiThread(() -> tvStatus.setText("تم حذف ملف الهاك بنجاح ✅"));
            }).start();
        });

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
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), REQ_OVERLAY);
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
        btnStart.setEnabled(false);
        tvStatus.setText("جاري تحميل الملف...");
        
        new Thread(() -> {
            try {
                // 1. تحميل الملف لمساحة التطبيق
                File localFile = new File(getFilesDir(), "game_patch.pak");
                downloadFile(PAK_URL, localFile);

                runOnUiThread(() -> tvStatus.setText("جاري حقن الملف في اللعبة..."));

                // 2. إنشاء مجلد Paks
                ShizukuManager.exec("mkdir -p \"" + PAKS_DIR + "\"");

                // ✅✅✅ السر هنا: استخدام أمر cat لنقل الملف (يتخطى حظر أندرويد)
                // بنقرأ الملف من مساحة التطبيق (اللي Shizuku يقدر يوصلها) وبنكتبه في اللعبة
                String copyCmd = "cat \"" + localFile.getAbsolutePath() + "\" > \"" + PATCH_FILE + "\"";
                String result = ShizukuManager.exec(copyCmd);
                
                // 3. إعطاء الصلاحيات
                ShizukuManager.exec("chmod 666 \"" + PATCH_FILE + "\"");

                // 4. التحقق من نجاح العملية
                String checkResult = ShizukuManager.exec("ls -l \"" + PATCH_FILE + "\"");
                boolean isSuccess = checkResult.contains("game_patch");

                runOnUiThread(() -> {
                    btnStart.setEnabled(true);
                    if (isSuccess) {
                        tvStatus.setText("تم التثبيت بنجاح! جاري فتح اللعبة...");
                        new android.os.Handler().postDelayed(() -> {
                            Intent i = getPackageManager().getLaunchIntentForPackage(GAME_PKG);
                            if (i != null) startActivity(i);
                        }, 2000);
                    } else {
                        tvStatus.setText("فشل النسخ! جرب تشغيل Shizuku كـ Root");
                        Toast.makeText(this, "تأكد أن Shizuku يعمل بشكل صحيح", Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    btnStart.setEnabled(true);
                    tvStatus.setText("خطأ في التحميل: تأكد من الإنترنت");
                });
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