# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }

# Gson
-keepattributes *Annotation*
-keep class com.simutrade.data.remote.** { *; }
-keep class com.simutrade.data.model.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }