# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Jsoup classes
-keep class org.jsoup.** { *; }
-keeppackagenames org.jsoup.nodes

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Keep data classes
-keep class com.massmediasaver.data.** { *; }
