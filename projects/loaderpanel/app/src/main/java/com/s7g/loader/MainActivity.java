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
    
    // رابط مباشر
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

        findGamePackage();

        btnStart.setOnClickListener(v -> startHack());
        btnDelete.setOnClickListener(v -> removeHack());
        btnLaunch.setOnClickListener(v -> launchGame());

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
                // 1. تحميل الملف
                updateLoadingText("جاري تحميل ملف الهكر...");
                File localFile = new File(getFilesDir(), PATCH_FILE_NAME);
                boolean downloaded = downloadFile(PAK_URL, localFile);

                if (!downloaded || localFile.length() < 1000) {
                    throw new Exception("فشل التحميل. تأكد من الإنترنت أو صحة الرابط.");
                }

                // 2. نسخ الملف
                updateLoadingText("جاري تثبيت ملف الهكر...");
                boolean copied = copyToGame(localFile.getAbsolutePath(), patchFile);

                if (!copied) {
                    throw new Exception("فشل نسخ الملف. تأكد من منح Shizuku الصلاحيات.");
                }

                // 3. حذف الحماية
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
                    Toast.makeText(this, "فشل التثبيت", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // النسخ باستخدام cat + dd + cp
    private boolean copyToGame(String source, String dest) {
        // 1. cat
        String cmd1 = "cat \"" + source + "\" > \"" + dest + "\"";
        ShizukuManager.exec(cmd1);
        if (checkFileExists(dest)) return true;

        // 2. dd
        String cmd2 = "dd if=\"" + source + "\" of=\"" + dest + "\"";
        ShizukuManager.exec(cmd2);
        if (checkFileExists(dest)) return true;

        // 3. cp
        String cmd3 = "cp -f \"" + source + "\" \"" + dest + "\"";
        ShizukuManager.exec(cmd3);
        if (checkFileExists(dest)) return true;

        return false;
    }

    private boolean checkFileExists(String path) {
        String cmd = "ls \"" + path + "\"";
        String out = ShizukuManager.execWithOutput(cmd);
        return out.contains("Exit: 0");
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

    private void updateLoadingText(String text) {
        new Handler(Looper.getMainLooper()).post(() -> tvLoading.setText(text));
    }

    // دالة تحميل محسنة: بتدير الـ Redirect يدوي وتتأكد من حجم الملف
    private boolean downloadFile(String urlStr, File out) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setInstanceFollowRedirects(false); // ندير الـ Redirect يدوي
            c.setConnectTimeout(20000);
            c.setReadTimeout(20000);
            c.setRequestProperty("User-Agent", "Mozilla/5.0");

            int code = c.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM) {
                String newUrl = c.getHeaderField("Location");
                c = (HttpURLConnection) new URL(newUrl).openConnection();
                c.setRequestMethod("GET");
                c.setRequestProperty("User-Agent", "Mozilla/5.0");
            }

            try (InputStream in = c.getInputStream(); FileOutputStream fo = new FileOutputStream(out)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) fo.write(buf, 0, n);
            } finally {
                c.disconnect();
            }

            // التحقق من حجم الملف
            return out.length() > 1000;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}