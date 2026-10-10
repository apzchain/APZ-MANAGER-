-keep class com.chaquo.python.** { *; }
-keep class com.apz.manager.data.model.** { *; }
-dontwarn com.chaquo.python.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.examples.android.model.** { *; }
