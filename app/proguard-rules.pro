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

# ===== Glide =====
# Glide 通过 @GlideModule 注解在编译期生成索引，运行时反射实例化各模块，
# 混淆会移除/重命名这些类的构造器导致崩溃（NoSuchMethodException）。
-keep public class * implements com.bumptech.glide.module.GlideModule {
    public <init>();
}
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    public <init>();
}
-keep class * extends com.bumptech.glide.module.LibraryGlideModule {
    public <init>();
}
# Glide 依赖的 OkHttp 集成模块（通过索引反射加载）
-keep class com.bumptech.glide.integration.okhttp3.OkHttpGlideModule {
    public <init>();
}

# ===== kotlinx.serialization =====
# 序列化类的 serializer 通过反射/编译器生成访问，混淆会导致解析崩溃
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers,allowshrinking class * {
    @kotlinx.serialization.SerialInfo *;
    @kotlinx.serialization.Serializable <fields>;
}
-keep,includedescriptorclasses class * implements kotlinx.serialization.KSerializer
-keepclassmembers class **$$serializer {
    *** INSTANCE;
}

# ===== Kodein DI =====
# Kodein 内部通过反射创建/解析绑定（构造器反射），混淆会导致
# NoSuchMethodException: <init> [interface ...] 崩溃。
-keep class org.kodein.di.** { *; }
-dontwarn org.kodein.di.**

# 项目的 diViewModel 通过 getDeclaredConstructor(DI::class.java).newInstance(di)
# 反射创建所有 Store/ViewModel（构造器接收 DI），必须保留这些构造器，
# 否则 R8 混淆后反射失败（NoSuchMethodException: <init> [interface org.kodein.di.DI]）。
-keepclassmembers class * implements org.kodein.di.DIAware {
    public <init>(org.kodein.di.DI);
}

# ===== 播放器（mediamp / Media3）=====
# mediamp（mpv 播放器）通过反射加载播放器实现（Class.newInstance），
# R8 混淆会使其变成抽象类/移除构造器导致视频无法播放（InstantiationException）。
-keep class org.openani.mediamp.** { *; }
# Media3 部分组件也依赖反射/注解索引，保留避免混淆问题
-keep class androidx.media3.** { *; }

# 播放器链路中仍有依赖反射加载实现类（Handler 消息里 Class.newInstance）的组件
# 无法通过 keep 规则精确覆盖（类名在混淆后仍被反射引用），
# 直接禁用类名混淆：R8 优化与压缩保留，仅不重命名类名，确保所有反射/ServiceLoader 可用。
-dontobfuscate

# GSYVideoPlayer 与极验 SDK 的类结构相似，R8 类合并会把两者的代码串在一起，
# 导致播放器缓存逻辑执行到极验的 getCacheManager（反射 newInstance androidx.sqlite.SQLite 失败）。
# keep 这些库阻止 R8 合并/优化它们。
-keep class com.shuyu.gsyvideoplayer.** { *; }
-keep class com.geetest.sdk.** { *; }
-keep class androidx.sqlite.** { *; }

# GSY 的 PlayerFactory.setPlayManager(Class) 通过反射 newInstance 创建播放器管理器，
# R8 会移除/改写无参构造器导致 playerManager 为 null、视频无法初始化。
-keep class com.a10miaomiao.bilimiao.comm.player.** { *; }
-keep class com.a10miaomiao.bilimiao.widget.player.media3.** { *; }
