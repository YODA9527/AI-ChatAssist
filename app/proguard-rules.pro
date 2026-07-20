# =====================================================================
# ChatAssist ProGuard / R8 规则（v0.22 精简）
# =====================================================================
# 设计原则：
#   - 只保留"必须保留"的具体类，绝不宽 -keep
#   - 依赖库自身的 consumer-rules 规则会处理常见情况，我们只补 ChatAssist 特有项
#   - R8 fullmode + AGP 8.6 已默认更激进
# =====================================================================

# ----- ML Kit 文本识别（v0.22 收紧，只保留中文识别实际用到的类） -----
-keep class com.google.mlkit.vision.text.chinese.** { *; }
-keep class com.google.mlkit.common.sdkinternal.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_** { *; }
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.mlkit.**

# ----- Hilt（v0.22 收紧到精确类） -----
# Hilt 生成代码 + 注解（consumer-rules 已处理大部分，我们只兜底）
-keep class dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { <init>(...); }

# ----- Kotlinx Serialization（必须保留 @Serializable 类的 companion + serializer） -----
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes Signature, InnerClasses, EnclosingMethod
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# ----- Retrofit / OkHttp -----
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**

# ----- Room（v0.22 收紧到只保留实体字段） -----
-keepclassmembers @androidx.room.Entity class * { <fields>; }
-keep @androidx.room.Dao class * { <methods>; }
-keepclassmembers class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.room.RoomMasterTable { *; }
-dontwarn androidx.room.paging.**

# ----- Timber -----
-dontwarn org.jetbrains.annotations.**

# ----- 通用：保留行号、源文件（崩溃栈可读） -----
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# =====================================================================
# 显式 NOT keep 的（v0.22 移除的）：
#   - androidx.compose.**        （R8 + Compose 编译器已处理）
#   - androidx.lifecycle.ViewModel   （AGP/Hilt 已自动）
#   - com.google.mlkit.**       （已收紧到 chinese + sdkinternal）
#   - @androidx.room.Entity class *   （已改为只 keep 字段）
#   - @dagger.hilt.android.HiltAndroidApp  （Hilt 已自动）
# =====================================================================
