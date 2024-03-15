# The Android Gradle plugin allows to define ProGuard rules which get embedded in the AAR.
# These ProGuard rules are automatically applied when a consumer app sets minifyEnabled to true.
# The custom rule file must be defined using the 'consumerProguardFiles' property in your
# build.gradle.kts file.

-keepclassmembers class dev.hotwire.core.turbo.session.TurboSession {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class dev.hotwire.core.bridge.Bridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

-keep class dev.hotwire.core.** { *; }

# Gson
-keep class com.google.** { *; }
-keep class org.apache.** { *; }
-keep class javax.** { *; }
-keep class sun.misc.Unsafe { *; }

# Resolve R8 issue: "ERROR: R8: Missing class java.lang.invoke.StringConcatFactory"
-dontwarn java.lang.invoke.StringConcatFactory
