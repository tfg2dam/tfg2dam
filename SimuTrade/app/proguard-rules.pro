# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }

# Gson
-keepattributes *Annotation*
-keep class com.simutrade.datos.remoto.** { *; }
-keep class com.simutrade.datos.modelo.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }