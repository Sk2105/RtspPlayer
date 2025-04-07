# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
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


# Keep all classes from VLC packages
-keep class org.videolan.libvlc.** { *; }
-keep class org.videolan.vlc.** { *; }

# Keep native methods (important for JNI calls)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Don't strip classes used in reflection (libVLC uses reflection internally)
-keepclassmembers class * {
    public <init>(...);
}

# Keep all annotations (used by libVLC)
-keepattributes *Annotation*

# Avoid stripping VLC media related classes
-keepclassmembers class org.videolan.libvlc.MediaPlayer {
    *;
}

# Prevent issues with Parcelable
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}