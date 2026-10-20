# ---- kotlinx-serialization -------------------------------------------------
# Serializers are looked up reflectively by generated Companion methods.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.filmy.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.filmy.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep the generated serializer() companions from being stripped.
-keep,includedescriptorclasses class com.filmy.app.data.**$$serializer { *; }

# ---- OkHttp ----------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ---- Media3 / ExoPlayer ----------------------------------------------------
# Media3 uses @Keep + consumer rules, but release minification can still strip
# renderer factories loaded reflectively. Keep the default factory explicitly.
-keep class androidx.media3.exoplayer.DefaultRenderersFactory { *; }
-keep class androidx.media3.exoplayer.source.DefaultMediaSourceFactory { *; }
-keep class androidx.media3.exoplayer.dash.DashMediaSource$Factory { *; }

# ---- General ---------------------------------------------------------------
# Playable model classes are only accessed via serialization; keep field names
# so JSON mapping cannot break under R8 name mangling.
-keepclassmembers class com.filmy.app.data.** {
    <fields>;
    <init>(...);
}
