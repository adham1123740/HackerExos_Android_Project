package com.example.geminiscreen;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * واجهة Android أصلية مستوحاة من شاشة Gemini المرفقة.
 * لا تستخدم مكتبات خارجية أو موارد صور؛ يمكن تغيير اسم الحزمة ليتوافق مع مشروعك.
 */
public class MainActivity extends Activity {
    private static final int INK = Color.rgb(24, 24, 27);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prepareSystemBars();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        root.setClipChildren(false);
        root.setClipToPadding(false);
        root.addView(new GlowBackgroundView(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        addHeader(root);
        addWelcome(root);
        addComposer(root);
        setContentView(root);
    }

    private void prepareSystemBars() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }

        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        window.getDecorView().setSystemUiVisibility(flags);
    }

    private void addHeader(FrameLayout root) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), 0, dp(12), 0);

        ProfileAvatarView avatar = new ProfileAvatarView(this);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(34), dp(34));
        header.addView(avatar, avatarParams);
        avatar.setContentDescription("صورة الملف الشخصي");

        ComposeIconView compose = new ComposeIconView(this);
        LinearLayout.LayoutParams composeParams = new LinearLayout.LayoutParams(dp(38), dp(42));
        composeParams.leftMargin = dp(9);
        header.addView(compose, composeParams);
        compose.setContentDescription("محادثة جديدة");
        compose.setOnClickListener(v -> Toast.makeText(this, "محادثة جديدة", Toast.LENGTH_SHORT).show());

        View spacer = new View(this);
        header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        LinearLayout modelPicker = new LinearLayout(this);
        modelPicker.setGravity(Gravity.CENTER_VERTICAL);
        modelPicker.setOrientation(LinearLayout.HORIZONTAL);

        ChevronView chevron = new ChevronView(this);
        modelPicker.addView(chevron, new LinearLayout.LayoutParams(dp(22), dp(34)));
        TextView modelName = new TextView(this);
        modelName.setText("Gemini المتكيف");
        modelName.setTextColor(INK);
        modelName.setTextSize(17);
        modelName.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        modelName.setGravity(Gravity.CENTER_VERTICAL);
        modelName.setSingleLine(true);
        modelName.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_LTR);
        modelPicker.addView(modelName, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)));
        modelPicker.setContentDescription("اختيار نموذج Gemini");
        modelPicker.setOnClickListener(v -> Toast.makeText(this, "اختيار النموذج", Toast.LENGTH_SHORT).show());
        header.addView(modelPicker, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)));

        MenuIconView menu = new MenuIconView(this);
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        menuParams.leftMargin = dp(14);
        header.addView(menu, menuParams);
        menu.setContentDescription("القائمة");
        menu.setOnClickListener(v -> Toast.makeText(this, "القائمة", Toast.LENGTH_SHORT).show());

        FrameLayout.LayoutParams headerFrameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52), Gravity.TOP);
        headerFrameParams.topMargin = systemBarSize("status_bar_height", 24) + dp(7);
        root.addView(header, headerFrameParams);
    }

    private void addWelcome(FrameLayout root) {
        LinearLayout welcome = new LinearLayout(this);
        welcome.setOrientation(LinearLayout.VERTICAL);
        welcome.setGravity(Gravity.CENTER);

        GeminiSparkView spark = new GeminiSparkView(this);
        welcome.addView(spark, new LinearLayout.LayoutParams(dp(38), dp(38)));

        TextView greeting = new TextView(this);
        greeting.setText("adham، بماذا نبدأ؟");
        greeting.setTextColor(Color.rgb(18, 18, 20));
        greeting.setTextSize(23);
        greeting.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        greeting.setGravity(Gravity.CENTER);
        greeting.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        greeting.setSingleLine(true);
        LinearLayout.LayoutParams greetingParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        greetingParams.topMargin = dp(13);
        welcome.addView(greeting, greetingParams);

        FrameLayout.LayoutParams welcomeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        welcomeParams.leftMargin = dp(14);
        welcomeParams.rightMargin = dp(14);
        welcomeParams.topMargin = -dp(4);
        root.addView(welcome, welcomeParams);
    }

    private void addComposer(FrameLayout root) {
        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(14), 0, dp(12), 0);
        composer.setBackground(roundRect(Color.WHITE, dp(34)));
        composer.setElevation(dp(5));
        composer.setClipToOutline(true);

        VoiceWaveView wave = new VoiceWaveView(this);
        LinearLayout.LayoutParams waveParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        composer.addView(wave, waveParams);
        wave.setContentDescription("إدخال صوتي");
        wave.setOnClickListener(v -> toastVoiceNotice());

        MicrophoneView microphone = new MicrophoneView(this);
        LinearLayout.LayoutParams micParams = new LinearLayout.LayoutParams(dp(34), dp(38));
        micParams.leftMargin = dp(13);
        composer.addView(microphone, micParams);
        microphone.setContentDescription("الميكروفون");
        microphone.setOnClickListener(v -> toastVoiceNotice());

        EditText prompt = new EditText(this);
        prompt.setSingleLine(true);
        prompt.setTextSize(15);
        prompt.setTextColor(Color.rgb(70, 70, 74));
        prompt.setHintTextColor(Color.rgb(119, 119, 123));
        prompt.setHint("اسأل Gemini");
        prompt.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        prompt.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        prompt.setPadding(dp(4), 0, dp(4), 0);
        prompt.setBackgroundColor(Color.TRANSPARENT);
        prompt.setImeOptions(EditorInfo.IME_ACTION_SEND);
        prompt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        composer.addView(prompt, new LinearLayout.LayoutParams(0, dp(48), 1f));
        prompt.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                return true;
            }
            return false;
        });

        PlusIconView plus = new PlusIconView(this);
        LinearLayout.LayoutParams plusParams = new LinearLayout.LayoutParams(dp(38), dp(42));
        composer.addView(plus, plusParams);
        plus.setContentDescription("إضافة مرفق");
        plus.setOnClickListener(v -> Toast.makeText(this,
                "المرفقات تحتاج إلى ربطها بمشروعك", Toast.LENGTH_SHORT).show());

        FrameLayout.LayoutParams composerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(60), Gravity.BOTTOM);
        composerParams.leftMargin = dp(15);
        composerParams.rightMargin = dp(15);
        composerParams.bottomMargin = systemBarSize("navigation_bar_height", 28) + dp(7);
        root.addView(composer, composerParams);
    }

    private void toastVoiceNotice() {
        Toast.makeText(this, "الإدخال الصوتي يحتاج إلى ربط خدمة صوتية", Toast.LENGTH_SHORT).show();
    }

    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int systemBarSize(String resourceName, int fallbackDp) {
        int id = getResources().getIdentifier(resourceName, "dimen", "android");
        return id == 0 ? dp(fallbackDp) : getResources().getDimensionPixelSize(id);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private abstract class IconView extends View {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        IconView() {
            super(MainActivity.this);
            setClickable(true);
            setFocusable(true);
        }

        void stroke(Canvas canvas, float width, int color) {
            paint.setShader(null);
            paint.setColor(color);
            paint.setStrokeWidth(width);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStyle(Paint.Style.STROKE);
        }

        void fill(int color) {
            paint.setShader(null);
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private class GlowBackgroundView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        GlowBackgroundView(MainActivity context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            paint.setShader(new LinearGradient(0, 0, 0, h,
                    new int[]{Color.WHITE, Color.WHITE, 0xFFFDFEFF, 0xFFF2F9FF, 0xFFD8EDFF, 0xFF9BCFFF},
                    new float[]{0f, 0.62f, 0.76f, 0.87f, 0.95f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(new RadialGradient(w * 0.5f, h * 1.01f, Math.max(w * 1.22f, h * 0.55f),
                    new int[]{0x887FC8FF, 0x557CC5FF, 0x00FFFFFF},
                    new float[]{0f, 0.48f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, h * 0.58f, w, h, paint);
            paint.setShader(null);
        }
    }

    private class GeminiSparkView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        GeminiSparkView(MainActivity context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(getWidth(), getHeight()) * 0.46f;
            path.reset();
            path.moveTo(cx, cy - r);
            path.cubicTo(cx + r * 0.12f, cy - r * 0.35f, cx + r * 0.35f, cy - r * 0.12f, cx + r, cy);
            path.cubicTo(cx + r * 0.36f, cy + r * 0.13f, cx + r * 0.12f, cy + r * 0.35f, cx, cy + r);
            path.cubicTo(cx - r * 0.12f, cy + r * 0.35f, cx - r * 0.36f, cy + r * 0.13f, cx - r, cy);
            path.cubicTo(cx - r * 0.35f, cy - r * 0.12f, cx - r * 0.12f, cy - r * 0.35f, cx, cy - r);
            path.close();
            paint.setShader(new LinearGradient(cx - r, cy - r, cx + r, cy + r,
                    new int[]{0xFFEA4335, 0xFFFBBC05, 0xFF34A853, 0xFF4285F4, 0xFFEA4335},
                    null, Shader.TileMode.CLAMP));
            canvas.drawPath(path, paint);
            paint.setShader(null);
        }
    }

    private class ProfileAvatarView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        ProfileAvatarView(MainActivity context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float r = Math.min(w, h) * 0.48f;
            canvas.save();
            Path clip = new Path();
            clip.addCircle(w / 2f, h / 2f, r, Path.Direction.CW);
            canvas.clipPath(clip);
            paint.setShader(new LinearGradient(0, 0, w, h,
                    new int[]{0xFFBD72E8, 0xFF7441BA, 0xFFE6B6FA}, null, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
            paint.setColor(0xFFEEA9F3);
            canvas.drawOval(new RectF(w * 0.05f, h * 0.12f, w * 0.52f, h * 0.53f), paint);
            paint.setColor(0xFF6A31A8);
            canvas.drawOval(new RectF(w * 0.43f, h * 0.10f, w * 0.96f, h * 0.55f), paint);
            paint.setColor(0xFF2E8B68);
            canvas.drawOval(new RectF(w * 0.12f, h * 0.48f, w * 0.60f, h * 0.95f), paint);
            paint.setColor(0xFF17151F);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, w * 0.045f));
            Path scribble = new Path();
            scribble.moveTo(w * 0.12f, h * 0.31f);
            scribble.quadTo(w * 0.45f, h * 0.63f, w * 0.85f, h * 0.29f);
            scribble.moveTo(w * 0.23f, h * 0.84f);
            scribble.quadTo(w * 0.55f, h * 0.57f, w * 0.84f, h * 0.76f);
            canvas.drawPath(scribble, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.restore();
        }
    }

    private class ComposeIconView extends IconView {
        ComposeIconView(MainActivity context) { super(); }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float s = getResources().getDisplayMetrics().density;
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            stroke(canvas, 1.7f * s, INK);
            RectF loop = new RectF(cx - 11 * s, cy - 8 * s, cx + 9 * s, cy + 9 * s);
            canvas.drawArc(loop, 150, 285, false, paint);
            Path pen = new Path();
            pen.moveTo(cx - 1 * s, cy - 1 * s);
            pen.lineTo(cx + 8 * s, cy - 10 * s);
            pen.quadTo(cx + 11 * s, cy - 12 * s, cx + 13 * s, cy - 9 * s);
            pen.lineTo(cx + 3 * s, cy + 1 * s);
            pen.close();
            canvas.drawPath(pen, paint);
            paint.setStyle(Paint.Style.FILL);
            float[] dotsX = {cx - 14 * s, cx - 11 * s, cx - 8 * s, cx - 4 * s, cx + 11 * s};
            float[] dotsY = {cy - 11 * s, cy - 14 * s, cy - 15 * s, cy - 14 * s, cy + 8 * s};
            for (int i = 0; i < dotsX.length; i++) canvas.drawCircle(dotsX[i], dotsY[i], 1.1f * s, paint);
            paint.setStyle(Paint.Style.STROKE);
        }
    }

    private class ChevronView extends IconView {
        ChevronView(MainActivity context) { super(); }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float s = getResources().getDisplayMetrics().density;
            stroke(canvas, 1.7f * s, INK);
            Path p = new Path();
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            p.moveTo(cx - 4 * s, cy - 2 * s);
            p.lineTo(cx, cy + 2 * s);
            p.lineTo(cx + 4 * s, cy - 2 * s);
            canvas.drawPath(p, paint);
        }
    }

    private class MenuIconView extends IconView {
        MenuIconView(MainActivity context) { super(); }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float s = getResources().getDisplayMetrics().density;
            stroke(canvas, 2f * s, INK);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            canvas.drawLine(cx - 7 * s, cy - 5 * s, cx + 7 * s, cy - 5 * s, paint);
            canvas.drawLine(cx - 7 * s, cy, cx + 7 * s, cy, paint);
            canvas.drawLine(cx - 7 * s, cy + 5 * s, cx + 7 * s, cy + 5 * s, paint);
        }
    }

    private class VoiceWaveView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        VoiceWaveView(MainActivity context) {
            super(context);
            setClickable(true);
            setFocusable(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float s = getResources().getDisplayMetrics().density;
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            paint.setColor(0xFFE1F3FC);
            canvas.drawCircle(cx, cy, Math.min(getWidth(), getHeight()) * 0.49f, paint);
            paint.setColor(INK);
            paint.setStrokeWidth(1.55f * s);
            paint.setStrokeCap(Paint.Cap.ROUND);
            float[] heights = {10, 18, 24, 15, 8};
            for (int i = 0; i < heights.length; i++) {
                float x = cx + (i - 2) * 4.3f * s;
                float half = heights[i] * s * 0.5f;
                canvas.drawLine(x, cy - half, x, cy + half, paint);
            }
        }
    }

    private class MicrophoneView extends IconView {
        MicrophoneView(MainActivity context) { super(); }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float s = getResources().getDisplayMetrics().density;
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            stroke(canvas, 1.65f * s, INK);
            RectF capsule = new RectF(cx - 4 * s, cy - 10 * s, cx + 4 * s, cy + 3 * s);
            canvas.drawRoundRect(capsule, 4 * s, 4 * s, paint);
            Path cradle = new Path();
            cradle.moveTo(cx - 8 * s, cy - 1 * s);
            cradle.cubicTo(cx - 8 * s, cy + 8 * s, cx + 8 * s, cy + 8 * s, cx + 8 * s, cy - 1 * s);
            canvas.drawPath(cradle, paint);
            canvas.drawLine(cx, cy + 7 * s, cx, cy + 11 * s, paint);
            canvas.drawLine(cx - 4 * s, cy + 11 * s, cx + 4 * s, cy + 11 * s, paint);
        }
    }

    private class PlusIconView extends IconView {
        PlusIconView(MainActivity context) { super(); }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float s = getResources().getDisplayMetrics().density;
            stroke(canvas, 1.8f * s, INK);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            canvas.drawLine(cx - 6.5f * s, cy, cx + 6.5f * s, cy, paint);
            canvas.drawLine(cx, cy - 6.5f * s, cx, cy + 6.5f * s, paint);
        }
    }
}