# Live2D Cubism SDK 的混淆规则：保留 framework/core 的 JNI 相关类与 native 方法
-keep class com.live2d.sdk.cubism.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
