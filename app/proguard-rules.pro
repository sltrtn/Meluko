# ── R8 / ProGuard rules for Meluko ──────────────────────────────────────────
# These rules prevent R8 from stripping or renaming classes that are accessed
# via reflection, annotation processing, or serialization.

# Keep line numbers for readable Crashlytics stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Firebase ────────────────────────────────────────────────────────────────
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.storage.** { *; }
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.**

# Firestore data classes — accessed via reflection by Firestore SDK
-keep class com.alarm.meluko.data.** { *; }

# ── Hilt ────────────────────────────────────────────────────────────────────
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ── Kotlin Coroutines ───────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ── Coil (image loading) ───────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ── Kotlin Serialization (if used in future) ────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ── General ─────────────────────────────────────────────────────────────────
# Keep Crashlytics custom exceptions
-keep public class * extends java.lang.Exception

# Keep Hilt entry points
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.hilt.lifecycle.ViewModelInject class * { *; }

# Release builds: drop verbose/debug/info logging (some of it includes user IDs).
# Warnings and errors stay — Crashlytics breadcrumbs rely on them.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Firebase discovers its components by reflection; keep their no-arg constructors.
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }

# ── Haze (backdrop blur for glass surfaces) ─────────────────────────────────
# Haze uses graphics/RenderEffect classes discovered via reflection on API 31+.
-keep class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**

# ── Material3 Expressive (MaterialShapes, LoadingIndicator, MotionScheme) ───
# MaterialShapes polygons are resolved by name/ordinal; keep shape definitions.
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.material3.**

# ── androidx.graphics.shapes (RoundedPolygon used by MaterialShapes) ────────
-keep class androidx.graphics.shapes.** { *; }
-dontwarn androidx.graphics.shapes.**

# ── Google Identity / Credential Manager ───────────────────────────────────
# Reflection-based credential parsing must survive R8.
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**

# ── Coil image loading (profile/tribe photos) ──────────────────────────────
-keep class coil3.** { *; }
-dontwarn coil3.**
