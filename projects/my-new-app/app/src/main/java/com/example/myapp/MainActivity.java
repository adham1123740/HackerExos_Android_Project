package com.example.myapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQUEST_OVERLAY = 100;

    private TextView statusText;
    private Button startButton;
    private Button stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createInterface();
    }

    private void createInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 48, 32, 32);
        root.setBackgroundColor(0xFF101010);

        TextView title = new TextView(this);
        title.setText("OVERLAY CONTROL");
        title.setTextColor(0xFF00FF88);
        title.setTextSize(25);
        title.setPadding(0, 0, 0, 24);
        root.addView(title);

        TextView description = new TextView(this);
        description.setText(
                "واجهة اختبار المربع العائم\n\n" +
                "يمكنك تحريك المربع فوق التطبيقات، " +
                "وفتح لوحة التحكم وإغلاقها."
        );
        description.setTextColor(0xFFE0E0E0);
        description.setTextSize(16);
        description.setPadding(0, 0, 0, 24);
        root.addView(description);

        statusText = new TextView(this);
        statusText.setTextColor(0xFFFFFFFF);
        statusText.setTextSize(15);
        statusText.setPadding(0, 0, 0, 24);
        root.addView(statusText);

        startButton = new Button(this);
        startButton.setText("تشغيل المربع العائم");
        root.addView(startButton);

        stopButton = new Button(this);
        stopButton.setText("إيقاف المربع العائم");
        root.addView(stopButton);

        setContentView(root);

        updateStatus();

        startButton.setOnClickListener(v -> checkPermissionAndStart());

        stopButton.setOnClickListener(v -> {
            stopService(new Intent(
                    MainActivity.this,
                    OverlayService.class
            ));

            Toast.makeText(
                    MainActivity.this,
                    "تم طلب إيقاف الخدمة",
                    Toast.LENGTH_SHORT
            ).show();

            updateStatus();
        });
    }

    private void checkPermissionAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );

            startActivityForResult(intent, REQUEST_OVERLAY);
            return;
        }

        startOverlayService();
    }

    private void startOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        intent.setAction(OverlayService.ACTION_START);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            Toast.makeText(
                    this,
                    "تم إرسال طلب تشغيل الواجهة",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "تعذر تشغيل الخدمة: " + e.getClass().getSimpleName(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                startOverlayService();
            } else {
                Toast.makeText(
                        this,
                        "يجب السماح بالظهور فوق التطبيقات",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void updateStatus() {
        boolean allowed = Settings.canDrawOverlays(this);

        statusText.setText(
                "حالة صلاحية الظهور فوق التطبيقات: " +
                (allowed ? "مسموح" : "غير مفعّلة")
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (statusText != null) {
            updateStatus();
        }
    }
}