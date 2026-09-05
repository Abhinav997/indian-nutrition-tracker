# ============================================================
# Indian Nutrition Tracker — ProGuard / R8 rules
# ============================================================

# --- Kotlin serialization ---
# Keep serialization annotations and generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep the backup DTO classes and their serializers.
-keep class com.indian.nutrition.tracker.data.export.** { *; }
-keepclassmembers class com.indian.nutrition.tracker.data.export.** {
    *** Companion;
}
-keepclasseswithmembers class com.indian.nutrition.tracker.data.export.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all @Serializable classes and their generated Companion.serializer()
-keep,includedescriptorclasses class com.indian.nutrition.tracker.**$$serializer { *; }
-keepclassmembers class com.indian.nutrition.tracker.** {
    *** Companion;
}
-keepclasseswithmembers class com.indian.nutrition.tracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Retrofit ---
# Retrofit does reflection on generic parameters.
-keepattributes Signature, Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# --- OkHttp ---
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Room ---
# Room uses annotations, keep the generated impl.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# --- Coil ---
-dontwarn coil.**

# --- Kotlin coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# --- General Android ---
# Keep source file names and line numbers for crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
