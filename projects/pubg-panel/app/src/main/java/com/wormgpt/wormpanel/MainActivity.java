package com.wormgpt.wormpanel;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_OVERLAY = 100;
    private static final int REQ_CAPTURE = 200;
    private MediaProjectionManager mpm;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        
        mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        Button btnDownload = findViewById(R.id.btnDownload);
        Button btnStart = findViewById(R.id.btnStart);

        btnDownload.setVisibility(View.GONE);
        btnStart.setText("تفعيل لودار ببجي");
        btnStart.setEnabled(true);

        btnStart.setOnClickListener(v -> checkAndRequestOverlay());
    }

    private void checkAndRequestOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_OVERLAY);
        } else {
            startScreenCapture();
        }
    }

    private void startScreenCapture() {
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        
        if (req == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                startScreenCapture();
            } else {
                Toast.makeText(this, "لازم تفعل صلاحية 'الظهور فوق التطبيقات'", Toast.LENGTH_LONG).show();
            }
        } else if (req == REQ_CAPTURE) {
            if (res == Activity.RESULT_OK && data != null) {
                Intent svc = new Intent(this, OverlayService.class);
                svc.setAction(OverlayService.ACTION_START);
                svc.putExtra("code", res);
                svc.putExtra("data", data);
                startForegroundService(svc);
                
                openPubg();
                
                finish();
            } else {
                Toast.makeText(this, "مشاركة الشاشة مرفوضة", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openPubg() {
        String[] packages = {
                "com.tencent.ig",      // ببجي العالمية
                "com.pubg.krmobile",  // ببجي الكورية
                "com.rekoo.pubgm"     // ببجي التايوانية
        };
        
        for (String pkg : packages) {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
                return;
            }
        }
        
        Toast.makeText(this, "لم يتم العثور على ببجي، افتحها يدوياً", Toast.LENGTH_LONG).show();
    }
}