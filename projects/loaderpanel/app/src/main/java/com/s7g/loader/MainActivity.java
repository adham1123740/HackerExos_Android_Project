package com.s7g.loader;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
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

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_OVERLAY = 100;

    // المسارات
    private static final String GAME_PKG = "com.tencent.ig";
    private static final String PAKS_DIR = "/storage/emulated/0/Android/data/com.tencent.ig/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/";
    private static final String PATCH_FILE = PAKS_DIR + "game_patch_4.6.1.21597.pak";
    private static final String PROTECT_FILE = PAKS_DIR + "pakchunk0-Android_NoES2.pak";
    private static final String ACE_LIB = "/data/data/com.tencent.ig/lib/libanogs.so";

    private static final String PREFS_NAME = "ExosPrefs";
    private static final String KEY_DOWNLOADED = "is_downloaded";

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
        
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#0A0810"));
        
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(50, 60, 50, 50);
        main.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(main);

        // 1. الهيدر
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(30, 30, 30, 30);
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.parseColor("#B300B3"), Color.parseColor("#330033")});
        gd.setCornerRadius(50);
        gd.setStroke(4, Color.parseColor("#D500F9"));
        header.setBackground(gd);

        TextView title = new TextView(this);
        title.setText("PANEL EXOS\nDEVELOPER");
        title.setTextColor(Color.parseColor("#00E5FF"));
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        header.addView(title);
        main.addView(header);

        // 2. Shizuku Status
        tvShizuku = new TextView(this);
        tvShizuku.setText("Shizuku: جاري الفحص...");
        tvShizuku.setTextColor(Color.RED);
        tvShizuku.setGravity(Gravity.CENTER);
        tvShizuku.setPadding(0, 30, 0, 30);
        main.addView(tvShizuku);

        // 3. Game Card
        LinearLayout gameCard = new LinearLayout(this);
        gameCard.setOrientation(LinearLayout.VERTICAL);
        gameCard.setPadding(30, 30, 30, 30);
        GradientDrawable gdCard = new GradientDrawable();
        gdCard.setColor(Color.parseColor("#0F0B1A"));
        gdCard.setCornerRadius(30);
        gdCard.setStroke(2, Color.parseColor("#D500F9"));
        gameCard.setBackground(gdCard);

        TextView gameName = new TextView(this);
        gameName.setText("PUBG Mobile | Global\ncom.tencent.ig");
        gameName.setTextColor(Color.WHITE);
        gameName.setTextSize(16);
        gameCard.addView(gameName);

        tvStatus = new TextView(this);
        tvStatus.setText("جاهز للبدء");
        tvStatus.setTextColor(Color.GRAY);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setPadding(0, 20, 0, 20);
        gameCard.addView(tvStatus);
        main.addView(gameCard);

        // 4. Buttons
        Button btnStart = createButton("تشغيل الهاك | ENABLE", "#D500F9");
        btnStart.setOnClickListener(v -> startHack());
        main.addView(btnStart);

        Button btnDelete = createButton("حذف الهاك | REMOVE", "#FF1744");
        btnDelete.setOnClickListener(v -> removeHack());
        main.addView(btnDelete);

        Button btnFloating = createButton("القائمة العائمة | FLOATING", "#00E5FF");
        btnFloating.setOnClickListener(v -> startFloatingMenu());
        main.addView(btnFloating);

        // 5. Loading Screen
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
        tvLoadingText.setTextSize(18);
        tvLoadingText.setPadding(0, 30, 0, 0);
        loadingOverlay.addView(tvLoadingText);

        addContentView(loadingOverlay, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        setContentView(scroll);

        try {
            Shizuku.addRequestPermissionResultListener((code, res) -> runOnUiThread(this::updateShizukuStatus));
        } catch (Exception ignored) {}
        
        checkPermissions();
        updateShizukuStatus();
    }

    private Button createButton(String text, String colorHex) {
        Button btn = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 140);
        params.setMargins(0, 20, 0, 20);
        btn.setLayoutParams(params);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(16);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(colorHex));
        gd.setCornerRadius(20);
        btn.setBackground(gd);
        return btn;
    }

    private void updateShizukuStatus() {
        if (isShizukuReady()) {
            tvShizuku.setText("Shizuku: شغال ✔");
            tvShizuku.setTextColor(Color.parseColor("#00E5FF"));
        } else {
            tvShizuku.setText("Shizuku: غير شغال ✘");
            tvShizuku.setTextColor(Color.RED);
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

        if (prefs.getBoolean(KEY_DOWNLOADED, false)) {
            launchGame();
            return;
        }

        loadingOverlay.setVisibility(View.VISIBLE);
        ObjectAnimator.ofFloat(loadingOverlay, "alpha", 0f, 1f).setDuration(500).start();
        
        new Thread(() -> {
            try {
                updateLoadingText("جاري تحميل ملف الهكر...");
                File f = new File(getFilesDir(), "game_patch.pak");
                copyFromAssets("game_patch.pak", f);

                updateLoadingText("جاري تثبيت ملف الهكر...");
                boolean ok = ShizukuManager.exec("cp -f \"" + f.getAbsolutePath() + "\" \"" + PATCH_FILE + "\"") == 0;
                
                updateLoadingText("جاري إغلاق الحماية...");
                ShizukuManager.exec("rm -f \"" + PROTECT_FILE + "\"");
                ShizukuManager.exec("rm -f \"" + ACE_LIB + "\"");
                
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

    private void removeHack() {
        if (!isShizukuReady()) {
            Toast.makeText(this, "يجب تفعيل Shizuku أولاً!", Toast.LENGTH_LONG).show();
            return;
        }
        
        new Thread(() -> {
            try {
                ShizukuManager.exec("rm -f \"" + PATCH_FILE + "\"");
                
                File f = new File(getFilesDir(), "game_patch.pak");
                if (f.exists()) f.delete();
                
                prefs.edit().putBoolean(KEY_DOWNLOADED, false).apply();
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    tvStatus.setText("تم حذف الهاك بنجاح");
                    Toast.makeText(this, "تم الحذف", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> 
                    Toast.makeText(this, "خطأ في الحذف: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void launchGame() {
        new Handler(Looper.getMainLooper()).post(() -> {
            tvStatus.setText("جاري الدخول إلى اللعبة...");
            Intent i = getPackageManager().getLaunchIntentForPackage(GAME_PKG);
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

    private void copyFromAssets(String assetName, File outFile) throws Exception {
        try (InputStream in = getAssets().open(assetName);
             FileOutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }
}