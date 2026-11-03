package com.s7g.loader;

import android.content.pm.PackageManager;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

public class ShizukuManager {
    public static final int REQUEST_CODE = 1001;

    public static boolean isGranted() {
        try {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            return false;
        }
    }

    public static void requestPermission() {
        try {
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) return;
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(REQUEST_CODE);
            }
        } catch (Exception ignored) {}
    }

    public static int exec(String cmd) {
        if (!isGranted()) return -1;
        try {
            java.lang.reflect.Method method = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            method.setAccessible(true);
            Object processObj = method.invoke(null, new String[]{"sh", "-c", cmd}, null, null);
            
            ShizukuRemoteProcess process = (ShizukuRemoteProcess) processObj;
            return process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}