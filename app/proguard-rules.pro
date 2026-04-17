# FediQuest ProGuard Rules

# Keep TFLite models and classes
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

# Keep ARCore and SceneView classes
-keep class com.google.ar.** { *; }
-keep class io.github.sceneview.** { *; }
-keep class com.gorisse.thomas.sceneview.** { *; }

# Keep Gson type tokens
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.fediquest.app.data.models.** { *; }
-dontwarn sun.reflect.**

# Keep Room entities
-keep class com.fediquest.app.data.local.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Retrofit interfaces
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Keep OSMDroid
-keep class org.osmdroid.** { *; }

# Keep CameraX
-keep class androidx.camera.** { *; }

# Keep Material components
-keep class com.google.android.material.** { *; }
