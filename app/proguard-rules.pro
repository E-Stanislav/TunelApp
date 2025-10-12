# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep VPN Service
-keep class com.tunelapp.service.** { *; }

# Keep Xray native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep data classes
-keep class com.tunelapp.data.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

