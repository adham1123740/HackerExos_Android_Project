package com.s7g.loader;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
    
    // رابط Raw المباشر (تم إصلاحه)
    private static final String PAK_URL = "https://raw.githubusercontent.com/adham1123740/Hack4.6/main/game_patch_4.6.0.21572.pak";
    private static final String PATCH_FILE_NAME = "game_patch_4.6.0.21572.pak";
    private static final String PREFS_NAME = "ExosPrefs";
    private static final String KEY_DOWNLOADED = "is_downloaded";

    private static final String[] GAME_PACKAGES = {
            "com.tencent.ig", "com.pubg.krmobile", "com.vng.pubgmobile", "com.rekoo.pubgm", "com.pubg.imobile"
    };

    private String gamePackage = null;
    private String paksDir = null;
    private String patchFile = null;
    private String protectFile = null;
    private String aceLib = null;

    private TextView tvStatus, tvShizuku, tvLoading, tvPackageName;
    private LinearLayout loadingOverlay;
    private ProgressBar progressBar;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        tvStatus = findViewById(R.id.tv_status);
        tvShizuku = findViewById(R.id.tv_shizuku_status);
        tvLoading = findViewById(R.id.tv_loading);
        tvPackageName = findViewById(R.id.tv_package_name);
        loadingOverlay = findViewById(R.id.loading_overlay);
        progressBar = findViewById(R.id.progress_bar);

        Button btnStart = findViewById(R.id.btn_start);
        Button btnDelete = findViewById(R.id.btn_delete_hack);
        Button btnLaunch = findViewById(R.id.btn_launch);
        Button btnFloating = findViewById(R.id.btn_floating);

        // البحث عن الباقة
        findGamePackage();

        btnStart.setOnClickListener(v -> startHack());
        btnDelete.setOnClickListener(v -> removeHack());
        btnLaunch.setOnClickListener(v -> launchGame());
        btnFloating.setOnClickListener(v -> startFloatingMenu());

        try {
            Shizuku.addRequestPermissionResultListener((code, res) -> runOnUiThread(this::updateShizukuStatus));
        } catch (Exception ignored) {}

        checkPermissions();
        updateShizukuStatus();
    }

    private void findGamePackage() {
        PackageManager pm = getPackageManager();
        for (String pkg : GAME_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
                gamePackage = pkg;
                break;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }

        if (gamePackage != null) {
            paksDir = "/storage/emulated/0/Android/data/" + gamePackage + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/";
            patchFile = paksDir + PATCH_FILE_NAME;
            protectFile = paksDir + "pakchunk0-Android_NoES2.pak";
            aceLib = "/data/data/" + gamePackage + "/lib/libanogs.so";
            tvPackageName.setText(gamePackage);
        } else {
            tvPackageName.setText("غير مثبتة");
        }
    }

    private void updateShizukuStatus() {
        if (isShizukuReady()) {
            tvShizuku.setText("Shizuku: شغال ✔");
            tvShizuku.setTextColor(Color.parseColor("#00E5FF"));
        } else {
            tvShizuku.setText("Shizuku: غير شغال ✘");
            tvShizuku.setTextColor(Color.parseColor("#FF5252"));
        }
    }

    private boolean isShizukuReady() {
        try {
            return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) { return false; }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())), REQ_OVERLAY);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_OVERLAY && Settings.canDrawOverlays(this)) updateShizukuStatus();
    }

    private void startHack() {
        if (!isShizukuReady()) {
            Toast.makeText(this, "يجب تفعيل Shizuku أولاً!", Toast.LENGTH_LONG).show();
            return;
        }
        if (gamePackage == null) {
            Toast.makeText(this, "ببجي غير مثبتة!", Toast.LENGTH_LONG).show();
            return;
        }

        if (prefs.getBoolean(KEY_DOWNLOADED, false)) {
            launchGame();
            return;
        }

        loadingOverlay.setVisibility(View.VISIBLE);
        ObjectAnimator.ofFloat(loadingOverlay, "alpha", 0f, 1f).setDuration(800).start();

        new Thread(() -> {
            try {
                updateLoadingText("جاري تحميل ملف الهكر...");
                File localFile = new File(getFilesDir(), PATCH_FILE_NAME);
                boolean downloaded = downloadFile(PAK_URL, localFile);

                if (!downloaded) {
                    throw new Exception("فشل التحميل، تأكد من اتصال الإنترنت أو صحة الرابط.");
                }

                updateLoadingText("جاري تثبيت ملف الهكر...");
                boolean ok = ShizukuManager.exec("cp -f \"" + localFile.getAbsolutePath() + "\" \"" + patchFile + "\"") == 0;

                if (!ok) {
                    throw new Exception("فشل نسخ الملف إلى ببجي. تأكد من تفعيل Shizuku.");
                }

                updateLoadingText("جاري إغلاق الحماية...");
                ShizukuManager.exec("rm -f \"" + protectFile + "\"");
                ShizukuManager.exec("rm -f \"" + aceLib + "\"");

                prefs.edit().putBoolean(KEY_DOWNLOADED, true).apply();

                new Handler(Looper.getMainLooper()).post(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    tvStatus.setText("تم التثبيت بنجاح!");
                    launchGame();
                });

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    tvStatus.setText("خطأ: " + e.getMessage());
                    Toast.makeText(this, "فشل التحميل", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void removeHack() {
        if (!isShizukuReady()) {
            Toast.makeText(this, "يجب تفعيل Shizuku أولاً!", Toast.LENGTH_LONG).show();
            return;
        }
        if (gamePackage == null) {
            Toast.makeText(this, "ببجي غير مثبتة!", Toast.LENGTH_LONG).show();
            return;
        }

        new Thread(() -> {
            try {
                ShizukuManager.exec("rm -f \"" + patchFile + "\"");
                File f = new File(getFilesDir(), PATCH_FILE_NAME);
                if (f.exists()) f.delete();
                prefs.edit().putBoolean(KEY_DOWNLOADED, false).apply();

                new Handler(Looper.getMainLooper()).post(() -> {
                    tvStatus.setText("تم حذف الهاك بنجاح");
                    Toast.makeText(this, "تم الحذف", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void launchGame() {
        if (gamePackage == null) {
            Toast.makeText(this, "ببجي غير مثبتة!", Toast.LENGTH_SHORT).show();
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            tvStatus.setText("جاري الدخول إلى اللعبة...");
            Intent i = getPackageManager().getLaunchIntentForPackage(gamePackage);
            if (i != null) startActivity(i);
            else Toast.makeText(this, "ببجي غير مثبتة", Toast.LENGTH_SHORT).show();
        });
    }

    private void startFloatingMenu() {
        if (Settings.canDrawOverlays(this)) {
            startService(new Intent(this, FloatingService.class));
            Toast.makeText(this, "تم تشغيل القائمة العائمة", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "الرجاء إعطاء صلاحية النافذة العائمة", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLoadingText(String text) {
        new Handler(Looper.getMainLooper()).post(() -> tvLoading.setText(text));
    }

    // دالة التحميل المحسنة: بتتأكد من محتوى الملف (مش صفحة HTML)
    private boolean downloadFile(String urlStr, File out) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setInstanceFollowRedirects(true);
            c.setConnectTimeout(20000);
            c.setReadTimeout(20000);
            c.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = c.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return false;
            }

            long fileLength = c.getContentLength();
            if (fileLength > 0) {
                // تحديث الـ progress bar بالحجم الكلي
                progressBar.setMax((int) fileLength);
            }

            try (InputStream in = c.getInputStream(); FileOutputStream fo = new FileOutputStream(out)) {
                byte[] buf = new byte[8192];
                int n;
                long total = 0;
                while ((n = in.read(buf)) > 0) {
                    fo.write(buf, 0, n);
                    total += n;
                    progressBar.setProgress((int) total);
                }
            } finally {
                c.disconnect();
            }

            // التأكد من إن الملف مش فاضي ومش صفحة HTML
            if (out.length() < 1000) {
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}