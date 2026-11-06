package com.s7g.loader;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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

    // رابط التحميل المباشر (Raw)
    private static final String PAK_URL = "https://raw.githubusercontent.com/adham1123740/Hack4.6/main/game_patch_4.6.0.21572.pak";
    
    // مسارات ببجي المحتملة
    private static final String[] GAME_PACKAGES = {
            "com.tencent.ig",       // Global
            "com.pubg.krmobile",    // KR
            "com.vng.pubgmobile",   // VNG
            "com.rekoo.pubgm",      // TW
            "com.pubg.imobile"      // Global (Alternative)
    };

    private static final String PATCH_FILE_NAME = "game_patch_4.6.0.21572.pak";
    private static final String PREFS_NAME = "ExosPrefs";
    private static final String KEY_DOWNLOADED = "is_downloaded";

    // بقية المسارات (هنبحث عنها ديناميكياً بناءً على الباقة)
    private String gamePackage = null;
    private String paksDir = null;
    private String patchFile = null;
    private String protectFile = null;
    private String aceLib = null;

    private TextView tvStatus;
    private TextView tvShizuku;
    private LinearLayout loadingOverlay;
    private ProgressBar progressBar;
    private TextView tvLoadingText;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // بناء الواجهة
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#0A0810"));
        scroll.setFillViewport(true);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(60, 80, 60, 60);
        main.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(main);

        // 1. الهيدر
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(40, 40, 40, 40);
        GradientDrawable gdHeader = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.parseColor("#B300B3"), Color.parseColor("#330033")});
        gdHeader.setCornerRadius(60);
        gdHeader.setStroke(5, Color.parseColor("#D500F9"));
        header.setBackground(gdHeader);

        LinearLayout avatarCircle = new LinearLayout(this);
        avatarCircle.setGravity(Gravity.CENTER);
        GradientDrawable gdAvatar = new GradientDrawable();
        gdAvatar.setShape(GradientDrawable.OVAL);
        gdAvatar.setColor(Color.parseColor("#1A0A1A"));
        gdAvatar.setStroke(3, Color.parseColor("#00E5FF"));
        avatarCircle.setBackground(gdAvatar);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(150, 150);
        avatarCircle.setLayoutParams(avatarParams);
        TextView avatarText = new TextView(this);
        avatarText.setText("EX");
        avatarText.setTextColor(Color.parseColor("#00E5FF"));
        avatarText.setTextSize(40);
        avatarText.setGravity(Gravity.CENTER);
        avatarCircle.addView(avatarText);
        header.addView(avatarCircle);

        TextView title = new TextView(this);
        title.setText("PANEL EXOS\nDEVELOPER");
        title.setTextColor(Color.parseColor("#00E5FF"));
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 20, 0, 0);
        header.addView(title);

        TextView subTitle = new TextView(this);
        subTitle.setText("@HackerExos");
        subTitle.setTextColor(Color.parseColor("#B388FF"));
        subTitle.setTextSize(14);
        header.addView(subTitle);

        main.addView(header);

        // 2. حالة Shizuku
        tvShizuku = new TextView(this);
        tvShizuku.setText("Shizuku: جاري الفحص...");
        tvShizuku.setTextColor(Color.parseColor("#FF5252"));
        tvShizuku.setGravity(Gravity.CENTER);
        tvShizuku.setTextSize(16);
        tvShizuku.setPadding(0, 40, 0, 40);
        main.addView(tvShizuku);

        // 3. كارت اللعبة
        LinearLayout gameCard = new LinearLayout(this);
        gameCard.setOrientation(LinearLayout.VERTICAL);
        gameCard.setPadding(40, 40, 40, 40);
        GradientDrawable gdCard = new GradientDrawable();
        gdCard.setColor(Color.parseColor("#0F0B1A"));
        gdCard.setCornerRadius(40);
        gdCard.setStroke(3, Color.parseColor("#D500F9"));
        gameCard.setBackground(gdCard);

        TextView gameName = new TextView(this);
        gameName.setText("PUBG Mobile | Global");
        gameName.setTextColor(Color.WHITE);
        gameName.setTextSize(18);
        gameName.setGravity(Gravity.CENTER);
        gameCard.addView(gameName);

        tvStatus = new TextView(this);
        tvStatus.setText("جاهز للبدء");
        tvStatus.setTextColor(Color.GRAY);
        tvStatus.setTextSize(14);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setPadding(0, 20, 0, 20);
        gameCard.addView(tvStatus);
        main.addView(gameCard);

        // 4. أزرار التحكم
        Button btnStart = createButton("تشغيل الهاك | ENABLE", "#D500F9");
        btnStart.setOnClickListener(v -> startHack());
        main.addView(btnStart);

        Button btnDelete = createButton("حذف الهاك | REMOVE", "#FF1744");
        btnDelete.setOnClickListener(v -> removeHack());
        main.addView(btnDelete);

        Button btnFloating = createButton("القائمة العائمة | FLOATING", "#00E5FF");
        btnFloating.setOnClickListener(v -> startFloatingMenu());
        main.addView(btnFloating);

        // 5. شاشة التحميل
        loadingOverlay = new LinearLayout(this);
        loadingOverlay.setOrientation(LinearLayout.VERTICAL);
        loadingOverlay.setGravity(Gravity.CENTER);
        loadingOverlay.setBackgroundColor(Color.parseColor("#E60A0810"));
        loadingOverlay.setVisibility(View.GONE);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(600, 30);
        progressBar.setLayoutParams(params);
        loadingOverlay.addView(progressBar);

        tvLoadingText = new TextView(this);
        tvLoadingText.setText("جاري التحميل...");
        tvLoadingText.setTextColor(Color.WHITE);
        tvLoadingText.setTextSize(20);
        tvLoadingText.setPadding(0, 40, 0, 0);
        loadingOverlay.addView(tvLoadingText);

        addContentView(loadingOverlay, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        setContentView(scroll);

        // البحث عن ببجي
        findGamePackage();

        // منطق التطبيق
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
        }
    }

    private Button createButton(String text, String colorHex) {
        Button btn = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 150);
        params.setMargins(0, 25, 0, 25);
        btn.setLayoutParams(params);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(18);
        btn.setAllCaps(false);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(colorHex));
        gd.setCornerRadius(30);
        btn.setBackground(gd);
        return btn;
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
            Toast.makeText(this, "ببجي غير مثبتة على هذا الجهاز!", Toast.LENGTH_LONG).show();
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
                File f = new File(getFilesDir(), PATCH_FILE_NAME);
                downloadFile(PAK_URL, f);

                updateLoadingText("جاري تثبيت ملف الهكر...");
                boolean ok = ShizukuManager.exec("cp -f \"" + f.getAbsolutePath() + "\" \"" + patchFile + "\"") == 0;
                
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
        new Handler(Looper.getMainLooper()).post(() -> tvLoadingText.setText(text));
    }

    private void downloadFile(String urlStr, File out) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(15000);
        c.setInstanceFollowRedirects(true); // مهم جداً عشان GitHub بيحولك لرابط تاني
        c.connect();
        
        int responseCode = c.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP Error: " + responseCode);
        }

        try (InputStream in = c.getInputStream(); FileOutputStream fo = new FileOutputStream(out)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) fo.write(buf, 0, n);
        } finally { c.disconnect(); }
    }
}