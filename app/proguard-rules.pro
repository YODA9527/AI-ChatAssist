# ============================
# ChatAssist ProGuard 规则
# ============================

# ----- ML Kit 文本识别（保留模型反射） -----
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.android.odml.** { *; }

# ----- Hilt（生成代码 + 注解） -----
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep class dagger.hilt.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ----- Kotlinx Serialization -----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class **$$serializer { *; }
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# ----- Retrofit / OkHttp -----
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**

# ----- Compose -----
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }

# ----- Room（实体类字段） -----
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# ----- Timber -----
-dontwarn org.jetbrains.annotations.**

# ----- 通用：保留行号、源文件（崩溃栈可读） -----
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
