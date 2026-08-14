# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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

# Keep Room entities
-keep class com.reganye.pocketrate.data.local.entity.** { *; }

# Keep Retrofit models
-keep class com.reganye.pocketrate.data.remote.** { *; }

# Keep Hilt
-keepclassmembers class * extends android.app.Application {
    <init>();
}

# --- Gson / JSON serialization ---
# Keep generic signatures and annotations so Retrofit/Gson can parse models correctly.
-keepattributes Signature, InnerClasses, EnclosingMethod, Exceptions
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }
-dontwarn com.google.gson.**

# --- Hilt + WorkManager ---
# Keep Hilt-generated components and entry points.
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# Keep WorkManager workers and the Hilt worker factory.
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.impl.WorkerWrapper { *; }
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.hilt.work.HiltWorkerFactory { *; }

# --- Kotlin coroutines / serialization metadata ---
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations
