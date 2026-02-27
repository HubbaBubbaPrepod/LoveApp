# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ─── Retrofit + Gson models ──────────────────────────────────────────────────
# Keep all API model data classes so Gson can deserialize JSON correctly
-keep class com.example.loveapp.data.api.models.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepattributes Signature
-keepattributes *Annotation*

# ─── Google Sign-In (Credential Manager) ─────────────────────────────────────
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn sun.misc.**

# ─── Retrofit interface ───────────────────────────────────────────────────────
-keep interface com.example.loveapp.data.api.LoveAppApiService { *; }

# ─── Retrofit core ────────────────────────────────────────────────────────────
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ─── Gson generics / TypeToken ────────────────────────────────────────────────
# These three lines are the critical ones for ApiResponse<T> deserialization:
# Without them R8 strips generic signatures → Class cast to ParameterizedType
-keepattributes InnerClasses
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.** { *; }

# ─── Hilt ─────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# ─── Room ─────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# ─── WorkManager workers ──────────────────────────────────────────────────────
# WorkManager создаёт воркеры по имени класса из базы данных.
# Если R8 переименует классы — при перезапуске приложения будет ClassNotFoundException.
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ─── Glance AppWidget ─────────────────────────────────────────────────────────
# Аналогично: система запускает Receiver по имени из манифеста
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver

# ─── Hilt EntryPoint ─────────────────────────────────────────────────────────
# EntryPointAccessors.fromApplication() ищет реализации по рефлексии
-keep @dagger.hilt.EntryPoint interface *
-keep @dagger.hilt.InstallIn @dagger.hilt.EntryPoint interface * { *; }

# ─── Firebase Messaging ───────────────────────────────────────────────────────
-keep class * extends com.google.firebase.messaging.FirebaseMessagingService

# ─── Kotlin coroutines ────────────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Preserve line numbers for crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile