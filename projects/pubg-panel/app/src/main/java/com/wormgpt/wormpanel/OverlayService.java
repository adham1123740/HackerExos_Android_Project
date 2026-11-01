package com.wormgpt.wormpanel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class OverlayService extends Service {

    public static final String ACTION_START = "com.wormgpt.wormpanel.START";
    private static final String CH = "esp_ch";
    private static final int CH_ID = 42;

    private WindowManager wm;
    private SurfaceView surface;
    private SurfaceHolder holder;
    private WindowManager.LayoutParams lp;

    private MediaProjection mp;
    private ImageReader reader;
    private VirtualDisplay vd;
    private HandlerThread ht;
    private Handler handler;

    private ObjectDetector detector;
    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chestPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private volatile List<Player> players = new ArrayList<>();
    private volatile boolean busy = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !ACTION_START.equals(intent.getAction())) return START_STICKY;

        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(CH_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(CH_ID, n);
        }

        initPaints();
        initMLKit();

        int code = intent.getIntExtra("code", 0);
        Intent data = intent.getParcelableExtra("data");
        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (data == null || mpm == null) { stopSelf(); return START_NOT_STICKY; }
        mp = mpm.getMediaProjection(code, data);

        setupOverlay();
        startCapture();
        return START_STICKY;
    }

    private void initPaints() {
        boxPaint.setColor(Color.RED);
        boxPaint.setStrokeWidth(5f);
        boxPaint.setStyle(Paint.Style.STROKE);
        headPaint.setColor(Color.YELLOW);
        headPaint.setStyle(Paint.Style.FILL);
        chestPaint.setColor(Color.MAGENTA);
        chestPaint.setStyle(Paint.Style.FILL);
    }

    private void initMLKit() {
        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build();
        detector = ObjectDetection.getClient(options);
    }

    private void setupOverlay() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        surface = new SurfaceView(this);
        surface.setZOrderOnTop(true);
        holder = surface.getHolder();
        holder.setFormat(PixelFormat.TRANSLUCENT);
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder h) { draw(); }
            @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {}
            @Override public void surfaceDestroyed(SurfaceHolder h) {}
        });

        lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        wm.addView(surface, lp);
    }

    private void startCapture() {
        ht = new HandlerThread("cap");
        ht.start();
        handler = new Handler(ht.getLooper());

        DisplayMetrics m = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(m);
        int srcW = m.widthPixels;
        int srcH = m.heightPixels;
        int density = m.densityDpi;

        int w = 1280;
        int h = (int)((long) srcH * w / srcW);

        reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 3);
        vd = mp.createVirtualDisplay("esp", w, h, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.getSurface(), null, handler);

        reader.setOnImageAvailableListener(r -> {
            Image img = null;
            try {
                img = r.acquireLatestImage();
                if (img == null || busy) return;
                Image.Plane p = img.getPlanes()[0];
                ByteBuffer buf = p.getBuffer();
                int iw = img.getWidth();
                int ih = img.getHeight();
                Bitmap bmp = Bitmap.createBitmap(iw, ih, Bitmap.Config.ARGB_8888);
                bmp.copyPixelsFromBuffer(buf);
                busy = true;
                processFrame(bmp);
            } catch (Exception ignored) {
            } finally {
                if (img != null) img.close();
            }
        }, handler);
    }

    private void processFrame(Bitmap frame) {
        InputImage image = InputImage.fromBitmap(frame, 0);
        detector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<DetectedObject>>() {
                    @Override
                    public void onSuccess(List<DetectedObject> objects) {
                        List<Player> list = new ArrayList<>();
                        for (DetectedObject obj : objects) {
                            RectF boundingBox = new RectF(obj.getBoundingBox());
                            float w = boundingBox.width();
                            float h = boundingBox.height();
                            
                            if (w < 20 || h < 40) continue; // فلتر للضوضاء
                            
                            float hx = boundingBox.left + w * 0.5f;
                            float hy = boundingBox.top  + h * 0.12f;
                            float chx = boundingBox.left + w * 0.5f;
                            float chy = boundingBox.top  + h * 0.30f;
                            
                            list.add(new Player(boundingBox, hx, hy, chx, chy));
                        }
                        players = list;
                        draw();
                        busy = false;
                    }
                })
                .addOnFailureListener(e -> busy = false);
    }

    private void draw() {
        SurfaceHolder h = holder;
        if (h == null) return;
        Canvas c = h.lockCanvas();
        if (c == null) return;
        try {
            c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            for (Player p : players) {
                c.drawRect(p.box, boxPaint);
                c.drawCircle(p.headX, p.headY, 8f, headPaint);
                c.drawCircle(p.chestX, p.chestY, 8f, chestPaint);
                c.drawLine(p.headX, p.headY, p.chestX, p.chestY, chestPaint);
            }
        } finally {
            h.unlockCanvasAndPost(c);
        }
    }

    private Notification buildNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CH, "Overlay", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
        }
        return new Notification.Builder(this, CH)
                .setContentTitle("Overlay ESP")
                .setContentText("ESP يعمل")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build();
    }

    @Override
    public void onDestroy() {
        try { if (surface != null && wm != null) wm.removeView(surface); } catch (Exception ignored) {}
        try { if (vd != null) vd.release(); } catch (Exception ignored) {}
        try { if (reader != null) reader.close(); } catch (Exception ignored) {}
        try { if (mp != null) mp.stop(); } catch (Exception ignored) {}
        try { if (ht != null) ht.quitSafely(); } catch (Exception ignored) {}
        try { if (detector != null) detector.close(); } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) { return null; }

    private static class Player {
        final RectF box;
        final float headX, headY, chestX, chestY;
        Player(RectF b, float hx, float hy, float cx, float cy) {
            box = b; headX = hx; headY = hy; chestX = cx; chestY = cy;
        }
    }
}