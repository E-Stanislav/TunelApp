# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep all TunelApp classes
-keep class com.tunelapp.** { *; }

# Keep VPN Service
-keep class com.tunelapp.service.** { *; }

# Keep Core classes
-keep class com.tunelapp.core.** { *; }

# Keep ViewModels
-keep class com.tunelapp.viewmodel.** { *; }

# Keep UI classes
-keep class com.tunelapp.ui.** { *; }

# Keep Xray native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep data classes
-keep class com.tunelapp.data.** { *; }

# Keep companion objects
-keepclassmembers class * {
    static ** INSTANCE;
}

# Keep reflection-based classes
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Keep coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep serialization
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep R class
-keep class **.R
-keep class **.R$* {
    <fields>;
}

# Keep Log calls (for debugging)
-keep class android.util.Log {
    public static *** d(...);
    public static *** e(...);
    public static *** w(...);
    public static *** i(...);
    public static *** v(...);
}





