# Sensus - Proguard / R8 Optimization & Obfuscation Rules

# ====================================================================
# Kotlinx Serialization
# ====================================================================
-keepattributes *Annotation*,InnerClasses,Signature
-dontnote kotlinx.serialization.SerializationKt

# Keep companion objects for serializable classes
-keepclassmembers class * {
    *** Companion;
}

# Keep the serializer method on companion objects
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep annotated @Serializable classes and their properties
-keep,allowobfuscation,allowoptimization class * {
    @kotlinx.serialization.Serializable class *;
}

-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Navigation 3 keys
-keep class com.example.sensus.NavigationKeysKt { *; }
-keep class com.example.sensus.Main { *; }

# ====================================================================
# Data Models
# ====================================================================
-keep class com.example.sensus.data.** { *; }

# ====================================================================
# Jetpack Compose & Navigation
# ====================================================================
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

-dontwarn androidx.compose.**

# ====================================================================
# ViewModels & Architecture Components
# ====================================================================
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ====================================================================
# Coroutines
# ====================================================================
-dontwarn kotlinx.coroutines.**

# ====================================================================
# Tink (androidx.security:security-crypto optional cloud KMS downloader)
# ====================================================================
-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn org.joda.time.Instant
