package com.s7g.loader;

import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import rikka.shizuku.Shizuku;

public class ShizukuManager {
    public static final int REQUEST_CODE = 1001;
    private static ShizukuManager instance;
    private boolean isGranted = false;

    public static ShizukuManager getInstance() {
        if (instance == null) instance = new ShizukuManager();
        return instance;
    }

    public void init() {
        Shizuku.addBinderReceivedListenerSticky(() -> {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                isGranted = true;
            }
        });
    }

    public void requestPermission() {
        if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
            return; // إصدار Shizuku قديم جداً
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(REQUEST_CODE);
        }
    }

    // تنفيذ أمر نسخ الملف باستخدام صلاحيات Shizuku (UserService)
    public boolean copyFile(String source, String dest) {
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        try {
            // استخدام أمر shell لنسخ الملف
            String cmd = "cp -f \"" + source + "\" \"" + dest + "\"";
            Process process = Shizuku.newProcess(new String[]{"sh", "-c", cmd}, null, null);
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}