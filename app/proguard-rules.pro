-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Google AdMob
-keep class com.google.android.gms.ads.** { *; }

# ML Kit Document Scanner & OCR
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.vision.** { *; }

# Room Database Entities & DAOs
-keep class com.smartscan.pro.data.** { *; }

# OkHttp & Coroutines warnings suppression
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
