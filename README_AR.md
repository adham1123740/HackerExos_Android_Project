# HackerExos Android Project (ملفات Android فقط)

هذا المجلد خاص بمشروع Android الذي يُرفع إلى مستودع GitHub للبناء. لا يحتوي على واجهة موقع IDE أو ملف `index.php`.

## البناء
1. ارفع جميع محتويات هذا المجلد إلى جذر مستودع GitHub جديد، مثل `adham1123740/HackerExosIDE-Android`.
2. تأكد من تفعيل GitHub Actions.
3. من تبويب Actions شغّل workflow المسمى `HackerExos IDE - Build APK`.
4. بعد نجاح البناء نزّل artifact باسم `HackerExos-debug-APK`؛ هذا artifact ZIP يحتوي على APK.

## ملاحظات مهمة
- هذا قالب Android أولي؛ المحرر/خادم الموقع يجب أن يكتب ملفات المصدر إلى المستودع قبل البناء.
- القالب الحالي يستهدف `app/src/main/java/com/hackerexos/ide/MainActivity.java` فقط؛ لا يدعم بعد إنشاء مشاريع متعددة أو تعديل كل ملفات Gradle/Manifest/resources ديناميكيًا.
- لا تضع أي GitHub Personal Access Token داخل ملفات هذا المشروع أو داخل JavaScript أو داخل مستودع GitHub.


## اسم التطبيق الديناميكي
يرسل موقع HackerExos Studio قيمة app_name إلى API. يقوم API بتحديث app/src/main/res/values/strings.xml قبل تشغيل GitHub Actions، ولذلك يظهر الاسم الذي أدخلته في الموقع أسفل أيقونة التطبيق الناتج. اسم ملف Artifact يبقى HackerExos-APK.zip.
