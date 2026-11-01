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

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class OverlayService extends Service {

    public static final String ACTION_START = "com.wormgpt.wormpanel.START";
    private static final String CH = "esp_ch";
    private static final int CH_ID = 42;
    private static final String MODEL_NAME = "yolov8n.tflite";

    private WindowManager wm;
    private SurfaceView surface;
    private SurfaceHolder holder;
    private WindowManager.LayoutParams lp;

    private MediaProjection mp;
    private ImageReader reader;
    private VirtualDisplay vd;
    private HandlerThread ht;
    private Handler handler;

    private Interpreter interpreter;
    private GpuDelegate gpuDelegate;
    private int inputSize = 640;

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

        try {
            initPaints();
            initInterpreter();
        } catch (Exception e) {
            stopSelf();
            return START_NOT_STICKY;
        }

        int code = intent.getIntExtra("code", 0);
        Intent data = intent.getParcelableExtra("data");
        MediaProjectionManager mpm = (MediaProjectionManager)
                getSystemService(MEDIA_PROJECTION_SERVICE);
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

    private void initInterpreter() throws Exception {
        File model = new File(new File(getFilesDir(), "models"), MODEL_NAME);
        if (!model.exists()) throw new Exception("model missing");

        Interpreter.Options opts = new Interpreter.Options();
        CompatibilityList compat = new CompatibilityList();
        if (compat.isDelegateSupportedOnThisDevice()) {
            gpuDelegate = new GpuDelegate(compat.getBestOptionsForThisDevice());
            opts.addDelegate(gpuDelegate);
        } else {
            opts.setNumThreads(4);
        }
        RandomAccessFile raf = new RandomAccessFile(model, "r");
        FileChannel ch = raf.getChannel();
        MappedByteBuffer mapped = ch.map(FileChannel.MapMode.READ_ONLY, 0, ch.size());
        interpreter = new Interpreter(mapped, opts);
        raf.close();

        try {
            inputSize = interpreter.getInputTensor(0).shape()[1];
        } catch (Exception ignored) {}
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
        try {
            Bitmap resized = Bitmap.createScaledBitmap(frame, inputSize, inputSize, true);
            ByteBuffer in = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3);
            in.order(ByteOrder.nativeOrder());
            int[] pixels = new int[inputSize * inputSize];
            resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize);
            for (int px : pixels) {
                in.putFloat(((px >> 16) & 0xFF) / 255f);
                in.putFloat(((px >>  8) & 0xFF) / 255f);
                in.putFloat(( px        & 0xFF) / 255f);
            }
            in.rewind();

            int[] outShape = interpreter.getOutputTensor(0).shape();
            int nC = outShape[1] - 4;
            int nB = outShape[2];
            float[][][] out = new float[1][outShape[1]][outShape[2]];
            interpreter.run(in, out);

            List<RectF> boxes = new ArrayList<>();
            List<Float> scores = new ArrayList<>();
            float sx = frame.getWidth() / (float) inputSize;
            float sy = frame.getHeight() / (float) inputSize;

            for (int i = 0; i < nB; i++) {
                float best = 0f;
                int bestC = -1;
                for (int c = 0; c < nC; c++) {
                    float s = out[0][4 + c][i];
                    if (s > best) { best = s; bestC = c; }
                }
                if (bestC != 0 || best < 0.35f) continue;
                float cx = out[0][0][i], cy = out[0][1][i];
                float w  = out[0][2][i], h  = out[0][3][i];
                float x1 = (cx - w / 2) * sx, y1 = (cy - h / 2) * sy;
                float x2 = (cx + w / 2) * sx, y2 = (cy + h / 2) * sy;
                boxes.add(new RectF(x1, y1, x2, y2));
                scores.add(best);
            }
            List<RectF> kept = nms(boxes, scores, 0.45f);

            List<Player> list = new ArrayList<>();
            for (RectF b : kept) {
                float w = b.width(), h = b.height();
                if (w < 20 || h < 40) continue;
                float hx = b.left + w * 0.5f;
                float hy = b.top  + h * 0.12f;
                float chx = b.left + w * 0.5f;
                float chy = b.top  + h * 0.30f;
                list.add(new Player(b, hx, hy, chx, chy));
            }
            players = list;
            draw();
        } catch (Exception ignored) {
        } finally {
            busy = false;
        }
    }

    private List<RectF> nms(List<RectF> boxes, List<Float> scores, float iouTh) {
        Integer[] idx = new Integer[boxes.size()];
        for (int i = 0; i < idx.length; i++) idx[i] = i;
        java.util.Arrays.sort(idx, (a, b) -> Float.compare(scores.get(b), scores.get(a)));
        List<RectF> kept = new ArrayList<>();
        for (int ii : idx) {
            RectF a = boxes.get(ii);
            boolean drop = false;
            for (RectF k : kept) if (iou(a, k) > iouTh) { drop = true; break; }
            if (!drop) kept.add(a);
        }
        return kept;
    }

    private float iou(RectF a, RectF b) {
        float ix1 = Math.max(a.left, b.left), iy1 = Math.max(a.top, b.top);
        float ix2 = Math.min(a.right, b.right), iy2 = Math.min(a.bottom, b.bottom);
        float iw = Math.max(0, ix2 - ix1), ih = Math.max(0, iy2 - iy1);
        float inter = iw * ih;
        float ua = a.width() * a.height() + b.width() * b.height() - inter;
        return ua <= 0 ? 0 : inter / ua;
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
        try { if (interpreter != null) interpreter.close(); } catch (Exception ignored) {}
        try { if (gpuDelegate != null) gpuDelegate.close(); } catch (Exception ignored) {}
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