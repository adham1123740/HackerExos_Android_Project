package com.s7g.loader;

import android.content.pm.PackageManager;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;
import java.io.BufferedReader;
import java.io.InputStreamReader;

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

    public static String exec(String cmd) {
        if (!isGranted()) return "Error: Shizuku permission denied";
        try {
            // تشغيل الأمر باستخدام Shizuku
            ShizukuRemoteProcess process = Shizuku.newProcess(new String[]{"sh", "-c", cmd}, null, null);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
            return output.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}