package com.example.myapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
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
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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

    public static final String ACTION_START = "com.example.myapp.START";
    private static final String CH = "esp_ch";
    private static final int CH_ID = 42;
    private static final String MODEL_NAME = "yolov8n.tflite";

    private WindowManager wm;
    private SurfaceView surface;
    private SurfaceHolder holder;
    private WindowManager.LayoutParams lp;

    private View floatingBox, controlPanel;
    private WindowManager.LayoutParams boxLp, panelLp;

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
    private volatile boolean espEnabled = true;

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
        
        try {
            initInterpreter();
        } catch (Exception e) {
            stopSelf();
            return START_NOT_STICKY;
        }

        int code = intent.getIntExtra("code", 0);
        Intent data = intent.getParcelableExtra("data");
        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (data == null || mpm == null) { stopSelf(); return START_NOT_STICKY; }
        mp = mpm.getMediaProjection(code, data);

        setupOverlay();
        setupFloatingUI();
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
        if (!model.exists()) throw new Exception("Model missing");

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
        
        inputSize = interpreter.getInputTensor(0).shape()[1];
    }

    private void setupOverlay() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        surface = new SurfaceView(this);
        surface.setZOrderOnTop(true);
        holder = surface.getHolder();
        holder.setFormat(PixelFormat.TRANSLUCENT);
        
        surface.setAlpha(0.01f); 
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

    private void setupFloatingUI() {
        floatingBox = new TextView(this);
        ((TextView) floatingBox).setText("HACK");
        ((TextView) floatingBox).setTextColor(Color.GREEN);
        ((TextView) floatingBox).setGravity(Gravity.CENTER);
        ((TextView) floatingBox).setTextSize(14f);
        
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#AA000000"));
        gd.setStroke(3, Color.GREEN);
        gd.setCornerRadius(20);
        floatingBox.setBackground(gd);

        boxLp = new WindowManager.LayoutParams(
                120, 120,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        boxLp.gravity = Gravity.TOP | Gravity.START;
        boxLp.x = 100; boxLp.y = 300;

        floatingBox.setOnTouchListener(new DragTapListener(boxLp, floatingBox, () -> {
            floatingBox.setVisibility(View.GONE);
            controlPanel.setVisibility(View.VISIBLE);
        }));
        wm.addView(floatingBox, boxLp);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(30, 30, 30, 30);
        GradientDrawable panelBg = new GradientDrawable();
        panelBg.setColor(Color.parseColor("#DD111111"));
        panelBg.setStroke(3, Color.GREEN);
        panelBg.setCornerRadius(30);
        ll.setBackground(panelBg);

        TextView btnClose = new TextView(this);
        btnClose.setText("X");
        btnClose.setTextColor(Color.RED);
        btnClose.setTextSize(20f);
        btnClose.setGravity(Gravity.END);
        ll.addView(btnClose);

        Button btnEsp = new Button(this);
        btnEsp.setText("ESP: ON");
        btnEsp.setTextColor(Color.BLACK);
        btnEsp.setBackgroundColor(Color.GREEN);
        ll.addView(btnEsp);

        controlPanel = ll;
        controlPanel.setVisibility(View.GONE);

        panelLp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        panelLp.gravity = Gravity.TOP | Gravity.START;
        panelLp.x = 100; panelLp.y = 450;

        controlPanel.setOnTouchListener(new DragListener(panelLp, controlPanel));

        btnClose.setOnClickListener(v -> {
            controlPanel.setVisibility(View.GONE);
            floatingBox.setVisibility(View.VISIBLE);
        });

        btnEsp.setOnClickListener(v -> {
            espEnabled = !espEnabled;
            btnEsp.setText(espEnabled ? "ESP: ON" : "ESP: OFF");
            btnEsp.setBackgroundColor(espEnabled ? Color.GREEN : Color.RED);
            if (!espEnabled) {
                players.clear();
                draw();
            }
        });

        wm.addView(controlPanel, panelLp);
    }

    private void startCapture() {
        ht = new HandlerThread("cap");
        ht.start();
        handler = new Handler(ht.getLooper());

        DisplayMetrics m = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(m);
        int w = 1280;
        int h = (int)((long) m.heightPixels * w / m.widthPixels);

        reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 3);
        vd = mp.createVirtualDisplay("esp", w, h, m.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.getSurface(), null, handler);

        reader.setOnImageAvailableListener(r -> {
            Image img = null;
            try {
                img = r.acquireLatestImage();
                if (img == null || busy || !espEnabled) return;
                ByteBuffer buf = img.getPlanes()[0].getBuffer();
                Bitmap bmp = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Bitmap.Config.ARGB_8888);
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
            ByteBuffer in = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3).order(ByteOrder.nativeOrder());
            int[] pixels = new int[inputSize * inputSize];
            resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize);
            for (int px : pixels) {
                in.putFloat(((px >> 16) & 0xFF) / 255f);
                in.putFloat(((px >> 8) & 0xFF) / 255f);
                in.putFloat((px & 0xFF) / 255f);
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
                float w = out[0][2][i], h = out[0][3][i];
                boxes.add(new RectF((cx - w/2)*sx, (cy - h/2)*sy, (cx + w/2)*sx, (cy + h/2)*sy));
                scores.add(best);
            }
            
            List<RectF> kept = new ArrayList<>();
            for (int i=0; i<boxes.size(); i++) {
                boolean drop = false;
                for (int j=0; j<kept.size(); j++) {
                    float inter = Math.max(0, Math.min(boxes.get(i).right, kept.get(j).right) - Math.max(boxes.get(i).left, kept.get(j).left)) *
                                  Math.max(0, Math.min(boxes.get(i).bottom, kept.get(j).bottom) - Math.max(boxes.get(i).top, kept.get(j).top));
                    float ua = boxes.get(i).width()*boxes.get(i).height() + kept.get(j).width()*kept.get(j).height() - inter;
                    if (inter / ua > 0.45f) { drop = true; break; }
                }
                if (!drop) kept.add(boxes.get(i));
            }

            List<Player> list = new ArrayList<>();
            for (RectF b : kept) {
                if (b.width() < 20 || b.height() < 40) continue;
                float hx = b.left + b.width() * 0.5f;
                float hy = b.top  + b.height() * 0.12f;
                float chx = b.left + b.width() * 0.5f;
                float chy = b.top  + b.height() * 0.30f;
                list.add(new Player(b, hx, hy, chx, chy));
            }
            players = list;
            draw();
        } catch (Exception ignored) {
        } finally {
            busy = false;
        }
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
            nm.createNotificationChannel(new NotificationChannel(CH, "Overlay", NotificationManager.IMPORTANCE_LOW));
        }
        return new Notification.Builder(this, CH).setContentTitle("Overlay ESP").setSmallIcon(android.R.drawable.ic_menu_view).build();
    }

    @Override
    public void onDestroy() {
        try { if (surface != null && wm != null) wm.removeView(surface); } catch (Exception ignored) {}
        try { if (floatingBox != null && wm != null) wm.removeView(floatingBox); } catch (Exception ignored) {}
        try { if (controlPanel != null && wm != null) wm.removeView(controlPanel); } catch (Exception ignored) {}
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

    private static class DragTapListener implements View.OnTouchListener {
        private final WindowManager.LayoutParams lp;
        private final View v;
        private final Runnable onClick;
        private int startX, startY;
        private float touchX, touchY;
        private long downAt;
        private boolean dragging;

        DragTapListener(WindowManager.LayoutParams lp, View v, Runnable onClick) {
            this.lp = lp; this.v = v; this.onClick = onClick;
        }

        @Override public boolean onTouch(View view, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX = lp.x; startY = lp.y;
                    touchX = e.getRawX(); touchY = e.getRawY();
                    downAt = System.currentTimeMillis();
                    dragging = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int)(e.getRawX() - touchX);
                    int dy = (int)(e.getRawY() - touchY);
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) dragging = true;
                    if (dragging) {
                        lp.x = startX + dx;
                        lp.y = startY + dy;
                        WindowManager wm = (WindowManager) v.getContext().getSystemService(Context.WINDOW_SERVICE);
                        wm.updateViewLayout(v, lp);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!dragging && System.currentTimeMillis() - downAt < 250) onClick.run();
                    return true;
            }
            return false;
        }
    }

    private static class DragListener implements View.OnTouchListener {
        private final WindowManager.LayoutParams lp;
        private final View v;
        private int startX, startY;
        private float touchX, touchY;

        DragListener(WindowManager.LayoutParams lp, View v) {
            this.lp = lp; this.v = v;
        }

        @Override public boolean onTouch(View view, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX = lp.x; startY = lp.y;
                    touchX = e.getRawX(); touchY = e.getRawY();
                    return false;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int)(e.getRawX() - touchX);
                    int dy = (int)(e.getRawY() - touchY);
                    if (Math.abs(dx) > 20 || Math.abs(dy) > 20) {
                        lp.x = startX + dx;
                        lp.y = startY + dy;
                        WindowManager wm = (WindowManager) v.getContext().getSystemService(Context.WINDOW_SERVICE);
                        wm.updateViewLayout(v, lp);
                    }
                    return true;
            }
            return false;
        }
    }
}