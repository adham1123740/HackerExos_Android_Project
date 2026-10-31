# HackerExos IDE — Android build repository

هذا المستودع يحتوي مشروع Android الذي يبنيه GitHub Actions فقط. لا تضع `api.php` أو `index.php` هنا؛ ارفع هذين الملفين إلى استضافتك الخاصة.

## الملفات المضافة
- أيقونات PNG متعددة الكثافات وأيقونة Adaptive Vector.
- Splash background وثيم وألوان التطبيق.
- انتقالات `res/anim` وأنيميشن نبض `res/animator`.
- واجهة تطبيق تجريبية في `MainActivity.java`.
- سير عمل GitHub Actions داخل `.github/workflows/build.yml`.

## البناء
افتح Actions ثم `HackerExos IDE - Build APK` ثم Run workflow. يمكن للموقع تحديث `MainActivity.java` وتشغيل البناء إذا أُعطي رمز GitHub الصلاحيات اللازمة.

## تنبيه مهم
هذه نسخة IDE ويب مبسطة: محرر Java واحد وقالب Android ثابت. ليست بعد Android Studio كاملًا ولا تدعم إنشاء مشاريع متعددة أو تحرير ملفات XML وGradle من الواجهة. لا تستخدم APK الناتج كتطبيق إنتاجي قبل اختباره وتوقيعه بمفتاح إصدار خاص.
