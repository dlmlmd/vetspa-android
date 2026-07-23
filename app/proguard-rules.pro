# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.vetspa.nativeapp.data.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
