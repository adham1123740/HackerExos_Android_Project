package com.worm.gpt;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

/**
 * واجهة Android أصلية بطابع أحمر، مع سجل محادثات ومرفقات وإملاء صوتي محلي.
 * إرسال الرسائل إلى نموذج ذكاء اصطناعي غير مفعّل إلى أن يوفّر المستخدم خدمة موثقة.
 */
public class MainActivity extends Activity {
    private static final int INK = Color.rgb(34, 23, 26);
    private static final int RED = Color.rgb(211, 35, 57);
    private static final int RED_DARK = Color.rgb(147, 17, 37);
    private static final int PICK_FILE_REQUEST = 4101;
    private static final int RECORD_AUDIO_REQUEST = 4102;
    private static final String PREFS_NAME = "wormgpt_local_chats";
    private static final String MODEL_KEY = "selected_model";

    private final ArrayList<ChatRecord> conversations = new ArrayList<>();
    private final ArrayList<Attachment> pendingAttachments = new ArrayList<>();
    private SharedPreferences preferences;
    private ChatRecord currentConversation;
    private String selectedModel = "WormGPT V1";
    private FrameLayout root;
    private View welcomeView;
    private ScrollView chatScroll;
    private LinearLayout messageList;
    private EditText prompt;
    private VoiceWaveView sendOrVoiceButton;
    private MicrophoneView microphone;
    private TextView modelName;
    private TextView attachmentLabel;
    private SpeechRecognizer speechRecognizer;
    private boolean listening;
    private String speechPrefix = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prepareSystemBars();
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        selectedModel = preferences.getString(MODEL_KEY, "WormGPT V1");
        loadConversations();

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        root.setClipChildren(false);
        root.setClipToPadding(false);
        root.addView(new GlowBackgroundView(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        addHeader(root);
        addChatArea(root);
        addWelcome(root);
        addComposer(root);
        setContentView(root);
        refreshConversationVisibility();
    }

    private void prepareSystemBars() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
        if (Build.VERSION.SDK_INT >= 29) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        window.getDecorView().setSystemUiVisibility(flags);
    }

    private void addHeader(FrameLayout target) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
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
        compose.setOnClickListener(v -> startNewConversation());

        View spacer = new View(this);
        header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        LinearLayout modelPicker = new LinearLayout(this);
        modelPicker.setGravity(Gravity.CENTER_VERTICAL);
        modelPicker.setOrientation(LinearLayout.HORIZONTAL);
        ChevronView chevron = new ChevronView(this);
        modelPicker.addView(chevron, new LinearLayout.LayoutParams(dp(22), dp(34)));
        modelName = new TextView(this);
        modelName.setTextColor(RED_DARK);
        modelName.setTextSize(16);
        modelName.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        modelName.setGravity(Gravity.CENTER_VERTICAL);
        modelName.setSingleLine(true);
        modelName.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_LTR);
        modelPicker.addView(modelName, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)));
        modelPicker.setContentDescription("اختيار نموذج WormGPT");
        modelPicker.setClickable(true);
        modelPicker.setFocusable(true);
        modelPicker.setOnClickListener(v -> showModelPicker());
        header.addView(modelPicker, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)));
        updateModelName();

        MenuIconView menu = new MenuIconView(this);
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        menuParams.leftMargin = dp(14);
        header.addView(menu, menuParams);
        menu.setContentDescription("المحادثات والإعدادات المحلية");
        menu.setOnClickListener(v -> showHistoryDrawer());

        FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52), Gravity.TOP);
        headerParams.topMargin = systemBarSize("status_bar_height", 24) + dp(7);
        target.addView(header, headerParams);
    }

    private void addChatArea(FrameLayout target) {
        chatScroll = new ScrollView(this);
        chatScroll.setFillViewport(false);
        chatScroll.setClipToPadding(false);
        chatScroll.setPadding(dp(14), dp(8), dp(14), dp(8));
        messageList = new LinearLayout(this);
        messageList.setOrientation(LinearLayout.VERTICAL);
        messageList.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        chatScroll.addView(messageList, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.TOP);
        params.topMargin = systemBarSize("status_bar_height", 24) + dp(66);
        params.bottomMargin = systemBarSize("navigation_bar_height", 28) + dp(84);
        target.addView(chatScroll, params);
        chatScroll.setVisibility(View.GONE);
    }

    private void addWelcome(FrameLayout target) {
        LinearLayout welcome = new LinearLayout(this);
        welcome.setOrientation(LinearLayout.VERTICAL);
        welcome.setGravity(Gravity.CENTER);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.wormgpt_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setBackground(roundRect(Color.rgb(6, 6, 8), dp(12)));
        logo.setClipToOutline(true);
        welcome.addView(logo, new LinearLayout.LayoutParams(dp(58), dp(58)));

        TextView greeting = new TextView(this);
        greeting.setText("أهلًا Adham، بماذا نبدأ؟");
        greeting.setTextColor(Color.rgb(38, 25, 28));
        greeting.setTextSize(23);
        greeting.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        greeting.setGravity(Gravity.CENTER);
        greeting.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        greeting.setSingleLine(true);
        LinearLayout.LayoutParams greetingParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        greetingParams.topMargin = dp(13);
        welcome.addView(greeting, greetingParams);

        TextView subtext = new TextView(this);
        subtext.setText("واجهة تجريبية — لا يوجد اتصال بنموذج بعد");
        subtext.setTextColor(Color.rgb(128, 89, 96));
        subtext.setTextSize(13);
        subtext.setGravity(Gravity.CENTER);
        subtext.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = dp(8);
        welcome.addView(subtext, subParams);

        FrameLayout.LayoutParams welcomeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        welcomeParams.leftMargin = dp(14);
        welcomeParams.rightMargin = dp(14);
        welcomeParams.topMargin = -dp(4);
        welcomeView = welcome;
        target.addView(welcomeView, welcomeParams);
    }

    private void addComposer(FrameLayout target) {
        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(12), 0, dp(10), 0);
        composer.setBackground(roundRect(Color.WHITE, dp(32)));
        composer.setElevation(dp(6));
        composer.setClipToOutline(true);

        sendOrVoiceButton = new VoiceWaveView(this);
        LinearLayout.LayoutParams waveParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        composer.addView(sendOrVoiceButton, waveParams);
        sendOrVoiceButton.setContentDescription("تحدث صوتيًا أو أرسل الرسالة");
        sendOrVoiceButton.setOnClickListener(v -> {
            if (prompt != null && prompt.getText().toString().trim().length() > 0
                    || !pendingAttachments.isEmpty()) {
                sendMessage();
            } else {
                Toast.makeText(this,
                        "المحادثة الصوتية الحية تحتاج إلى ربط خدمة ذكاء اصطناعي. استخدم الميكروفون للإملاء.",
                        Toast.LENGTH_LONG).show();
            }
        });

        microphone = new MicrophoneView(this);
        LinearLayout.LayoutParams micParams = new LinearLayout.LayoutParams(dp(34), dp(40));
        micParams.leftMargin = dp(8);
        composer.addView(microphone, micParams);
        microphone.setContentDescription("إملاء صوتي باللغة العربية");
        microphone.setOnClickListener(v -> toggleSpeechRecognition());

        prompt = new EditText(this);
        prompt.setSingleLine(true);
        prompt.setTextSize(15);
        prompt.setTextColor(Color.rgb(58, 43, 47));
        prompt.setHintTextColor(Color.rgb(151, 122, 128));
        prompt.setHint("اكتب رسالة إلى WormGPT");
        prompt.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        prompt.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        prompt.setPadding(dp(4), 0, dp(4), 0);
        prompt.setBackgroundColor(Color.TRANSPARENT);
        prompt.setImeOptions(EditorInfo.IME_ACTION_SEND);
        prompt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        composer.addView(prompt, new LinearLayout.LayoutParams(0, dp(48), 1f));
        prompt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (sendOrVoiceButton != null) {
                    sendOrVoiceButton.setSendMode(s.length() > 0 || !pendingAttachments.isEmpty());
                }
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        prompt.setOnEditorActionListener((view, actionId, event) -> {
            boolean enterSend = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_SEND || enterSend) {
                sendMessage();
                return true;
            }
            return false;
        });

        PlusIconView plus = new PlusIconView(this);
        LinearLayout.LayoutParams plusParams = new LinearLayout.LayoutParams(dp(38), dp(42));
        composer.addView(plus, plusParams);
        plus.setContentDescription("إرفاق صورة أو ملف");
        plus.setOnClickListener(v -> showAttachmentMenu(plus));

        attachmentLabel = new TextView(this);
        attachmentLabel.setTextColor(RED_DARK);
        attachmentLabel.setTextSize(11);
        attachmentLabel.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        attachmentLabel.setSingleLine(true);
        attachmentLabel.setVisibility(View.GONE);

        LinearLayout composerColumn = new LinearLayout(this);
        composerColumn.setOrientation(LinearLayout.VERTICAL);
        composerColumn.setPadding(dp(15), dp(5), dp(15), dp(6));
        composerColumn.addView(attachmentLabel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(18)));
        composerColumn.addView(composer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(60)));
        composerColumn.setBackgroundColor(Color.TRANSPARENT);
        FrameLayout.LayoutParams composerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        composerParams.leftMargin = dp(4);
        composerParams.rightMargin = dp(4);
        composerParams.bottomMargin = systemBarSize("navigation_bar_height", 28) + dp(3);
        target.addView(composerColumn, composerParams);
    }

    private void showAttachmentMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("إرفاق صورة").setOnMenuItemClickListener(item -> {
            openDocumentPicker("image/*");
            return true;
        });
        menu.getMenu().add("إرفاق ملف").setOnMenuItemClickListener(item -> {
            openDocumentPicker("*/*");
            return true;
        });
        menu.show();
    }

    private void openDocumentPicker(String mimeType) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, PICK_FILE_REQUEST);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "منتقي الملفات غير متاح على هذا الجهاز", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_FILE_REQUEST || resultCode != RESULT_OK || data == null) return;
        int count = 0;
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                keepUriPermission(uri, data.getFlags());
                pendingAttachments.add(new Attachment(uri, getDisplayName(uri)));
                count++;
            }
        } else if (data.getData() != null) {
            Uri uri = data.getData();
            keepUriPermission(uri, data.getFlags());
            pendingAttachments.add(new Attachment(uri, getDisplayName(uri)));
            count = 1;
        }
        updateAttachmentLabel();
        if (count > 0) Toast.makeText(this, "تمت إضافة " + count + " مرفق إلى الرسالة", Toast.LENGTH_SHORT).show();
    }

    private void keepUriPermission(Uri uri, int flags) {
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    flags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
        } catch (SecurityException ignored) {
            // بعض منتقيات الملفات تمنح وصولًا مؤقتًا فقط؛ يظل الملف متاحًا للجلسة الحالية.
        }
    }

    private String getDisplayName(Uri uri) {
        String name = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) name = cursor.getString(index);
            }
        } catch (Exception ignored) { }
        return name == null || name.trim().isEmpty() ? "مرفق" : name;
    }

    private void updateAttachmentLabel() {
        if (attachmentLabel == null) return;
        if (pendingAttachments.isEmpty()) {
            attachmentLabel.setVisibility(View.GONE);
            attachmentLabel.setText("");
        } else {
            StringBuilder label = new StringBuilder("المرفقات: ");
            for (int i = 0; i < pendingAttachments.size(); i++) {
                if (i > 0) label.append("، ");
                label.append(pendingAttachments.get(i).name);
            }
            attachmentLabel.setText(label.toString());
            attachmentLabel.setVisibility(View.VISIBLE);
        }
        if (sendOrVoiceButton != null && prompt != null) {
            sendOrVoiceButton.setSendMode(prompt.getText().length() > 0 || !pendingAttachments.isEmpty());
        }
    }

    private void toggleSpeechRecognition() {
        if (listening) {
            if (speechRecognizer != null) speechRecognizer.stopListening();
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "خدمة التعرف على الكلام غير متاحة على هذا الجهاز", Toast.LENGTH_LONG).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_REQUEST);
            return;
        }
        beginSpeechRecognition();
    }

    private void beginSpeechRecognition() {
        try {
            if (speechRecognizer != null) speechRecognizer.destroy();
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {
                    listening = true;
                    microphone.setActive(true);
                    Toast.makeText(MainActivity.this, "تحدث الآن — التعرف الصوتي بالعربية", Toast.LENGTH_SHORT).show();
                }
                @Override public void onBeginningOfSpeech() { }
                @Override public void onRmsChanged(float rmsdB) { }
                @Override public void onBufferReceived(byte[] buffer) { }
                @Override public void onEndOfSpeech() {
                    listening = false;
                    microphone.setActive(false);
                }
                @Override public void onError(int error) {
                    listening = false;
                    microphone.setActive(false);
                    if (error != SpeechRecognizer.ERROR_CLIENT) {
                        Toast.makeText(MainActivity.this, "لم أتمكن من تحويل الصوت إلى نص؛ حاول مرة أخرى", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onResults(Bundle results) {
                    listening = false;
                    microphone.setActive(false);
                    ArrayList<String> matches = results == null ? null
                            : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) applyTranscript(matches.get(0));
                }
                @Override public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults == null ? null
                            : partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) applyTranscript(matches.get(0));
                }
                @Override public void onEvent(int eventType, Bundle params) { }
            });
            speechPrefix = prompt.getText().toString().trim();
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-EG");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            listening = false;
            microphone.setActive(false);
            Toast.makeText(this, "تعذر بدء خدمة الإملاء الصوتي", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyTranscript(String recognizedText) {
        String combined = speechPrefix.isEmpty() ? recognizedText : speechPrefix + " " + recognizedText;
        prompt.setText(combined);
        prompt.setSelection(prompt.length());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                beginSpeechRecognition();
            } else {
                Toast.makeText(this, "يلزم السماح بالميكروفون لاستخدام الإملاء الصوتي", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void sendMessage() {
        String text = prompt.getText().toString().trim();
        if (text.isEmpty() && pendingAttachments.isEmpty()) return;
        StringBuilder message = new StringBuilder(text);
        for (Attachment attachment : pendingAttachments) {
            if (message.length() > 0) message.append("\n");
            message.append("[مرفق: ").append(attachment.name).append("]");
        }
        String savedMessage = message.toString();
        if (currentConversation == null) {
            currentConversation = new ChatRecord(UUID.randomUUID().toString(), makeTitle(savedMessage));
            conversations.add(0, currentConversation);
        } else if (currentConversation.messages.isEmpty()) {
            currentConversation.title = makeTitle(savedMessage);
        }
        currentConversation.messages.add(savedMessage);
        currentConversation.updatedAt = System.currentTimeMillis();
        saveConversations();

        addBubble(savedMessage, true);
        addConnectionNotice();
        refreshConversationVisibility();
        scrollChatToBottom();
        prompt.setText("");
        pendingAttachments.clear();
        updateAttachmentLabel();
    }

    private String makeTitle(String text) {
        String compact = text.replace('\n', ' ').trim();
        if (compact.length() > 42) return compact.substring(0, 42) + "…";
        return compact.isEmpty() ? "محادثة جديدة" : compact;
    }

    private void addBubble(String text, boolean userMessage) {
        FrameLayout row = new FrameLayout(this);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dp(10);
        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextColor(userMessage ? Color.rgb(73, 27, 37) : INK);
        bubble.setTextSize(15);
        bubble.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        bubble.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        bubble.setPadding(dp(14), dp(11), dp(14), dp(11));
        bubble.setBackground(roundRect(userMessage ? 0xFFFFE9ED : Color.WHITE, dp(20)));
        if (!userMessage) bubble.setElevation(dp(1));
        FrameLayout.LayoutParams bubbleParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                userMessage ? Gravity.RIGHT : Gravity.LEFT);
        bubbleParams.leftMargin = dp(14);
        bubbleParams.rightMargin = dp(14);
        bubble.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.78f));
        row.addView(bubble, bubbleParams);
        messageList.addView(row, rowParams);
    }

    private void addConnectionNotice() {
        TextView notice = new TextView(this);
        notice.setText("لم يتم إرسال الرسالة إلى نموذج: لم يُضبط اتصال AI بعد. أُبقيت الرسالة في سجل هذا الجهاز فقط.");
        notice.setTextColor(Color.rgb(124, 80, 88));
        notice.setTextSize(12);
        notice.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        notice.setGravity(Gravity.RIGHT);
        notice.setPadding(dp(12), dp(9), dp(12), dp(9));
        GradientDrawable bg = roundRect(0xFFFFF7F8, dp(14));
        bg.setStroke(dp(1), 0xFFF3D3D9);
        notice.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(6), 0, dp(6), dp(18));
        messageList.addView(notice, params);
    }

    private void refreshConversationVisibility() {
        boolean hasMessages = currentConversation != null && !currentConversation.messages.isEmpty();
        welcomeView.setVisibility(hasMessages ? View.GONE : View.VISIBLE);
        chatScroll.setVisibility(hasMessages ? View.VISIBLE : View.GONE);
    }

    private void scrollChatToBottom() {
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void startNewConversation() {
        currentConversation = null;
        messageList.removeAllViews();
        refreshConversationVisibility();
        prompt.setText("");
        pendingAttachments.clear();
        updateAttachmentLabel();
        Toast.makeText(this, "بدأت محادثة جديدة", Toast.LENGTH_SHORT).show();
    }

    private void showModelPicker() {
        String[] models = {"WormGPT V1", "WormGPT V2"};
        int checked = "WormGPT V2".equals(selectedModel) ? 1 : 0;
        new AlertDialog.Builder(this)
                .setTitle("اختيار النموذج")
                .setSingleChoiceItems(models, checked, (dialog, which) -> {
                    selectedModel = models[which];
                    preferences.edit().putString(MODEL_KEY, selectedModel).apply();
                    updateModelName();
                    dialog.dismiss();
                })
                .setNegativeButton("إغلاق", null)
                .show();
    }

    private void updateModelName() {
        if (modelName != null) modelName.setText(selectedModel);
    }

    private void showHistoryDrawer() {
        Dialog dialog = new Dialog(this);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0x55000000);
        overlay.setOnClickListener(v -> dialog.dismiss());

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        panel.setPadding(dp(18), dp(28), dp(18), dp(18));
        GradientDrawable panelBg = roundRect(Color.WHITE, dp(25));
        panelBg.setStroke(dp(1), 0xFFF2E1E4);
        panel.setBackground(panelBg);
        panel.setElevation(dp(8));
        panel.setOnClickListener(v -> { });
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                getResources().getDisplayMetrics().widthPixels - dp(38),
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.RIGHT | Gravity.TOP);
        panelParams.topMargin = dp(8);
        panelParams.bottomMargin = dp(8);
        panelParams.rightMargin = dp(8);
        overlay.addView(panel, panelParams);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(this);
        title.setText("المحادثات");
        title.setTextColor(INK);
        title.setTextSize(21);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, dp(44), 1f));
        TextView close = drawerButton("×", false);
        titleRow.addView(close, new LinearLayout.LayoutParams(dp(42), dp(42)));
        close.setOnClickListener(v -> dialog.dismiss());
        panel.addView(titleRow);

        TextView newChat = drawerButton("＋    محادثة جديدة", true);
        LinearLayout.LayoutParams newChatParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        newChatParams.topMargin = dp(14);
        panel.addView(newChat, newChatParams);
        newChat.setOnClickListener(v -> {
            dialog.dismiss();
            startNewConversation();
        });

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setHint("البحث في المحادثات");
        search.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        search.setInputType(InputType.TYPE_CLASS_TEXT);
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setBackground(roundRect(0xFFF8F4F5, dp(24)));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        searchParams.topMargin = dp(12);
        panel.addView(search, searchParams);

        TextView recent = new TextView(this);
        recent.setText("الأحدث");
        recent.setTextColor(0xFF88777B);
        recent.setTextSize(13);
        recent.setPadding(dp(4), dp(18), dp(4), dp(8));
        panel.addView(recent);

        ScrollView historyScroll = new ScrollView(this);
        LinearLayout historyItems = new LinearLayout(this);
        historyItems.setOrientation(LinearLayout.VERTICAL);
        historyScroll.addView(historyItems, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(historyScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        Runnable updateHistory = () -> renderHistoryList(historyItems, search.getText().toString(), dialog);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateHistory.run(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        renderHistoryList(historyItems, "", dialog);

        TextView footer = new TextView(this);
        footer.setText("المحادثات محفوظة على هذا الجهاز فقط");
        footer.setTextColor(0xFF8C7A7E);
        footer.setTextSize(11);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(10), 0, 0);
        panel.addView(footer);

        dialog.setContentView(overlay);
        dialog.setCancelable(true);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.dimAmount = 0.25f;
            window.setAttributes(attrs);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private TextView drawerButton(String label, boolean primary) {
        TextView button = new TextView(this);
        button.setText(label);
        button.setTextSize(15);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setTextColor(primary ? RED_DARK : INK);
        button.setBackground(roundRect(primary ? 0xFFFFF0F2 : 0xFFF8F5F6, dp(15)));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private void renderHistoryList(LinearLayout container, String query, Dialog dialog) {
        container.removeAllViews();
        ArrayList<ChatRecord> sorted = new ArrayList<>(conversations);
        Collections.sort(sorted, Comparator.comparingLong((ChatRecord c) -> c.updatedAt).reversed());
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        int matches = 0;
        for (ChatRecord record : sorted) {
            if (record.messages.isEmpty()) continue;
            if (!needle.isEmpty() && !record.title.toLowerCase(Locale.ROOT).contains(needle)) continue;
            TextView row = new TextView(this);
            row.setText(record.title);
            row.setTextColor(INK);
            row.setTextSize(15);
            row.setMaxLines(2);
            row.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
            row.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
            row.setPadding(dp(12), dp(8), dp(12), dp(8));
            row.setBackground(roundRect(0xFFFFFBFC, dp(14)));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
            rowParams.bottomMargin = dp(5);
            container.addView(row, rowParams);
            row.setOnClickListener(v -> {
                currentConversation = record;
                dialog.dismiss();
                renderConversation(record);
            });
            matches++;
        }
        if (matches == 0) {
            TextView empty = new TextView(this);
            empty.setText(needle.isEmpty() ? "لا توجد محادثات بعد" : "لا توجد نتائج مطابقة");
            empty.setTextColor(0xFF88777B);
            empty.setTextSize(14);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(12), dp(24), dp(12), dp(24));
            container.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private void renderConversation(ChatRecord record) {
        messageList.removeAllViews();
        for (String message : record.messages) {
            addBubble(message, true);
            addConnectionNotice();
        }
        refreshConversationVisibility();
        scrollChatToBottom();
    }

    private void loadConversations() {
        conversations.clear();
        String raw = preferences.getString("conversations", "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                ChatRecord record = new ChatRecord(item.optString("id", UUID.randomUUID().toString()),
                        item.optString("title", "محادثة"));
                record.updatedAt = item.optLong("updated", 0L);
                JSONArray messages = item.optJSONArray("messages");
                if (messages != null) {
                    for (int j = 0; j < messages.length(); j++) record.messages.add(messages.optString(j, ""));
                }
                conversations.add(record);
            }
        } catch (JSONException ignored) {
            preferences.edit().remove("conversations").apply();
        }
    }

    private void saveConversations() {
        JSONArray array = new JSONArray();
        try {
            for (ChatRecord record : conversations) {
                JSONObject item = new JSONObject();
                item.put("id", record.id);
                item.put("title", record.title);
                item.put("updated", record.updatedAt);
                JSONArray messages = new JSONArray();
                for (String message : record.messages) messages.put(message);
                item.put("messages", messages);
                array.put(item);
            }
            preferences.edit().putString("conversations", array.toString()).apply();
        } catch (JSONException ignored) {
            Toast.makeText(this, "تعذر حفظ سجل المحادثات محليًا", Toast.LENGTH_SHORT).show();
        }
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

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        super.onDestroy();
    }

    private static class ChatRecord {
        final String id;
        String title;
        long updatedAt = System.currentTimeMillis();
        final ArrayList<String> messages = new ArrayList<>();
        ChatRecord(String id, String title) { this.id = id; this.title = title; }
    }

    private static class Attachment {
        final Uri uri;
        final String name;
        Attachment(Uri uri, String name) { this.uri = uri; this.name = name; }
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
        GlowBackgroundView(MainActivity context) { super(context); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            paint.setShader(new LinearGradient(0, 0, 0, h,
                    new int[]{Color.WHITE, Color.WHITE, 0xFFFFFBFC, 0xFFFFF5F6, 0xFFFFE8EC, 0xFFFFD0D9},
                    new float[]{0f, 0.60f, 0.74f, 0.86f, 0.95f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(new RadialGradient(w * 0.5f, h * 1.02f, Math.max(w * 1.18f, h * 0.55f),
                    new int[]{0x66F05B76, 0x33E84B69, 0x00FFFFFF},
                    new float[]{0f, 0.48f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, h * 0.58f, w, h, paint);
            paint.setShader(null);
        }
    }

    private class WormLogoView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        WormLogoView(MainActivity context) { super(context); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) * 0.46f;
            paint.setShader(new RadialGradient(cx * 0.72f, cy * 0.62f, radius * 1.35f,
                    new int[]{0xFFFF7187, RED, RED_DARK}, null, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(null);
            paint.setColor(Color.WHITE);
            paint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            paint.setTextSize(radius * 0.95f);
            paint.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fm = paint.getFontMetrics();
            canvas.drawText("W", cx, cy - (fm.ascent + fm.descent) / 2f, paint);
        }
    }

    private class ProfileAvatarView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ProfileAvatarView(MainActivity context) { super(context); }
        @Override protected void onDraw(Canvas canvas) {
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
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float s = getResources().getDisplayMetrics().density;
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            stroke(canvas, 1.7f * s, RED_DARK);
            canvas.drawArc(new RectF(cx - 11 * s, cy - 8 * s, cx + 9 * s, cy + 9 * s), 150, 285, false, paint);
            Path pen = new Path();
            pen.moveTo(cx - 1 * s, cy - 1 * s);
            pen.lineTo(cx + 8 * s, cy - 10 * s);
            pen.quadTo(cx + 11 * s, cy - 12 * s, cx + 13 * s, cy - 9 * s);
            pen.lineTo(cx + 3 * s, cy + 1 * s);
            pen.close();
            canvas.drawPath(pen, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private class ChevronView extends IconView {
        ChevronView(MainActivity context) { super(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float s = getResources().getDisplayMetrics().density;
            stroke(canvas, 1.8f * s, RED_DARK);
            Path path = new Path();
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            path.moveTo(cx - 4 * s, cy - 2 * s);
            path.lineTo(cx, cy + 2 * s);
            path.lineTo(cx + 4 * s, cy - 2 * s);
            canvas.drawPath(path, paint);
        }
    }

    private class MenuIconView extends IconView {
        MenuIconView(MainActivity context) { super(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float s = getResources().getDisplayMetrics().density;
            stroke(canvas, 2f * s, RED_DARK);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            canvas.drawLine(cx - 7 * s, cy - 5 * s, cx + 7 * s, cy - 5 * s, paint);
            canvas.drawLine(cx - 7 * s, cy, cx + 7 * s, cy, paint);
            canvas.drawLine(cx - 7 * s, cy + 5 * s, cx + 7 * s, cy + 5 * s, paint);
        }
    }

    private class VoiceWaveView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean sendMode;
        VoiceWaveView(MainActivity context) { super(context); setClickable(true); setFocusable(true); }
        void setSendMode(boolean value) {
            if (sendMode != value) {
                sendMode = value;
                invalidate();
            }
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float s = getResources().getDisplayMetrics().density;
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            if (sendMode) {
                paint.setColor(RED);
                canvas.drawCircle(cx, cy, Math.min(getWidth(), getHeight()) * 0.49f, paint);
                paint.setColor(Color.WHITE);
                paint.setStrokeWidth(2f * s);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                canvas.drawLine(cx, cy + 7 * s, cx, cy - 7 * s, paint);
                canvas.drawLine(cx, cy - 7 * s, cx - 6 * s, cy - 1 * s, paint);
                canvas.drawLine(cx, cy - 7 * s, cx + 6 * s, cy - 1 * s, paint);
            } else {
                paint.setColor(0xFFFFE5E9);
                canvas.drawCircle(cx, cy, Math.min(getWidth(), getHeight()) * 0.49f, paint);
                paint.setColor(RED_DARK);
                paint.setStrokeWidth(1.7f * s);
                paint.setStrokeCap(Paint.Cap.ROUND);
                float[] heights = {10, 18, 24, 15, 8};
                for (int i = 0; i < heights.length; i++) {
                    float x = cx + (i - 2) * 4.3f * s;
                    float half = heights[i] * s * 0.5f;
                    canvas.drawLine(x, cy - half, x, cy + half, paint);
                }
            }
        }
    }

    private class MicrophoneView extends IconView {
        private boolean active;
        MicrophoneView(MainActivity context) { super(); }
        void setActive(boolean value) { active = value; invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float s = getResources().getDisplayMetrics().density;
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            stroke(canvas, 1.65f * s, active ? RED : INK);
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
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float s = getResources().getDisplayMetrics().density;
            stroke(canvas, 1.8f * s, RED_DARK);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            canvas.drawLine(cx - 6.5f * s, cy, cx + 6.5f * s, cy, paint);
            canvas.drawLine(cx, cy - 6.5f * s, cx, cy + 6.5f * s, paint);
        }
    }
}